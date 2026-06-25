import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

function mapUser(u) {
  return {
    id: u.id,
    email: u.email,
    fullName: u.fullName || '',
    role: u.role,          // 'ADMIN' | 'CUSTOMER'
    provider: u.provider,  // 'LOCAL' | 'GOOGLE' …
    enabled: u.enabled,
    createdAt: u.createdAt,
    updatedAt: u.updatedAt,
  }
}

export async function listUsers({ page = 0, size = 20, search = '' } = {}) {
  const params = { page, size }
  if (search) params.search = search
  const res = await apiClient.get(ENDPOINTS.ADMIN_USERS, { params })
  // res is already unwrapped by interceptor → { content, page, size, totalElements, … }
  return {
    content: (res.content || []).map(mapUser),
    page: res.page,
    size: res.size,
    totalElements: res.totalElements,
    totalPages: res.totalPages,
    last: res.last,
  }
}

export async function getUserById(userId) {
  const res = await apiClient.get(`${ENDPOINTS.ADMIN_USERS}/${userId}`)
  return mapUser(res)
}

export async function changeUserRole(userId, role) {
  const res = await apiClient.put(`${ENDPOINTS.ADMIN_USERS}/${userId}/role`, { role })
  return mapUser(res)
}

export async function changeUserEnabled(userId, enabled) {
  const res = await apiClient.put(`${ENDPOINTS.ADMIN_USERS}/${userId}/enabled`, { enabled })
  return mapUser(res)
}
