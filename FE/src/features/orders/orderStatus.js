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

export function getAvailableTransitions(order) {
  if (Array.isArray(order?.availableTransitions)) {
    return order.availableTransitions
  }
  const status = String(order?.orderStatus || '').toUpperCase()
  return ORDER_STATUS_TRANSITIONS[status] || []
}

export function formatStatusLabel(status) {
  const labels = {
    PENDING: 'Đang chờ',
    PAYMENT_PENDING: 'Chờ thanh toán',
    PAID: 'Đã thanh toán',
    CONFIRMED: 'Đã xác nhận',
    PROCESSING: 'Đang xử lý',
    SHIPPED: 'Đang giao',
    COMPLETED: 'Hoàn tất',
    CANCELLED: 'Đã hủy',
    EXPIRED: 'Đã hết hạn',
    FAILED: 'Thất bại',
  }
  const key = String(status || '').toUpperCase()
  return labels[key] || String(status || '')
}
