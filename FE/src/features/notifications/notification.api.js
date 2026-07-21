import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'
import { sortNotificationsNewestFirst } from './notification.display'

export async function getMyNotifications() {
  const response = await apiClient.get(ENDPOINTS.NOTIFICATIONS)
  return Array.isArray(response) ? sortNotificationsNewestFirst(response) : []
}

export async function getMyNotification(id) {
  return apiClient.get(`${ENDPOINTS.NOTIFICATIONS}/${id}`)
}

export async function getAdminNotifications(filters = {}) {
  const params = new URLSearchParams()
  const effectiveFilters = {
    page: 0,
    size: 100,
    ...filters,
  }
  Object.keys(effectiveFilters).forEach((key) => {
    if (effectiveFilters[key] !== undefined && effectiveFilters[key] !== null && effectiveFilters[key] !== '') {
      params.append(key, effectiveFilters[key])
    }
  })
  const qs = params.toString()
  const response = await apiClient.get(`${ENDPOINTS.ADMIN_NOTIFICATIONS}${qs ? `?${qs}` : ''}`)
  if (Array.isArray(response)) return sortNotificationsNewestFirst(response)
  return {
    ...response,
    content: sortNotificationsNewestFirst(response?.content || []),
  }
}

export async function retryAdminNotification(id) {
  return apiClient.post(`${ENDPOINTS.ADMIN_NOTIFICATIONS}/${id}/retry`)
}
