const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function normalizeEmailInput(email) {
  return String(email || '').trim().toLowerCase()
}

export function getEmailValidationMessage(email) {
  const value = String(email || '').trim()
  if (!value) return 'Vui lòng nhập email.'
  if (!EMAIL_PATTERN.test(value)) return 'Email không hợp lệ.'
  return ''
}

