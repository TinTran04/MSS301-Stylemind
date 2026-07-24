import { useEffect, useState } from 'react'
import Badge from '../common/Badge'
import { formatRefundStatus } from '../../features/orders/order-cancellation.utils'
import { formatCurrency } from '../../utils/formatCurrency'
import { formatDateTime } from '../../utils/formatDate'

function Field({ label, value }) {
  if (value === null || value === undefined || value === '') return null
  return (
    <div className="flex items-start justify-between gap-4 border-t border-outline-variant/10 pt-2 first:border-0 first:pt-0">
      <dt className="text-xs text-on-surface-variant">{label}</dt>
      <dd className="max-w-[65%] text-right text-xs leading-5 text-on-surface break-words">{value}</dd>
    </div>
  )
}

export default function AdminRefundPanel({ orderId, refund, loading = false, onComplete, onFail }) {
  const [providerReference, setProviderReference] = useState('')
  const [proofUrl, setProofUrl] = useState('')
  const [note, setNote] = useState('')
  const [failureReason, setFailureReason] = useState('')

  useEffect(() => {
    setProviderReference(refund?.providerReference || '')
    setProofUrl(refund?.proofUrl || '')
    setNote(refund?.note || '')
    setFailureReason(refund?.failureReason || '')
  }, [refund?.id, orderId])

  if (!refund) return null

  const isPending = refund.status === 'REFUND_PENDING'

  return (
    <section className="space-y-4">
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

      <dl className="space-y-2 rounded-xl bg-surface-container-low p-3">
        <Field label="Số tiền" value={formatCurrency(refund.amount)} />
        <Field label="Phương thức" value={refund.method} />
        <Field label="Mã hoàn tiền" value={refund.id} />
        <Field label="Mã giao dịch hoàn" value={refund.providerReference} />
        <Field label="Bằng chứng" value={refund.proofUrl} />
        <Field label="Ghi chú" value={refund.note} />
        <Field label="Xử lý bởi" value={refund.processedBy} />
        <Field label="Xử lý lúc" value={formatDateTime(refund.processedAt)} />
        <Field label="Lý do thất bại" value={refund.failureReason} />
      </dl>

      {isPending && (
        <div className="space-y-3 rounded-xl border border-outline-variant/20 bg-surface-container-lowest p-4">
          <div>
            <label className="mb-1 block text-xs font-medium text-on-surface-variant">Mã giao dịch hoàn tiền</label>
            <input
              value={providerReference}
              onChange={(event) => setProviderReference(event.target.value)}
              placeholder="Nhập mã giao dịch đối soát"
              className="w-full rounded-lg border border-outline-variant/30 bg-surface-container-low px-3 py-2 text-sm text-on-surface outline-none focus:border-tertiary-container"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-on-surface-variant">URL bằng chứng</label>
            <input
              value={proofUrl}
              onChange={(event) => setProofUrl(event.target.value)}
              placeholder="Tùy chọn"
              className="w-full rounded-lg border border-outline-variant/30 bg-surface-container-low px-3 py-2 text-sm text-on-surface outline-none focus:border-tertiary-container"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-on-surface-variant">Ghi chú</label>
            <textarea
              value={note}
              onChange={(event) => setNote(event.target.value)}
              rows={3}
              className="w-full rounded-lg border border-outline-variant/30 bg-surface-container-low px-3 py-2 text-sm text-on-surface outline-none focus:border-tertiary-container"
              placeholder="Tùy chọn"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-on-surface-variant">Lý do thất bại</label>
            <textarea
              value={failureReason}
              onChange={(event) => setFailureReason(event.target.value)}
              rows={3}
              className="w-full rounded-lg border border-outline-variant/30 bg-surface-container-low px-3 py-2 text-sm text-on-surface outline-none focus:border-tertiary-container"
              placeholder="Dùng khi ghi nhận hoàn tiền thất bại"
            />
          </div>
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => onComplete?.({ providerReference, proofUrl: proofUrl || undefined, note: note || undefined })}
              disabled={loading || !providerReference.trim()}
              className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-on-primary disabled:opacity-50"
            >
              Xác nhận hoàn tiền
            </button>
            <button
              type="button"
              onClick={() => onFail?.({ failureReason })}
              disabled={loading || !failureReason.trim()}
              className="rounded-lg border border-outline-variant/30 px-4 py-2 text-sm font-medium text-primary disabled:opacity-50"
            >
              Ghi nhận thất bại
            </button>
          </div>
        </div>
      )}
    </section>
  )
}
