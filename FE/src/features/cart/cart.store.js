import { create } from 'zustand'
import { peekGuestSessionId, resetGuestSessionId } from '../../services/apiClient'
import { resolveVariant } from '../products/product.variant-selection.js'
import { addToCart, getCart, mergeCart, removeCartItem, updateCartItem } from './cart.api'

// When no size/color is given (quick-add from a product card, AI
// recommendations), fall back to the product's default variant. When a
// selection IS given (Product Detail page), it must resolve to a real,
// existing combination — never invent/guess one by silently falling back.
function selectVariant(product, size, color) {
  if (!size && !color) {
    return product?.availableVariantId || null
  }
  return resolveVariant(product?.variants || [], size, color)?.id || null
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

  addItem: async (product, quantity = 1, size = null, color = null, aiSource = null) => {
    const variantId = selectVariant(product, size, color)
    if (!variantId) {
      set({ error: 'No variant available for this product.' })
      return null
    }

    set({ loading: true, error: null })
    try {
      const payload = { variantId, quantity }
      if (aiSource?.isAiRecommended) {
        payload.isAiRecommended = true
        if (aiSource.sourceBundleId) {
          payload.sourceBundleId = aiSource.sourceBundleId
        }
      }
      const cart = await addToCart(payload)
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

  // Logout-only: drops the in-memory cart view without touching the server
  // cart, so the previous user's items/badge count don't linger after logout.
  resetLocalCart: () => {
    set({ items: [], cartId: null, error: null, loading: false })
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
      // Only clear the guest id once its cart has actually been folded in —
      // clearing it on failure would silently orphan the guest cart forever.
      resetGuestSessionId()
    } catch (err) {
      set({
        error: 'Chưa thể đồng bộ giỏ hàng. Hệ thống chưa thể chuyển giỏ hàng tạm thời vào tài khoản của bạn. Vui lòng thử lại sau.',
      })
    }
  },
}))

export default useCartStore
