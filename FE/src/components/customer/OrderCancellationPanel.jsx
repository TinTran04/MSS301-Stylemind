import Badge from '../common/Badge'
import { formatCancellationReason, formatCancellationStatus, formatCancellationType, formatRefundStatus } from '../../features/orders/order-cancellation.utils'
import { formatCurrency } from '../../utils/formatCurrency'
import { formatDateTime } from '../../utils/formatDate'

function DetailRow({ label, value }) {
  if (value === null || value === undefined || value === '') return null
  return (
    <div className="flex items-start justify-between gap-4 border-t border-outline-variant/10 pt-2 first:border-0 first:pt-0">
      <dt className="text-xs text-on-surface-variant">{label}</dt>
      <dd className="max-w-[65%] text-right text-xs leading-5 text-on-surface break-words">{value}</dd>
    </div>
  )
}

export default function OrderCancellationPanel({ cancellation, refund }) {
  if (!cancellation && !refund) return null

  return (
    <section className="space-y-4 rounded-2xl border border-outline-variant/20 bg-surface-container-lowest p-4">
      {cancellation && (
        <div className="space-y-3">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm font-medium text-primary">Yêu cầu hủy</p>
              <p className="text-xs text-on-surface-variant">{formatCancellationType(cancellation.cancellationType)}</p>
            </div>
            <Badge
              variant={cancellation.status === 'REQUESTED'
                ? 'warning'
                : cancellation.status === 'REJECTED'
                  ? 'error'
                  : 'success'}
            >
              {formatCancellationStatus(cancellation.status)}
            </Badge>
          </div>
          <dl className="space-y-2 rounded-xl bg-surface-container-low p-3">
            <DetailRow label="Lý do" value={formatCancellationReason(cancellation.reasonCode)} />
            <DetailRow label="Ghi chú khách" value={cancellation.customerNote} />
            <DetailRow label="Ghi chú admin" value={cancellation.adminNote} />
            <DetailRow label="Lý do từ chối" value={cancellation.rejectionReason} />
            <DetailRow label="Gửi lúc" value={formatDateTime(cancellation.requestedAt)} />
            <DetailRow label="Duyệt lúc" value={formatDateTime(cancellation.reviewedAt)} />
            <DetailRow label="Xác nhận lúc" value={formatDateTime(cancellation.approvedAt)} />
          </dl>
        </div>
      )}

      {refund && (
        <div className={cancellation ? 'border-t border-outline-variant/20 pt-4' : ''}>
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm font-medium text-primary">Hoàn tiền</p>
              <p className="text-xs text-on-surface-variant">{formatRefundStatus(refund.status)}</p>
            </div>
            <Badge
              variant={refund.status === 'REFUND_PENDING'
                ? 'warning'
                : refund.status === 'REFUNDED'
                  ? 'success'
                  : 'error'}
            >
              {formatRefundStatus(refund.status)}
            </Badge>
          </div>
          <dl className="mt-3 space-y-2 rounded-xl bg-surface-container-low p-3">
            <DetailRow label="Số tiền" value={formatCurrency(refund.amount)} />
            <DetailRow label="Phương thức" value={refund.method} />
            <DetailRow label="Mã giao dịch hoàn" value={refund.providerReference} />
            <DetailRow label="Bằng chứng" value={refund.proofUrl} />
            <DetailRow label="Ghi chú" value={refund.note} />
            <DetailRow label="Xử lý bởi" value={refund.processedBy} />
            <DetailRow label="Xử lý lúc" value={formatDateTime(refund.processedAt)} />
            <DetailRow label="Lý do thất bại" value={refund.failureReason} />
          </dl>
        </div>
      )}
    </section>
  )
}
