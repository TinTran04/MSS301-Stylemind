// Mirrors BE/order-service/src/main/java/com/stylemind/order/entity/OrderStatus.java (§10.1).
// This is a fallback only — the authoritative source of valid next states for a
// given order is the `availableTransitions` field the backend returns on
// OrderResponse. Use this map only if that field is ever missing (e.g. stale
// cached data), so the dropdown never trusts client-derived state over the
// backend's own OrderStatusService.changeStatus() transition graph.
export const ORDER_STATUS_TRANSITIONS = {
  PENDING: ['PAYMENT_PENDING', 'CONFIRMED', 'CANCELLED'],
  PAYMENT_PENDING: ['PAID', 'EXPIRED', 'FAILED', 'CANCELLED'],
  PAID: ['CONFIRMED', 'PROCESSING', 'CANCELLED'],
  CONFIRMED: ['PROCESSING', 'CANCELLED'],
  PROCESSING: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['COMPLETED'],
  COMPLETED: [],
  CANCELLED: [],
  EXPIRED: [],
  FAILED: [],
}

export const ORDER_STATUS_LABELS = {
  PENDING: 'Đang chờ',
  PAYMENT_PENDING: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  CONFIRMED: 'Đã xác nhận',
  PROCESSING: 'Đang xử lý',
  SHIPPED: 'Đang giao',
  COMPLETED: 'Đã giao thành công',
  CANCELLED: 'Đã hủy',
  EXPIRED: 'Đã hết hạn',
  FAILED: 'Thất bại',
}

export const ORDER_TIMELINE_STEPS = ['PENDING', 'PAYMENT_PENDING', 'PAID', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'COMPLETED']

export const ORDER_COD_TIMELINE_STEPS = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'COMPLETED']

const ORDER_COD_TIMELINE_LABELS = {
  PROCESSING: 'Đang xử lý / Đóng gói',
  COMPLETED: 'Đã giao thành công / Đã thanh toán',
}

const COD_HIDDEN_PAYMENT_STATUSES = new Set(['PAYMENT_PENDING', 'PAID'])

export const ORDER_REVENUE_STATUSES = new Set(['PAID', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'COMPLETED'])

export function normalizeOrderStatus(status) {
  const normalized = String(status || '').trim().toUpperCase()
  if (normalized === 'FULFILLED' || normalized === 'DELIVERED') return 'COMPLETED'
  return normalized
}

export function getAvailableTransitions(order) {
  const status = normalizeOrderStatus(order?.orderStatus)
  const transitions = Array.isArray(order?.availableTransitions)
    ? order.availableTransitions
    : ORDER_STATUS_TRANSITIONS[status] || []

  if (isCodPaymentMethod(order?.paymentMethod)) {
    return transitions.filter((nextStatus) => !COD_HIDDEN_PAYMENT_STATUSES.has(normalizeOrderStatus(nextStatus)))
  }

  return transitions
}

export function isCodPaymentMethod(paymentMethod) {
  return String(paymentMethod || '').trim().toLowerCase() === 'cod'
}

export function getOrderTimelineSteps(order) {
  const status = normalizeOrderStatus(order?.orderStatus)
  const codOrder = isCodPaymentMethod(order?.paymentMethod)
  const baseSteps = codOrder ? ORDER_COD_TIMELINE_STEPS : ORDER_TIMELINE_STEPS

  if (baseSteps.includes(status) || (codOrder && COD_HIDDEN_PAYMENT_STATUSES.has(status))) {
    return baseSteps
  }

  return [...baseSteps, status]
}

export function formatStatusLabel(status) {
  const key = normalizeOrderStatus(status)
  return ORDER_STATUS_LABELS[key] || 'Không xác định'
}

export function formatTimelineStatusLabel(status, paymentMethod) {
  const key = normalizeOrderStatus(status)
  if (isCodPaymentMethod(paymentMethod) && ORDER_COD_TIMELINE_LABELS[key]) {
    return ORDER_COD_TIMELINE_LABELS[key]
  }
  return formatStatusLabel(key)
}
