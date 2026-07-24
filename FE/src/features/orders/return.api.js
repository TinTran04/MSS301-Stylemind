import apiClient from '../../services/apiClient.js'
import { ENDPOINTS } from '../../services/endpoints.js'

export async function evaluateReturnEligibility(orderId) {
  return apiClient.get(`${ENDPOINTS.ORDERS}/${orderId}/returns/eligibility`)
}

export async function createReturnRequest(orderId, payload) {
  return apiClient.post(`${ENDPOINTS.ORDERS}/${orderId}/returns`, payload)
}

export async function getCustomerReturns(orderId) {
  return apiClient.get(`${ENDPOINTS.ORDERS}/${orderId}/returns`)
}

export async function cancelReturnRequest(returnId) {
  return apiClient.post(`/api/v1/returns/${returnId}/cancel`)
}

export async function submitReturnShipment(returnId, payload) {
  return apiClient.post(`/api/v1/returns/${returnId}/shipment`, payload)
}

export async function savePayoutDestination(returnId, payload) {
  return apiClient.put(`/api/v1/returns/${returnId}/payout-destination`, payload)
}

export async function getPayoutDestination(returnId) {
  return apiClient.get(`/api/v1/returns/${returnId}/payout-destination`)
}

export async function adminGetReturns(status) {
  const query = status ? `?status=${status}` : ''
  return apiClient.get(`/api/v1/admin/returns${query}`)
}

export async function adminReviewReturn(returnId, payload) {
  return apiClient.post(`/api/v1/admin/returns/${returnId}/review`, payload)
}

export async function adminReceiveAndQc(returnId, payload) {
  return apiClient.post(`/api/v1/admin/returns/${returnId}/receive-qc`, payload)
}

export async function adminCompleteRefund(refundId, payload) {
  return apiClient.post(`/api/v1/admin/refunds/${refundId}/complete`, payload)
}

export async function adminGetRefunds() {
  return apiClient.get('/api/v1/admin/refunds')
}
