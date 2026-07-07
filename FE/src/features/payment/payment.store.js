import { create } from 'zustand'
import { createOrder, getOrderById } from '../orders/order.api'

const TERMINAL_SUCCESS_STATUSES = ['PAID', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'COMPLETED']
const TERMINAL_FAILURE_STATUSES = ['EXPIRED', 'CANCELLED', 'FAILED']

let pollTimer = null

const usePaymentStore = create((set, get) => ({
  status: 'idle',
  steps: [],
  currentStep: -1,
  error: null,
  lastOrder: null,
  method: 'cod',

  setMethod: (method) => set({ method }),

  processPayment: async (orderData) => {
    const { method } = get()
    set({ status: 'processing', steps: [], currentStep: -1, error: null })

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
      set({ steps: [...failed], status: 'failed', error: message })
    }

    try {
      markProcessing(0)
      const shippingAddress = orderData.shippingAddress
        || [orderData.address?.line1, orderData.address?.line2].filter(Boolean).join(', ')

      const order = await createOrder({
        shippingAddress,
        paymentMethod: method,
      })
      markDone(0)

      markProcessing(1)
      markDone(1)

      if (method === 'sepay') {
        // No customer confirmation step - SePay's webhook reconciles the bank
        // transfer server-side. Polling is the only way this tab finds out.
        markProcessing(2)
        set({ status: 'awaiting_confirmation', lastOrder: order })
        get().startPollingOrderStatus(order.id)
        return { success: true, requiresConfirmation: true, order }
      }

      markProcessing(2)
      markDone(2)

      set({ status: 'success', lastOrder: order })
      return { success: true, order }
    } catch (err) {
      markFailed(Math.max(get().currentStep, 0), 'Không thể đặt hàng.')
      return { success: false }
    }
  },

  // Poll order status while a SePay payment is awaiting the bank transfer. This
  // is the only way we learn of a PAID webhook or an OrderTimeoutJob expiry -
  // both happen entirely server-side with no action from this tab.
  startPollingOrderStatus: (orderId) => {
    get().stopPolling()
    pollTimer = setInterval(async () => {
      try {
        const order = await getOrderById(orderId)
        const status = String(order.orderStatus || '').toUpperCase()

        if (TERMINAL_SUCCESS_STATUSES.includes(status)) {
          get().stopPolling()
          set({ status: 'success', error: null, lastOrder: order })
        } else if (TERMINAL_FAILURE_STATUSES.includes(status)) {
          get().stopPolling()
          set({
            status: 'failed',
            error: status === 'EXPIRED'
              ? 'Phiên thanh toán đã hết hạn. Vui lòng đặt hàng mới.'
              : 'Thanh toán thất bại.',
            lastOrder: order,
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
    set({ status: 'idle', steps: [], currentStep: -1, error: null, lastOrder: null })
  },
}))

export default usePaymentStore
