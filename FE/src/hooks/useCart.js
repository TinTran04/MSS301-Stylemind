import useCartStore from '../features/cart/cart.store'

export function useCart() {
  const items = useCartStore((s) => s.items)
  const addItem = useCartStore((s) => s.addItem)
  const removeItem = useCartStore((s) => s.removeItem)
  const updateQuantity = useCartStore((s) => s.updateQuantity)
  const clearCart = useCartStore((s) => s.clearCart)
  const loadCart = useCartStore((s) => s.loadCart)
  const loading = useCartStore((s) => s.loading)
  const error = useCartStore((s) => s.error)

  const itemCount = items.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
  const subtotal = items.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0)

  return { items, addItem, removeItem, updateQuantity, clearCart, loadCart, loading, error, itemCount, subtotal }
}

export default useCart
