const AUTH_SESSION_ERROR_CODES = new Set([
  'AUTH_TOKEN_EXPIRED',
  'AUTH_TOKEN_INVALID',
])

export function shouldClearAuthForUnauthorized({ status, errorCode, skipAuthRedirect = false, url = '' } = {}) {
  if (skipAuthRedirect || Number(status) !== 401) return false

  const normalizedCode = String(errorCode || '').toUpperCase()
  if (!AUTH_SESSION_ERROR_CODES.has(normalizedCode)) return false

  const requestUrl = String(url || '')
  if (requestUrl.includes('/api/v1/auth/login')) return false

  return true
}
