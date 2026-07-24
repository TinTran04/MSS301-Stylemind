import { apiClient } from '../../lib/api-client'
import { ENDPOINTS } from '../../config/api.config'

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
  return apiClient.post(`/returns/${returnId}/cancel`)
}

export async function submitReturnShipment(returnId, payload) {
  return apiClient.post(`/returns/${returnId}/shipment`, payload)
}

export async function savePayoutDestination(returnId, payload) {
  return apiClient.put(`/returns/${returnId}/payout-destination`, payload)
}

export async function adminGetReturns(status) {
  const query = status ? `?status=${status}` : ''
  return apiClient.get(`/admin/returns${query}`)
}

export async function adminReviewReturn(returnId, payload) {
  return apiClient.post(`/admin/returns/${returnId}/review`, payload)
}

export async function adminReceiveAndQc(returnId, payload) {
  return apiClient.post(`/admin/returns/${returnId}/receive-qc`, payload)
}
