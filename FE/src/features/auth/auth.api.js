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
    name: user.email,
    role: normalizeRole(user.role),
    provider: user.provider,
    accountStatus: user.accountStatus,
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

export async function forgotPassword(email) {
  await apiClient.post(`${ENDPOINTS.AUTH}/forgot-password`, { email })
}

export async function verifyResetOtp(email, otp) {
  return apiClient.post(`${ENDPOINTS.AUTH}/verify-reset-otp`, { email, otp })
}

export async function resetPassword(email, resetToken, newPassword) {
  await apiClient.post(`${ENDPOINTS.AUTH}/reset-password`, {
    email,
    resetToken,
    newPassword,
  })
}
