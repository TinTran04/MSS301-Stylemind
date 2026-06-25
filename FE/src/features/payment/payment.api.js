import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

export async function checkoutPayment(payload) {
  return apiClient.post(`${ENDPOINTS.PAYMENT}/checkout`, payload)
}

export async function processPayment(payload) {
  return apiClient.post(`${ENDPOINTS.PAYMENT}/process`, payload)
}
