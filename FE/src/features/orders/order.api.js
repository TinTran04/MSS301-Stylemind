import apiClient from '../../services/apiClient.js'
import { ENDPOINTS } from '../../services/endpoints.js'
import { mapOrder, mapOrderSummary } from './order.mapper.js'

export { mapOrder }

export async function createOrder(payload, options = {}) {
  const response = await apiClient.post(ENDPOINTS.ORDERS, payload, {
    headers: options.idempotencyKey
      ? { 'Idempotency-Key': options.idempotencyKey }
      : undefined,
  })
  return mapOrder(response)
}

export async function getOrders({ page = 0, size = 10, sort = 'createdAt,desc', status } = {}) {
  const params = new URLSearchParams({ page: String(page), size: String(size), sort })
  if (status) params.set('status', status)
  const response = await apiClient.get(`${ENDPOINTS.ORDERS}?${params.toString()}`)
  return {
    ...response,
    content: (response?.content || []).map(mapOrderSummary).filter(Boolean),
  }
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

export async function requestOrderCancellation(orderId, payload, options = {}) {
  const response = await apiClient.post(`${ENDPOINTS.ORDERS}/${orderId}/cancellations`, payload, {
    headers: options.idempotencyKey ? { 'Idempotency-Key': options.idempotencyKey } : undefined,
  })
  return response
}

export async function getOrderCancellations(orderId) {
  return apiClient.get(`${ENDPOINTS.ORDERS}/${orderId}/cancellations`)
}

export async function uploadDeliveryImage(orderId, file) {
  const formData = new FormData()
  formData.append('file', file)
  const response = await apiClient.post(`${ENDPOINTS.ORDERS}/${orderId}/delivery-images`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return mapOrder(response)
}
