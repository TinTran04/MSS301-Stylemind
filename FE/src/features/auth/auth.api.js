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

// Step 1 of registration: validates the email is free and emails a 6-digit
// OTP. No account is created and no session is returned yet — the account is
// only created once the OTP is verified.
export async function registerUser(data) {
  await apiClient.post(`${ENDPOINTS.AUTH}/register`, {
    email: data.email,
    password: data.password,
  })
}

// Step 2: verify the emailed OTP. On success the backend creates the ACTIVE
// account; the caller then sends the user to /login to sign in.
export async function verifyRegisterOtp(email, otp) {
  await apiClient.post(`${ENDPOINTS.AUTH}/register/verify-otp`, { email, otp })
}

// Re-issue the registration OTP (subject to a server-side resend cooldown).
export async function resendRegisterOtp(email) {
  await apiClient.post(`${ENDPOINTS.AUTH}/register/resend-otp`, { email })
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

export async function setupPassword(email, token, newPassword) {
  await apiClient.post(`${ENDPOINTS.AUTH}/password/setup`, {
    email,
    token,
    newPassword,
  })
}
