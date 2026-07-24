import { useEffect, useMemo, useState } from 'react'
import Modal from '../common/Modal'
import { CUSTOMER_CANCELLATION_REASONS } from '../../features/orders/order-cancellation.constants'
import { buildCancellationPayload } from '../../features/orders/order-cancellation.utils'

export default function OrderCancellationDialog({
  isOpen,
  title = 'Hủy đơn hàng',
  confirmLabel = 'Xác nhận',
  cancelLabel = 'Đóng',
  loading = false,
  onClose,
  onConfirm,
}) {
  const [reasonCode, setReasonCode] = useState('OTHER')
  const [note, setNote] = useState('')

  useEffect(() => {
    if (isOpen) {
      setReasonCode('OTHER')
      setNote('')
    }
  }, [isOpen])

  const selectedReason = useMemo(
    () => CUSTOMER_CANCELLATION_REASONS.find((item) => item.value === reasonCode) || CUSTOMER_CANCELLATION_REASONS[0],
    [reasonCode]
  )
  const canSubmit = reasonCode !== 'OTHER' || note.trim().length > 0

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} className="max-w-xl">
      <div className="space-y-4">
        <div>
          <label className="mb-2 block text-sm font-medium text-on-surface">Lý do hủy</label>
          <select
            value={reasonCode}
            onChange={(event) => setReasonCode(event.target.value)}
            className="w-full rounded-xl border border-outline-variant/30 bg-surface-container-low px-4 py-3 text-sm text-on-surface outline-none focus:border-primary"
          >
            {CUSTOMER_CANCELLATION_REASONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
          </select>
        </div>
        <div>
          <label className="mb-2 block text-sm font-medium text-on-surface">Ghi chú</label>
          <textarea
            value={note}
            onChange={(event) => setNote(event.target.value)}
            rows={4}
            className="w-full rounded-xl border border-outline-variant/30 bg-surface-container-low px-4 py-3 text-sm text-on-surface outline-none focus:border-primary"
            placeholder={selectedReason?.value === 'OTHER' ? 'Vui lòng mô tả lý do...' : 'Có thể để trống'}
          />
        </div>
        <div className="flex flex-wrap justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="rounded-full border border-outline-variant/30 px-4 py-2 text-sm font-medium text-on-surface-variant"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={() => onConfirm(buildCancellationPayload(reasonCode, note.trim()))}
            disabled={loading || !canSubmit}
            className="rounded-full bg-primary px-4 py-2 text-sm font-medium text-on-primary"
          >
            {loading ? 'Đang xử lý...' : confirmLabel}
          </button>
        </div>
      </div>
    </Modal>
  )
}
