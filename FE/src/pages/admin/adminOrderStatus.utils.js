import {
  formatStatusLabel,
  getAvailableTransitions,
  normalizeOrderStatus,
  ORDER_STATUS_LABELS,
} from '../../features/orders/orderStatus.js'

const KNOWN_STATUS_KEYS = new Set(Object.keys(ORDER_STATUS_LABELS))

export function getAdminOrderStatusOptions(order) {
  const currentStatus = normalizeOrderStatus(order?.orderStatus)
  const seen = new Set()

  return getAvailableTransitions(order)
    .map(normalizeOrderStatus)
    .filter((status) => {
      if (!KNOWN_STATUS_KEYS.has(status) || status === currentStatus || seen.has(status)) return false
      seen.add(status)
      return true
    })
    .map((value) => ({ value, label: formatStatusLabel(value) }))
}

export function getStatusUpdateErrorMessage(error) {
  if (error?.status === 409 || error?.errorCode === 'INVALID_ORDER_STATUS_TRANSITION') {
    return {
      title: 'Trạng thái đã thay đổi',
      message: 'Trạng thái đơn hàng đã thay đổi. Dữ liệu sẽ được tải lại.',
      shouldRefetch: true,
    }
  }

  if (error?.status === 403) {
    return {
      title: 'Không có quyền thao tác',
      message: 'Bạn không có quyền cập nhật trạng thái đơn hàng.',
      shouldRefetch: false,
    }
  }

  if (error?.status === 404) {
    return {
      title: 'Không tìm thấy đơn hàng',
      message: 'Đơn hàng không còn tồn tại. Vui lòng tải lại danh sách.',
      shouldRefetch: false,
    }
  }

  if (error?.status === 400) {
    return {
      title: 'Không thể cập nhật trạng thái',
      message: error.message || 'Dữ liệu trạng thái không hợp lệ.',
      shouldRefetch: false,
    }
  }

  return {
    title: 'Không thể cập nhật trạng thái',
    message: 'Không thể kết nối tới máy chủ. Vui lòng thử lại sau.',
    shouldRefetch: false,
  }
}
