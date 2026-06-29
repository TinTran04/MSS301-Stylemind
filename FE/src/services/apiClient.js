import axios from 'axios'

const AUTH_TOKEN_KEY = 'auth_token'
const AUTH_USER_KEY = 'auth_user'
const GUEST_SESSION_KEY = 'guest_session_id'

const apiClient = axios.create({
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use(
  (config) => {
    const token = getAuthToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

apiClient.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'success' in body) {
      if (body.success === false) {
        return Promise.reject(createApiError(body, response.status))
      }
      return body.data
    }
    return body
  },
  (error) => {
    const normalizedError = normalizeApiError(error)
    if (error.response?.status === 401) {
      clearAuthSession()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(normalizedError)
  }
)

function createApiError(body, status) {
  const apiError = new Error(body?.message || 'Request failed')
  apiError.status = status
  apiError.errorCode = body?.errorCode
  apiError.details = body
  return apiError
}

function normalizeApiError(error) {
  const body = error.response?.data
  if (body && typeof body === 'object') {
    return createApiError(body, error.response?.status)
  }

  const apiError = new Error(error.message || 'Network request failed')
  apiError.status = error.response?.status
  apiError.errorCode = error.code
  apiError.details = error
  return apiError
}

export function getAuthToken() {
  return localStorage.getItem(AUTH_TOKEN_KEY)
}

export function getStoredUser() {
  const raw = localStorage.getItem(AUTH_USER_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw)
  } catch {
    localStorage.removeItem(AUTH_USER_KEY)
    return null
  }
}

export function setAuthSession(session) {
  if (session?.token) {
    localStorage.setItem(AUTH_TOKEN_KEY, session.token)
  }
  if (session?.user) {
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(session.user))
  }
}

export function clearAuthSession() {
  localStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(AUTH_USER_KEY)
  sessionStorage.clear()
  document.cookie.split(';').forEach((cookie) => {
    const name = cookie.split('=')[0].trim()
    if (name) {
      document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`
    }
  })
}

export function getGuestSessionId() {
  let guestSessionId = localStorage.getItem(GUEST_SESSION_KEY)
  if (!guestSessionId) {
    guestSessionId = `guest_${crypto.randomUUID?.() || Date.now()}`
    localStorage.setItem(GUEST_SESSION_KEY, guestSessionId)
  }
  return guestSessionId
}

export default apiClient
