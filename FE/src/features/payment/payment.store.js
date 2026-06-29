import { create } from 'zustand'
import { createOrder } from '../orders/order.api'

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
      { label: method === 'cod' ? 'Setting payment on delivery' : 'Processing payment', status: 'pending' },
      { label: 'Confirming order', status: 'pending' },
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

      const transactionId = method === 'sandbox'
        ? (crypto.randomUUID?.() ?? `txn_${Date.now()}`)
        : undefined

      const order = await createOrder({
        shippingAddress,
        paymentMethod: method,
        ...(transactionId && { transactionId }),
      })
      markDone(0)

      markProcessing(1)
      markDone(1)

      markProcessing(2)
      markDone(2)

      set({ status: 'success', lastOrder: order })
      return { success: true, order }
    } catch (err) {
      markFailed(Math.max(get().currentStep, 0), err.message || 'Unable to place order.')
      return { success: false }
    }
  },

  reset: () => set({ status: 'idle', steps: [], currentStep: -1, error: null }),
}))

export default usePaymentStore
