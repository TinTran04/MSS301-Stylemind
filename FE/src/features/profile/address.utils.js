export function buildAddressPayload(form) {
  return {
    recipientName: String(form.recipientName || '').trim(),
    phoneNumber: String(form.phoneNumber || '').trim(),
    provinceCode: form.provinceCode || '',
    wardCode: form.wardCode || '',
    addressLine: String(form.addressLine || '').trim(),
    shippingNote: String(form.shippingNote || '').trim() || null,
    isDefault: Boolean(form.isDefault),
  }
}

export function isCheckoutEligibleAddress(address) {
  return address?.validationStatus === 'VALID' && Boolean(address.id)
}

export function formatSavedAddress(address) {
  const parts = [address?.addressLine, address?.wardName, address?.provinceName || address?.city]
    .filter((part) => typeof part === 'string' && part.trim())
  return parts.length ? parts.join(', ') : 'Chưa có địa chỉ'
}
