export const TAX_RATE = 0.10
export const TAX_LABEL = 'Thuế VAT (10%)'
export const FREE_SHIPPING_THRESHOLD = 200000
export const STANDARD_SHIPPING_FEE = 15000

export function calculateSubtotal(items) {
  return items.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0)
}

export function calculateShipping(subtotal) {
  return subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : STANDARD_SHIPPING_FEE
}

export function calculateTax(subtotal) {
  return Math.round(subtotal * TAX_RATE)
}

export function isCodPaymentMethod(paymentMethod) {
  return String(paymentMethod || '').trim().toLowerCase() === 'cod'
}

export function calculateTotal(items, paymentMethod = null) {
  const subtotal = calculateSubtotal(items)
  const shipping = calculateShipping(subtotal)
  const tax = calculateTax(subtotal)
  const exactTotal = subtotal + shipping + tax
  return {
    subtotal,
    shipping,
    tax,
    exactTotal,
    roundingAdjustment: 0,
    total: exactTotal,
  }
}
