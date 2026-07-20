const PHONE_SEPARATORS = /[\s().-]/g

export function normalizeVietnamesePhoneInput(value) {
  return String(value || '').replace(PHONE_SEPARATORS, '')
}

export function getVietnamesePhoneValidationMessage(value) {
  const normalized = normalizeVietnamesePhoneInput(value)
  if (!normalized) return 'Vui lòng nhập số điện thoại người nhận.'

  // This is a user-facing shape check only. User Service remains authoritative
  // and validates the parsed number with libphonenumber before persistence.
  if (!/^0\d{9}$/.test(normalized) && !/^\+84\d{9,10}$/.test(normalized)) {
    return 'Số điện thoại phải là số Việt Nam hợp lệ.'
  }

  return ''
}
