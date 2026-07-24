export function isCancellationRequested(order) {
  return Boolean(order?.hasPendingCancellation || order?.latestCancellation?.status === 'REQUESTED')
}

export function canDirectCancel(order) {
  const status = String(order?.status || order?.orderStatus || '').toUpperCase()
  return ['pending', 'payment_pending'].includes(status.toLowerCase()) || ['PENDING', 'PAYMENT_PENDING'].includes(status)
}

export function canRequestCancellation(order) {
  const status = String(order?.status || order?.orderStatus || '').toUpperCase()
  if (String(order?.latestCancellation?.status || '').toUpperCase() === 'REJECTED') return false
  return ['PAID', 'CONFIRMED', 'PROCESSING'].includes(status)
}

export function formatCancellationStatus(status) {
  const key = String(status || '').toUpperCase()
  const labels = {
    REQUESTED: 'Đang chờ duyệt',
    APPROVED: 'Đã hủy',
    REJECTED: 'Từ chối hủy',
  }
  return labels[key] || 'Không xác định'
}

export function formatCancellationType(type) {
  const key = String(type || '').toUpperCase()
  const labels = {
    CUSTOMER_DIRECT: 'Khách hủy trực tiếp',
    CUSTOMER_REQUEST: 'Khách yêu cầu hủy',
    ADMIN_DIRECT: 'Quản trị hủy trực tiếp',
  }
  return labels[key] || 'Không xác định'
}

export function formatCancellationReason(reasonCode) {
  const key = String(reasonCode || '').toUpperCase()
  const labels = {
    ORDERED_BY_MISTAKE: 'Đặt nhầm đơn',
    CHANGE_PRODUCT_VARIANT: 'Đổi mẫu / size / màu',
    CHANGE_DELIVERY_ADDRESS: 'Đổi địa chỉ giao hàng',
    CHANGE_PAYMENT_METHOD: 'Đổi phương thức thanh toán',
    DELIVERY_TOO_SLOW: 'Giao hàng chậm',
    NO_LONGER_NEEDED: 'Không còn nhu cầu',
    OTHER: 'Khác',
    CUSTOMER_REQUESTED_OFFLINE: 'Khách yêu cầu qua kênh khác',
    PRODUCT_UNAVAILABLE: 'Hết hàng',
    INVALID_DELIVERY_INFORMATION: 'Thông tin giao hàng không hợp lệ',
    FRAUD_SUSPECTED: 'Nghi ngờ gian lận',
    DELIVERY_NOT_SUPPORTED: 'Không hỗ trợ giao tới khu vực này',
    SYSTEM_ERROR: 'Lỗi hệ thống',
  }
  return labels[key] || key || 'Không xác định'
}

export function formatRefundStatus(status) {
  const key = String(status || '').toUpperCase()
  const labels = {
    REFUND_PENDING: 'Chờ hoàn tiền',
    REFUNDED: 'Đã hoàn tiền',
    REFUND_FAILED: 'Hoàn tiền thất bại',
  }
  return labels[key] || 'Không xác định'
}

export function validateCancellationDialogInput({
  reasonCode,
  note,
  noteRequired = false,
  noteRequiredMessage = 'Vui lòng nhập ghi chú.',
  otherNoteRequired = true,
}) {
  const trimmedNote = String(note || '').trim()
  if (!reasonCode) return 'Vui lòng chọn lý do hủy đơn.'
  if (noteRequired && !trimmedNote) return noteRequiredMessage
  if (otherNoteRequired && reasonCode === 'OTHER' && !trimmedNote) {
    return 'Vui lòng nhập ghi chú khi chọn lý do khác.'
  }
  return ''
}

export function buildCancellationPayload(reasonCode, note) {
  return {
    reasonCode,
    customerNote: note || undefined,
  }
}
