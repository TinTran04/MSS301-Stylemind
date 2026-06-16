export function calculateSubtotal(items) {
  return items.reduce((sum, item) => sum + item.price * item.quantity, 0)
}

export function calculateShipping(subtotal) {
  return subtotal > 200 ? 0 : 15
}

export function calculateTax(subtotal) {
  return Math.round(subtotal * 0.08 * 100) / 100
}

export function calculateTotal(items) {
  const subtotal = calculateSubtotal(items)
  const shipping = calculateShipping(subtotal)
  const tax = calculateTax(subtotal)
  return { subtotal, shipping, tax, total: subtotal + shipping + tax }
}
