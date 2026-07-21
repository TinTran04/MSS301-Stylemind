import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'
import { sortNotificationsNewestFirst } from './notification.display'

export async function getMyNotifications({ page = 0, size = 10, sort = 'createdAt,desc', read } = {}) {
  const params = new URLSearchParams({ page: String(page), size: String(size), sort })
  if (read !== undefined) params.set('read', String(read))
  const response = await apiClient.get(`${ENDPOINTS.NOTIFICATIONS}?${params.toString()}`)
  return {
    ...response,
    content: response?.content || [],
  }
}

export async function getUnreadNotificationCount() {
  return apiClient.get(`${ENDPOINTS.NOTIFICATIONS}/unread-count`)
}

export async function markNotificationRead(id) {
  return apiClient.patch(`${ENDPOINTS.NOTIFICATIONS}/${id}/read`)
}

export async function markAllNotificationsRead() {
  return apiClient.post(`${ENDPOINTS.NOTIFICATIONS}/read-all`)
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
