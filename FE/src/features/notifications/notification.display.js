import { parseBackendDate } from '../../utils/formatDate.js'

const STATUS_LABELS = {
  PENDING: 'Chờ gửi email',
  SENT: 'Email đã gửi',
  FAILED: 'Gửi email lỗi',
  SKIPPED: 'Không gửi email',
}

const TYPE_LABELS = {
  ORDER: 'Đơn hàng',
  ORDER_PAYMENT_PENDING: 'Đơn hàng chờ thanh toán',
  ORDER_CONFIRMED: 'Đơn hàng đã được xác nhận',
  ORDER_CONFIRMATION: 'Đơn hàng đã được xác nhận',
  ORDER_PAID: 'Thanh toán thành công',
  ORDER_PROCESSING: 'Đơn hàng đang xử lý',
  ORDER_SHIPPED: 'Đơn hàng đang giao',
  ORDER_COMPLETED: 'Đơn hàng đã giao thành công',
  ORDER_CANCELLED: 'Đơn hàng đã bị hủy',
  ORDER_EXPIRED: 'Thanh toán đã hết hạn',
  ORDER_FAILED: 'Đơn hàng không thành công',
  PAYMENT_SUCCESS: 'Thanh toán thành công',
  PAYMENT_FAILED: 'Thanh toán thất bại',
  REGISTER_OTP: 'Mã OTP đăng ký',
  FORGOT_PASSWORD_OTP: 'Mã OTP đặt lại mật khẩu',
  USER_INVITE: 'Thiết lập mật khẩu StyleMind',
  WELCOME: 'Chào mừng đến với StyleMind',
  SYSTEM: 'Hệ thống',
}

const LEGACY_TITLE_LABELS = {
  'Order confirmed': 'Đơn hàng đã được xác nhận',
  'Payment received': 'Thanh toán thành công',
  'Authentication failed': 'Xác thực thất bại',
  'Set password StyleMind': 'Thiết lập mật khẩu StyleMind',
}

export function normalizeNotificationCode(value) {
  return String(value || '').trim().toUpperCase()
}

export function formatNotificationStatus(status) {
  const normalized = normalizeNotificationCode(status)
  return STATUS_LABELS[normalized] || status || ''
}

export function formatNotificationTitle(notification) {
  const rawTitle = notification?.title || ''
  if (LEGACY_TITLE_LABELS[rawTitle]) return LEGACY_TITLE_LABELS[rawTitle]
  if (rawTitle) return rawTitle

  const type = normalizeNotificationCode(notification?.type)
  return TYPE_LABELS[type] || notification?.type || 'Thông báo'
}

function formatOrderReference(orderId) {
  const value = String(orderId || '').trim()
  return value.startsWith('#') ? value : `#${value}`
}

export function formatNotificationContent(notification) {
  const content = notification?.content || ''
  if (!content) return ''

  const type = normalizeNotificationCode(notification?.type)
  const title = notification?.title || ''
  const confirmedOrder = content.match(/^Your order\s+(.+?)\s+has been confirmed/i)
  if (confirmedOrder && (type === 'ORDER_CONFIRMED' || title === 'Order confirmed')) {
    return `Đơn hàng ${formatOrderReference(confirmedOrder[1])} của bạn đã được xác nhận. Bạn sẽ thanh toán khi nhận hàng.`
  }

  const paidOrder = content.match(/^Payment for your order\s+(.+?)\s+has been received/i)
  if (paidOrder && (type === 'ORDER_PAID' || title === 'Payment received')) {
    return `Thanh toán cho đơn hàng ${formatOrderReference(paidOrder[1])} đã được ghi nhận thành công.`
  }

  return content
}

export function getNotificationTimestamp(notification) {
  const value = notification?.sentAt || notification?.createdAt
  return parseBackendDate(value)?.getTime() || 0
}

export function sortNotificationsNewestFirst(notifications) {
  return [...(notifications || [])].sort((left, right) => {
    const byTime = getNotificationTimestamp(right) - getNotificationTimestamp(left)
    if (byTime !== 0) return byTime
    return Number(right?.id || 0) - Number(left?.id || 0)
  })
}

export function isOrderNotification(type) {
  const normalized = normalizeNotificationCode(type)
  return normalized.startsWith('ORDER') || normalized.startsWith('PAYMENT')
}
