import { create } from 'zustand'
import { createOrder, getOrderById } from '../orders/order.api'
import { createCheckoutAttemptKey, isCurrentCheckoutAttempt } from './checkoutAttempt'

const TERMINAL_SUCCESS_STATUSES = ['PAID', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'COMPLETED']
const TERMINAL_FAILURE_STATUSES = ['EXPIRED', 'CANCELLED', 'FAILED']

let pollTimer = null
let attemptSequence = 0

function buildSepayAwaitingSteps() {
  return [
    { label: 'Đơn hàng đã tạo', status: 'completed' },
    { label: 'Tạo mã VietQR', status: 'completed' },
    { label: 'Chờ chuyển khoản từ ngân hàng', status: 'processing' },
  ]
}

const usePaymentStore = create((set, get) => ({
  status: 'idle',
  steps: [],
  currentStep: -1,
  error: null,
  lastOrder: null,
  activeOrderId: null,
  idempotencyKey: null,
  attemptId: 0,
  method: 'cod',

  setMethod: (method) => set({ method }),

  resumeSepayPayment: (order) => {
    if (!order?.id) return
    get().stopPolling()
    const attemptId = ++attemptSequence
    set({
      status: 'awaiting_confirmation',
      steps: buildSepayAwaitingSteps(),
      currentStep: 2,
      error: null,
      lastOrder: order,
      activeOrderId: order.id,
      idempotencyKey: null,
      attemptId,
      method: 'sepay',
    })
    get().startPollingOrderStatus(order.id, attemptId)
  },

  processPayment: async (orderData) => {
    const currentStatus = get().status
    if (currentStatus === 'processing' || currentStatus === 'awaiting_confirmation') {
      return { success: false, inProgress: true }
    }

    const { method } = get()
    const attemptId = ++attemptSequence
    const idempotencyKey = createCheckoutAttemptKey()
    set({
      status: 'processing',
      steps: [],
      currentStep: -1,
      error: null,
      lastOrder: null,
      activeOrderId: null,
      idempotencyKey,
      attemptId,
    })

    const steps = [
      { label: 'Đang tạo đơn hàng', status: 'pending' },
      { label: method === 'cod' ? 'Thiết lập thanh toán khi nhận hàng' : 'Tạo mã VietQR', status: 'pending' },
      { label: method === 'cod' ? 'Xác nhận đơn hàng' : 'Chờ chuyển khoản từ ngân hàng', status: 'pending' },
    ]
    set({ steps: [...steps] })

    const markProcessing = (index) => {
      set({ currentStep: index })
      const updated = [...get().steps]
      updated[index] = { ...updated[index], status: 'processing' }
      set({ steps: [...updated] })
    }

    const markDone = (index) => {
      const done = [...get().steps]
      done[index] = { ...done[index], status: 'completed' }
      set({ steps: [...done] })
    }

    const markFailed = (index, message) => {
      const failed = [...get().steps]
      failed[index] = { ...failed[index], status: 'failed' }
      set({
        steps: [...failed],
        status: 'failed',
        error: message,
        activeOrderId: null,
        idempotencyKey: null,
      })
    }

    try {
      markProcessing(0)
      const order = await createOrder({
        addressId: orderData.addressId,
        paymentMethod: method,
      }, {
        idempotencyKey,
      })
      if (!isCurrentCheckoutAttempt(get(), attemptId)) {
        return { success: false, stale: true }
      }
      markDone(0)

      markProcessing(1)
      markDone(1)

      if (method === 'sepay') {
        // No customer confirmation step - SePay's webhook reconciles the bank
        // transfer server-side. Polling is the only way this tab finds out.
        markProcessing(2)
        set({ status: 'awaiting_confirmation', lastOrder: order, activeOrderId: order.id })
        get().startPollingOrderStatus(order.id, attemptId)
        return { success: true, requiresConfirmation: true, order }
      }

      markProcessing(2)
      markDone(2)

      set({ status: 'success', lastOrder: order, activeOrderId: order.id, idempotencyKey: null })
      return { success: true, order }
    } catch (err) {
      if (!isCurrentCheckoutAttempt(get(), attemptId)) {
        return { success: false, stale: true }
      }
      markFailed(Math.max(get().currentStep, 0), err?.message || 'Không thể đặt hàng.')
      return { success: false }
    }
  },

  // Poll order status while a SePay payment is awaiting the bank transfer. This
  // is the only way we learn of a PAID webhook or an OrderTimeoutJob expiry -
  // both happen entirely server-side with no action from this tab.
  startPollingOrderStatus: (orderId, attemptId = get().attemptId) => {
    get().stopPolling()
    pollTimer = setInterval(async () => {
      if (!isCurrentCheckoutAttempt(get(), attemptId, orderId)) {
        get().stopPolling()
        return
      }
      try {
        const order = await getOrderById(orderId)
        if (!isCurrentCheckoutAttempt(get(), attemptId, orderId)) {
          get().stopPolling()
          return
        }
        const status = String(order.orderStatus || '').toUpperCase()

        if (TERMINAL_SUCCESS_STATUSES.includes(status)) {
          get().stopPolling()
          set({ status: 'success', error: null, lastOrder: order, idempotencyKey: null })
        } else if (TERMINAL_FAILURE_STATUSES.includes(status)) {
          get().stopPolling()
          set({
            status: 'failed',
            error: status === 'EXPIRED'
              ? 'Phiên thanh toán đã hết hạn. Vui lòng đặt hàng mới.'
              : 'Thanh toán thất bại.',
            lastOrder: order,
            activeOrderId: null,
            idempotencyKey: null,
          })
        } else {
          set({ lastOrder: order })
        }
      } catch {
        // Transient poll failure - keep polling rather than flipping to failed on one blip.
      }
    }, 3000)
  },

  stopPolling: () => {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  },

  reset: () => {
    get().stopPolling()
    const nextAttemptId = ++attemptSequence
    set({
      status: 'idle',
      steps: [],
      currentStep: -1,
      error: null,
      lastOrder: null,
      activeOrderId: null,
      idempotencyKey: null,
      attemptId: nextAttemptId,
    })
  },
}))

export default usePaymentStore
