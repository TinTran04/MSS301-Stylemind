import { create } from 'zustand'

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
      { label: 'Reserving stock', status: 'pending' },
      { label: 'Processing payment', status: 'pending' },
      { label: 'Confirming order', status: 'pending' },
    ]
    set({ steps: [...steps] })

    for (let i = 0; i < steps.length; i++) {
      set({ currentStep: i })
      const updated = [...get().steps]
      updated[i] = { ...updated[i], status: 'processing' }
      set({ steps: [...updated] })

      await new Promise((r) => setTimeout(r, 800 + Math.random() * 600))

      if (i === 2 && method === 'online_simulated' && orderData?.simulateFailure) {
        const failed = [...get().steps]
        failed[i] = { ...failed[i], status: 'failed' }
        set({ steps: [...failed], status: 'failed', error: 'Payment was declined. Your card was not charged.' })
        return { success: false }
      }

      const done = [...get().steps]
      done[i] = { ...done[i], status: 'completed' }
      set({ steps: [...done] })
    }

    const order = {
      id: `ORD-${Date.now()}`,
      date: new Date().toISOString(),
      status: 'processing',
      items: orderData.items,
      total: orderData.total,
      timeline: [
        { status: 'pending', date: new Date().toISOString(), completed: true },
        { status: 'confirmed', date: new Date().toISOString(), completed: true },
        { status: 'processing', date: new Date().toISOString(), completed: true },
        { status: 'shipped', date: null, completed: false },
        { status: 'delivered', date: null, completed: false },
      ],
    }

    set({ status: 'success', lastOrder: order })
    return { success: true, order }
  },

  reset: () => set({ status: 'idle', steps: [], currentStep: -1, error: null }),
}))

export default usePaymentStore
