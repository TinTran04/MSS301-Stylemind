import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

export async function getAdminOrders(filters = {}) {
  const params = new URLSearchParams()
  Object.keys(filters).forEach(key => {
    if (filters[key] !== undefined && filters[key] !== null && filters[key] !== '') {
      params.append(key, filters[key])
    }
  })
  const qs = params.toString()
  return apiClient.get(`${ENDPOINTS.ADMIN_ORDERS}${qs ? `?${qs}` : ''}`)
}

export async function updateAdminOrderStatus(id, statusPayload) {
  return apiClient.put(`${ENDPOINTS.ADMIN_ORDERS}/${id}/status`, statusPayload)
}

export async function getAdminOrderAnalytics() {
  // If there's an analytics endpoint for orders
  return apiClient.get(`${ENDPOINTS.ANALYTICS}/orders`)
}
