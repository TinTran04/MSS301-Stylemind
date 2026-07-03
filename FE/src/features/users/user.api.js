import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

function mapUser(u) {
  return {
    id: u.id,
    email: u.email,
    role: u.role,          // 'ADMIN' | 'CUSTOMER'
    provider: u.provider,  // 'LOCAL' | 'GOOGLE' …
    accountStatus: u.accountStatus,
    enabled: u.enabled ?? u.accountStatus === 'ACTIVE',
    createdAt: u.createdAt,
    updatedAt: u.updatedAt,
  }
}

export async function listUsers({ page = 0, size = 20, search = '', role = '', enabled = null } = {}) {
  const params = { page, size }
  if (search) params.search = search
  if (role) params.role = role
  if (enabled !== null && enabled !== undefined) params.enabled = enabled
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

export async function createUser({ email, role }) {
  const res = await apiClient.post(ENDPOINTS.ADMIN_USERS, { email, role })
  return mapUser(res)
}

export async function changeUserRole(userId, role) {
  const res = await apiClient.patch(`${ENDPOINTS.ADMIN_USERS}/${userId}/role`, { role })
  return mapUser(res)
}

export async function changeUserEnabled(userId, enabled) {
  const res = await apiClient.patch(`${ENDPOINTS.ADMIN_USERS}/${userId}/status`, { enabled })
  return mapUser(res)
}

export async function deleteUser(userId) {
  await apiClient.delete(`${ENDPOINTS.ADMIN_USERS}/${userId}`)
}
