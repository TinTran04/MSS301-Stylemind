import clsx from 'clsx'

const statusConfig = {
  synced: { color: 'bg-green-status/10 text-green-status', dot: 'bg-green-status' },
  processing: { color: 'bg-tertiary-fixed/30 text-tertiary', dot: 'bg-tertiary' },
  pending: { color: 'bg-surface-container-high text-on-surface-variant', dot: 'bg-on-surface-variant' },
  failed: { color: 'bg-error-container text-error', dot: 'bg-error' },
  completed: { color: 'bg-green-status/10 text-green-status', dot: 'bg-green-status' },
  in_stock: { color: 'bg-green-status/10 text-green-status', dot: 'bg-green-status' },
  low_stock: { color: 'bg-tertiary-fixed/30 text-tertiary', dot: 'bg-tertiary' },
  out_of_stock: { color: 'bg-error-container text-error', dot: 'bg-error' },
  shipped: { color: 'bg-tertiary-fixed/30 text-tertiary', dot: 'bg-tertiary' },
  delivered: { color: 'bg-green-status/10 text-green-status', dot: 'bg-green-status' },
  confirmed: { color: 'bg-blue-50 text-blue-600', dot: 'bg-blue-500' },
  processing_order: { color: 'bg-tertiary-fixed/30 text-tertiary', dot: 'bg-tertiary' },
}

export default function StatusBadge({ status }) {
  const config = statusConfig[status] || statusConfig.pending
  const label = status?.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())

  return (
    <span className={clsx('inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium', config.color)}>
      <span className={clsx('w-1.5 h-1.5 rounded-full', config.dot)} />
      {label}
    </span>
  )
}
