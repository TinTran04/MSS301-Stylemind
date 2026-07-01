import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

const statusOrder = ['pending', 'confirmed', 'processing', 'shipped', 'delivered']

function normalizeStatus(status) {
  const normalized = String(status || 'pending').toLowerCase()
  if (normalized === 'fulfilled') return 'delivered'
  if (normalized === 'cancelled') return 'cancelled'
  return normalized
}

function buildTimeline(order) {
  const status = normalizeStatus(order.orderStatus)
  const currentIndex = Math.max(statusOrder.indexOf(status), 0)

  return statusOrder.map((step, index) => ({
    status: step,
    label: step.charAt(0).toUpperCase() + step.slice(1),
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
    status: normalizeStatus(order.orderStatus),
    total: Number(order.totalAmount || 0),
    shippingAddress: order.shippingAddress,
    items: (order.items || []).map((item) => ({
      id: item.id,
      variantId: item.variantId,
      name: item.variantId || 'Order item',
      image: 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=200&h=260&fit=crop',
      size: 'One Size',
      color: 'Default',
      price: Number(item.priceAtPurchase || 0),
      quantity: item.quantity || 1,
    })),
    timeline: buildTimeline(order),
  }
}

export async function createOrder(payload) {
  const response = await apiClient.post(ENDPOINTS.ORDERS, payload)
  return mapOrder(response)
}

export async function confirmOrderPayment(orderId, payload) {
  const response = await apiClient.post(`${ENDPOINTS.ORDERS}/${orderId}/payment/confirm`, payload)
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
