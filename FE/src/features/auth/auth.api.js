import { ENDPOINTS } from '../../services/endpoints'
import apiClient, { clearAuthSession, setAuthSession } from '../../services/apiClient'

function normalizeRole(role) {
  return role ? role.toLowerCase().replace('role_', '') : 'customer'
}

function mapUser(user) {
  if (!user) return null
  return {
    id: user.id,
    email: user.email,
    name: user.name || user.fullName || user.email,
    fullName: user.fullName || user.name || user.email,
    role: normalizeRole(user.role),
    provider: user.provider,
    createdAt: user.createdAt,
  }
}

function mapSession(response) {
  const session = {
    token: response?.token,
    user: mapUser(response?.user),
  }
  setAuthSession(session)
  return session
}

export async function loginUser(email, password) {
  const response = await apiClient.post(`${ENDPOINTS.AUTH}/login`, { email, password })
  return mapSession(response)
}

export async function registerUser(data) {
  const response = await apiClient.post(`${ENDPOINTS.AUTH}/register`, {
    name: data.name,
    email: data.email,
    password: data.password,
  })
  return mapSession(response)
}

export async function logoutUser() {
  try {
    await apiClient.post(`${ENDPOINTS.AUTH}/logout`)
  } finally {
    clearAuthSession()
  }
  return true
}

export async function getCurrentUser() {
  const response = await apiClient.get(`${ENDPOINTS.AUTH}/me`)
  return mapUser(response)
}
