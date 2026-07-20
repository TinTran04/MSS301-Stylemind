import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'
import { mapOrder } from './order.mapper'

export { mapOrder }

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
