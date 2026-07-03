const PASSWORD_RESET_CONTEXT_KEY = 'password_reset_context'

export function getPasswordResetContext() {
  const rawContext = sessionStorage.getItem(PASSWORD_RESET_CONTEXT_KEY)
  if (!rawContext) return null

  try {
    return JSON.parse(rawContext)
  } catch {
    clearPasswordResetContext()
    return null
  }
}

export function setPasswordResetContext(context) {
  sessionStorage.setItem(PASSWORD_RESET_CONTEXT_KEY, JSON.stringify(context))
}

export function clearPasswordResetContext() {
  sessionStorage.removeItem(PASSWORD_RESET_CONTEXT_KEY)
}
