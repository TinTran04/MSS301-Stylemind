import { create } from 'zustand'
import { peekGuestSessionId, resetGuestSessionId } from '../../services/apiClient'
import { addToCart, getCart, mergeCart, removeCartItem, updateCartItem } from './cart.api'

function selectVariant(product, size, color) {
  if (!product?.variants?.length) {
    return product?.availableVariantId || null
  }

  return product.variants.find((variant) => {
    const sizeMatches = !size || variant.size === size
    const colorMatches = !color || variant.color === color
    return sizeMatches && colorMatches
  })?.id || product.availableVariantId
}

const useCartStore = create((set, get) => ({
  items: [],
  cartId: null,
  loading: false,
  error: null,

  loadCart: async () => {
    set({ loading: true, error: null })
    try {
      const cart = await getCart()
      set({ items: cart.items, cartId: cart.cartId, loading: false })
      return cart
    } catch (err) {
      set({ error: err.message || 'Unable to load cart.', loading: false })
      return null
    }
  },

  addItem: async (product, quantity = 1, size = 'M', color = 'Default') => {
    const variantId = selectVariant(product, size, color)
    if (!variantId) {
      set({ error: 'No variant available for this product.' })
      return null
    }

    set({ loading: true, error: null })
    try {
      const cart = await addToCart({ variantId, quantity })
      set({ items: cart.items, cartId: cart.cartId, loading: false })
      return cart
    } catch (err) {
      set({ error: err.message || 'Unable to add item to cart.', loading: false })
      return null
    }
  },

  removeItem: async (cartItemId) => {
    const previousItems = get().items
    set({ items: previousItems.filter((item) => item.cartItemId !== cartItemId), error: null })
    try {
      await removeCartItem(cartItemId)
    } catch (err) {
      set({ items: previousItems, error: err.message || 'Unable to remove item.' })
    }
  },

  updateQuantity: async (cartItemId, quantity) => {
    if (quantity <= 0) {
      await get().removeItem(cartItemId)
      return
    }

    const previousItems = get().items
    set({
      items: previousItems.map((item) => item.cartItemId === cartItemId ? { ...item, quantity } : item),
      error: null,
    })

    try {
      const cart = await updateCartItem(cartItemId, quantity)
      set({ items: cart.items, cartId: cart.cartId })
    } catch (err) {
      set({ items: previousItems, error: err.message || 'Unable to update quantity.' })
    }
  },

  clearCart: async () => {
    const currentItems = [...get().items]
    set({ items: [], error: null })
    await Promise.allSettled(currentItems.map((item) => removeCartItem(item.cartItemId)))
  },

  // Called right after login: folds the guest cart (if any) into the
  // authenticated user's cart, then rotates the guest session id so a
  // future logout/browse-as-guest cycle doesn't re-merge stale items.
  mergeGuestCart: async () => {
    const guestSessionId = peekGuestSessionId()
    if (!guestSessionId) {
      return
    }

    try {
      const cart = await mergeCart(guestSessionId)
      set({ items: cart.items, cartId: cart.cartId, error: null })
    } catch (err) {
      set({ error: err.message || 'Unable to merge guest cart.' })
    } finally {
      resetGuestSessionId()
    }
  },
}))

export default useCartStore
