import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

export async function getMyNotifications() {
  const response = await apiClient.get(ENDPOINTS.NOTIFICATIONS)
  return Array.isArray(response) ? response : []
}

export async function getMyNotification(id) {
  return apiClient.get(`${ENDPOINTS.NOTIFICATIONS}/${id}`)
}

export async function getAdminNotifications(filters = {}) {
  const params = new URLSearchParams()
  Object.keys(filters).forEach((key) => {
    if (filters[key] !== undefined && filters[key] !== null && filters[key] !== '') {
      params.append(key, filters[key])
    }
  })
  const qs = params.toString()
  return apiClient.get(`${ENDPOINTS.ADMIN_NOTIFICATIONS}${qs ? `?${qs}` : ''}`)
}

export async function retryAdminNotification(id) {
  return apiClient.post(`${ENDPOINTS.ADMIN_NOTIFICATIONS}/${id}/retry`)
}
