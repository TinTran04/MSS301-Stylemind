import apiClient from '../../services/apiClient.js'
import { ENDPOINTS } from '../../services/endpoints.js'
import { getOrderTimestamp, mapOrder } from './order.mapper.js'

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
  return Array.isArray(response)
    ? response.map(mapOrder).filter(Boolean).sort((a, b) => getOrderTimestamp(b) - getOrderTimestamp(a))
    : []
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

export async function uploadDeliveryImage(orderId, file) {
  const formData = new FormData()
  formData.append('file', file)
  const response = await apiClient.post(`${ENDPOINTS.ORDERS}/${orderId}/delivery-images`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return mapOrder(response)
}
