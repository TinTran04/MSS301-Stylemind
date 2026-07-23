import apiClient from '../../services/apiClient.js'
import { ENDPOINTS } from '../../services/endpoints.js'

export async function requestOrderCancellation(orderId, payload, idempotencyKey) {
  return apiClient.post(`${ENDPOINTS.ORDERS}/${orderId}/cancellations`, payload, {
    headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined,
  })
}

export async function getOrderCancellations(orderId) {
  return apiClient.get(`${ENDPOINTS.ORDERS}/${orderId}/cancellations`)
}

export async function adminCancelOrder(orderId, payload) {
  return apiClient.post(`${ENDPOINTS.ADMIN_ORDERS}/${orderId}/cancel`, payload)
}

export async function approveOrderCancellation(cancellationId) {
  return apiClient.patch(`${ENDPOINTS.ADMIN_ORDERS}/order-cancellations/${cancellationId}/approve`)
}

export async function rejectOrderCancellation(cancellationId, payload) {
  return apiClient.patch(`${ENDPOINTS.ADMIN_ORDERS}/order-cancellations/${cancellationId}/reject`, payload)
}

export async function getPendingCancellationSummary() {
  return apiClient.get(`${ENDPOINTS.ADMIN_ORDERS}/cancellations/summary`)
}

export async function completeOrderRefund(orderId, refundId, payload) {
  return apiClient.post(`${ENDPOINTS.ADMIN_ORDERS}/${orderId}/refunds/${refundId}/complete`, payload)
}

export async function failOrderRefund(orderId, refundId, payload) {
  return apiClient.post(`${ENDPOINTS.ADMIN_ORDERS}/${orderId}/refunds/${refundId}/fail`, payload)
}
