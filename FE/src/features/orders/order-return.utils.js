import { formatStatusLabel, normalizeOrderStatus } from './orderStatus.js'

export const RETURN_REASON_OPTIONS = [
  { value: 'PRODUCT_NOT_AS_DESCRIBED', label: 'Sản phẩm không đúng mô tả' },
  { value: 'DEFECTIVE_PRODUCT', label: 'Sản phẩm bị lỗi' },
  { value: 'WRONG_PRODUCT', label: 'Giao sai sản phẩm' },
  { value: 'SIZE_NOT_FIT', label: 'Kích cỡ không phù hợp' },
  { value: 'OTHER', label: 'Khác' },
]

const RETURN_STATUS_LABELS = {
  REQUESTED: 'Yêu cầu hoàn hàng',
  AWAITING_BANK_INFO: 'Chờ thông tin ngân hàng',
  BANK_INFO_SUBMITTED: 'Chờ xử lí hoàn tiền',
  REFUNDED: 'Đã hoàn hàng',
  REJECTED: 'Từ chối hoàn hàng',
}

const RETURN_STATUS_VARIANTS = {
  REQUESTED: 'warning',
  AWAITING_BANK_INFO: 'warning',
  BANK_INFO_SUBMITTED: 'warning',
  REFUNDED: 'success',
  REJECTED: 'error',
}

const ORDER_VISIBLE_RETURN_STATUSES = new Set([
  'REQUESTED',
  'AWAITING_BANK_INFO',
  'BANK_INFO_SUBMITTED',
  'REFUNDED',
])

export function formatReturnReason(reasonCode) {
  const key = String(reasonCode || '').toUpperCase()
  return RETURN_REASON_OPTIONS.find((reason) => reason.value === key)?.label || key || 'Không xác định'
}

export function formatReturnStatus(status) {
  const key = String(status || '').toUpperCase()
  return RETURN_STATUS_LABELS[key] || 'Không xác định'
}

export function getReturnStatusVariant(status) {
  const key = String(status || '').toUpperCase()
  return RETURN_STATUS_VARIANTS[key] || 'secondary'
}

export function getOrderReturnDisplay(order) {
  const status = String(order?.latestReturnRequest?.status || '').toUpperCase()
  if (!ORDER_VISIBLE_RETURN_STATUSES.has(status)) {
    return { label: '', variant: 'default', visible: false }
  }

  return {
    label: formatReturnStatus(status),
    variant: getReturnStatusVariant(status),
    visible: true,
  }
}

const ORDER_STATUS_VARIANTS = {
  COMPLETED: 'success',
  SHIPPED: 'warning',
  PROCESSING: 'secondary',
  CONFIRMED: 'secondary',
  PAID: 'success',
  PENDING: 'default',
  PAYMENT_PENDING: 'warning',
  CANCELLED: 'error',
  EXPIRED: 'default',
  FAILED: 'error',
}

export function getOrderStatusDisplay(order) {
  const returnDisplay = getOrderReturnDisplay(order)
  if (returnDisplay.visible) {
    return {
      label: returnDisplay.label,
      variant: returnDisplay.variant,
      visible: true,
      source: 'return',
    }
  }

  const status = normalizeOrderStatus(order?.orderStatus || order?.status)
  return {
    label: formatStatusLabel(status),
    variant: ORDER_STATUS_VARIANTS[status] || 'default',
    visible: true,
    source: 'order',
  }
}

export function canRequestCodReturn(order) {
  const status = String(order?.status || order?.orderStatus || '').toUpperCase()
  const method = String(order?.paymentMethod || '').toLowerCase()
  return status === 'COMPLETED' && method === 'cod' && !order?.latestReturnRequest
}

export function needsReturnBankInfo(returnRequest) {
  return String(returnRequest?.status || '').toUpperCase() === 'AWAITING_BANK_INFO'
}

export function getManualBankInfoValidationMessage(form) {
  if (!String(form?.bankName || '').trim()
    || !String(form?.bankAccountNumber || '').trim()
    || !String(form?.bankAccountHolder || '').trim()) {
    return 'Vui lòng nhập đầy đủ tên ngân hàng, số tài khoản và chủ tài khoản.'
  }
  return ''
}

export function toManualBankInfoPayload(form) {
  return {
    bankName: String(form?.bankName || '').trim(),
    bankAccountNumber: String(form?.bankAccountNumber || '').trim(),
    bankAccountHolder: String(form?.bankAccountHolder || '').trim(),
  }
}

export function groupReturnAttachments(returnRequest) {
  const attachments = Array.isArray(returnRequest?.attachments) ? returnRequest.attachments : []
  return {
    customerProofs: attachments.filter((attachment) => attachment.kind === 'CUSTOMER_PROOF'),
    adminRejections: attachments.filter((attachment) => attachment.kind === 'ADMIN_REJECTION'),
    adminBills: attachments.filter((attachment) => attachment.kind === 'ADMIN_BILL'),
  }
}
