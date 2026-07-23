export const CUSTOMER_CANCELLATION_REASONS = [
  { value: 'ORDERED_BY_MISTAKE', label: 'Đặt nhầm đơn' },
  { value: 'CHANGE_PRODUCT_VARIANT', label: 'Đổi mẫu / size / màu' },
  { value: 'CHANGE_DELIVERY_ADDRESS', label: 'Đổi địa chỉ giao hàng' },
  { value: 'CHANGE_PAYMENT_METHOD', label: 'Đổi phương thức thanh toán' },
  { value: 'NO_LONGER_NEEDED', label: 'Không còn nhu cầu' },
  { value: 'OTHER', label: 'Khác' },
]

export const ADMIN_CANCELLATION_REASONS = [
  { value: 'CUSTOMER_REQUESTED_OFFLINE', label: 'Khách yêu cầu qua kênh khác' },
  { value: 'PRODUCT_UNAVAILABLE', label: 'Hết hàng' },
  { value: 'INVALID_DELIVERY_INFORMATION', label: 'Thông tin giao hàng không hợp lệ' },
  { value: 'FRAUD_SUSPECTED', label: 'Nghi ngờ gian lận' },
  { value: 'DELIVERY_NOT_SUPPORTED', label: 'Không hỗ trợ giao tới khu vực này' },
  { value: 'SYSTEM_ERROR', label: 'Lỗi hệ thống' },
  { value: 'OTHER', label: 'Khác' },
]
