import { create } from 'zustand'
import { confirmOrderPayment, createOrder } from '../orders/order.api'

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
      { label: 'Creating order', status: 'pending' },
      { label: method === 'cod' ? 'Setting payment on delivery' : 'Creating sandbox transaction', status: 'pending' },
      { label: method === 'cod' ? 'Confirming order' : 'Waiting for sandbox code', status: 'pending' },
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

      if (method === 'online_simulated') {
        markProcessing(2)
        set({ status: 'awaiting_confirmation', lastOrder: order })
        return { success: true, requiresConfirmation: true, order }
      }

      markProcessing(2)
      markDone(2)

      set({ status: 'success', lastOrder: order })
      return { success: true, order }
    } catch (err) {
      markFailed(Math.max(get().currentStep, 0), err.message || 'Unable to place order.')
      return { success: false }
    }
  },

  confirmSandboxPayment: async (verificationCode) => {
    const { lastOrder } = get()
    if (!lastOrder?.id || !lastOrder?.paymentTransactionId) {
      set({ status: 'failed', error: 'Missing sandbox transaction. Please place the order again.' })
      return { success: false }
    }

    try {
      const order = await confirmOrderPayment(lastOrder.id, {
        transactionId: lastOrder.paymentTransactionId,
        verificationCode,
      })

      const steps = [...get().steps]
      if (steps[2]) {
        steps[2] = {
          ...steps[2],
          status: order.orderStatus === 'CANCELLED' ? 'failed' : 'completed',
        }
      }

      if (order.orderStatus === 'CANCELLED') {
        set({
          status: 'failed',
          steps,
          error: 'Sandbox payment failed.',
          lastOrder: order,
        })
        return { success: false, order }
      }

      set({ status: 'success', steps, error: null, lastOrder: order })
      return { success: true, order }
    } catch (err) {
      if (err.errorCode === 'INVALID_SANDBOX_CODE') {
        set({ status: 'awaiting_confirmation', error: err.message || 'Invalid sandbox code.' })
        return { success: false, retryable: true, message: err.message || 'Invalid sandbox code.' }
      }

      set({ status: 'failed', error: err.message || 'Unable to confirm sandbox payment.' })
      return { success: false }
    }
  },

  reset: () => set({ status: 'idle', steps: [], currentStep: -1, error: null, lastOrder: null }),
}))

export default usePaymentStore
