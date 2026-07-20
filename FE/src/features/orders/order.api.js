import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'
import {
  formatStatusLabel,
  normalizeOrderStatus,
  ORDER_TIMELINE_STEPS,
} from './orderStatus'

function buildTimeline(order) {
  const status = normalizeOrderStatus(order.orderStatus)
  const currentIndex = Math.max(ORDER_TIMELINE_STEPS.indexOf(status), 0)

  return ORDER_TIMELINE_STEPS.map((step, index) => ({
    status: step,
    label: formatStatusLabel(step),
    date: index <= currentIndex ? order.updatedAt || order.createdAt : null,
    completed: index <= currentIndex,
  }))
}

export function mapOrder(order) {
  if (!order) return null

  return {
    ...order,
    id: order.id,
    date: order.createdAt || order.updatedAt,
    status: normalizeOrderStatus(order.orderStatus).toLowerCase(),
    statusLabel: formatStatusLabel(order.orderStatus),
    total: Number(order.totalAmount || 0),
    shippingAddress: order.shippingAddress,
    items: (order.items || []).map((item) => ({
      id: item.id,
      variantId: item.variantId,
      name: item.productName || item.name || item.sku || item.variantId || 'Mặt hàng trong đơn',
      image: item.primaryImageUrl || item.image || 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=200&h=260&fit=crop',
      size: item.size || 'Mặc định',
      color: item.color || 'Mặc định',
      price: Number(item.priceAtPurchase || 0),
      quantity: item.quantity || 1,
    })),
    timeline: buildTimeline(order),
  }
}

export async function createOrder(payload, options = {}) {
  const response = await apiClient.post(ENDPOINTS.ORDERS, payload, {
    headers: options.idempotencyKey
      ? { 'Idempotency-Key': options.idempotencyKey }
      : undefined,
  })
  return mapOrder(response)
}

export async function getOrders() {
  const response = await apiClient.get(ENDPOINTS.ORDERS)
  return Array.isArray(response) ? response.map(mapOrder).filter(Boolean) : []
}

export async function getOrderById(id) {
  const response = await apiClient.get(`${ENDPOINTS.ORDERS}/${id}`)
  return mapOrder(response)
}

export async function getOrderTracking(id) {
  const order = await getOrderById(id)
  return order ? order.timeline : null
}

export async function cancelOrder(orderId) {
  const response = await apiClient.patch(`${ENDPOINTS.ORDERS}/${orderId}/cancel`)
  return mapOrder(response)
}
