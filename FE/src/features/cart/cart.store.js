import { create } from 'zustand'

const useCartStore = create((set, get) => ({
  items: [],

  addItem: (product, quantity = 1, size = 'M', color = 'Default') => {
    const { items } = get()
    const existingIndex = items.findIndex(
      (item) => item.id === product.id && item.size === size && item.color === color
    )
    if (existingIndex > -1) {
      const updated = [...items]
      updated[existingIndex].quantity += quantity
      set({ items: updated })
    } else {
      set({ items: [...items, { ...product, quantity, size, color, cartItemId: Date.now() }] })
    }
  },

  removeItem: (cartItemId) => {
    set({ items: get().items.filter((item) => item.cartItemId !== cartItemId) })
  },

  updateQuantity: (cartItemId, quantity) => {
    if (quantity <= 0) { get().removeItem(cartItemId); return }
    set({ items: get().items.map((item) => item.cartItemId === cartItemId ? { ...item, quantity } : item) })
  },

  clearCart: () => set({ items: [] }),
}))

export default useCartStore
