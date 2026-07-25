import clsx from 'clsx'

const statusConfig = {
  synced: { color: 'bg-green-status/10 text-green-status', dot: 'bg-green-status', label: 'Đã đồng bộ' },
  processing: { color: 'bg-tertiary-fixed/30 text-tertiary', dot: 'bg-tertiary', label: 'Đang xử lý' },
  pending: { color: 'bg-surface-container-high text-on-surface-variant', dot: 'bg-on-surface-variant', label: 'Đang chờ' },
  approved: { color: 'bg-green-status/10 text-green-status', dot: 'bg-green-status', label: 'Đã duyệt' },
  payment_pending: { color: 'bg-tertiary-fixed/30 text-tertiary', dot: 'bg-tertiary', label: 'Chờ thanh toán' },
  pending_payment: { color: 'bg-tertiary-fixed/30 text-tertiary', dot: 'bg-tertiary', label: 'Chờ thanh toán' },
  failed: { color: 'bg-error-container text-error', dot: 'bg-error', label: 'Thất bại' },
  cancelled: { color: 'bg-error-container text-error', dot: 'bg-error', label: 'Đã hủy' },
  completed: { color: 'bg-green-status/10 text-green-status', dot: 'bg-green-status', label: 'Đã giao thành công' },
  fulfilled: { color: 'bg-green-status/10 text-green-status', dot: 'bg-green-status', label: 'Đã giao thành công' },
  in_stock: { color: 'bg-green-status/10 text-green-status', dot: 'bg-green-status', label: 'Còn hàng' },
  low_stock: { color: 'bg-tertiary-fixed/30 text-tertiary', dot: 'bg-tertiary', label: 'Sắp hết hàng' },
  out_of_stock: { color: 'bg-error-container text-error', dot: 'bg-error', label: 'Hết hàng' },
  shipped: { color: 'bg-tertiary-fixed/30 text-tertiary', dot: 'bg-tertiary', label: 'Đang giao' },
  delivered: { color: 'bg-green-status/10 text-green-status', dot: 'bg-green-status', label: 'Đã giao thành công' },
  confirmed: { color: 'bg-blue-50 text-blue-600', dot: 'bg-blue-500', label: 'Đã xác nhận' },
  processing_order: { color: 'bg-tertiary-fixed/30 text-tertiary', dot: 'bg-tertiary', label: 'Đang xử lý' },
  paid: { color: 'bg-blue-50 text-blue-600', dot: 'bg-blue-500', label: 'Đã thanh toán' },
  expired: { color: 'bg-error-container text-error', dot: 'bg-error', label: 'Đã hết hạn' },
}

export default function StatusBadge({ status, label: labelOverride }) {
  const config = statusConfig[status] || statusConfig.pending
  const label = labelOverride || config.label || 'Không xác định'

  return (
    <span className={clsx('inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium', config.color)}>
      <span className={clsx('w-1.5 h-1.5 rounded-full', config.dot)} />
      {label}
    </span>
  )
}
