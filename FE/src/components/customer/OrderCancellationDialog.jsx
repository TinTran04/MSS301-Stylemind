import { useEffect, useMemo, useState } from 'react'
import Modal from '../common/Modal'
import { CUSTOMER_CANCELLATION_REASONS } from '../../features/orders/order-cancellation.constants'
import { validateCancellationDialogInput } from '../../features/orders/order-cancellation.utils'

function getInitialReasonCode(reasonOptions) {
  return reasonOptions.find((reason) => reason.value === 'OTHER')?.value || reasonOptions[0]?.value || ''
}

export default function OrderCancellationDialog({
  isOpen,
  title = 'Hủy đơn hàng',
  confirmLabel = 'Xác nhận',
  cancelLabel = 'Đóng',
  loading = false,
  reasonOptions = CUSTOMER_CANCELLATION_REASONS,
  noteLabel = 'Ghi chú',
  noteFieldName = 'customerNote',
  notePlaceholder,
  noteRequired = false,
  noteRequiredMessage,
  otherNoteRequired = true,
  onClose,
  onConfirm,
}) {
  const [reasonCode, setReasonCode] = useState(getInitialReasonCode(reasonOptions))
  const [note, setNote] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (isOpen) {
      setReasonCode(getInitialReasonCode(reasonOptions))
      setNote('')
      setError('')
    }
  }, [isOpen, reasonOptions])

  const selectedReason = useMemo(
    () => reasonOptions.find((item) => item.value === reasonCode) || reasonOptions[0],
    [reasonCode, reasonOptions]
  )

  const handleSubmit = () => {
    const trimmedNote = note.trim()
    const validationMessage = validateCancellationDialogInput({
      reasonCode,
      note: trimmedNote,
      noteRequired,
      noteRequiredMessage,
      otherNoteRequired,
    })
    if (validationMessage) {
      setError(validationMessage)
      return
    }

    onConfirm({
      reasonCode,
      [noteFieldName]: trimmedNote || undefined,
    })
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} className="max-w-xl">
      <div className="space-y-4">
        <div>
          <label className="mb-2 block text-sm font-medium text-on-surface">Lý do hủy</label>
          <select
            value={reasonCode}
            onChange={(event) => {
              setReasonCode(event.target.value)
              if (error) setError('')
            }}
            disabled={loading}
            className="w-full rounded-xl border border-outline-variant/30 bg-surface-container-low px-4 py-3 text-sm text-on-surface outline-none focus:border-primary disabled:opacity-60"
          >
            {reasonOptions.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
          </select>
        </div>
        <div>
          <label className="mb-2 block text-sm font-medium text-on-surface">{noteLabel}</label>
          <textarea
            value={note}
            onChange={(event) => {
              setNote(event.target.value)
              if (error) setError('')
            }}
            rows={4}
            disabled={loading}
            className="w-full rounded-xl border border-outline-variant/30 bg-surface-container-low px-4 py-3 text-sm text-on-surface outline-none focus:border-primary disabled:opacity-60"
            placeholder={notePlaceholder || (selectedReason?.value === 'OTHER' ? 'Vui lòng mô tả lý do...' : 'Có thể để trống')}
          />
        </div>
        {error && (
          <p role="alert" className="rounded-lg bg-error-container/40 px-3 py-2 text-sm font-medium text-error">
            {error}
          </p>
        )}
        <div className="flex flex-wrap justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="rounded-full border border-outline-variant/30 px-4 py-2 text-sm font-medium text-on-surface-variant disabled:opacity-50"
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={loading}
            className="rounded-full bg-primary px-4 py-2 text-sm font-medium text-on-primary disabled:opacity-50"
          >
            {loading ? 'Đang xử lý...' : confirmLabel}
          </button>
        </div>
      </div>
    </Modal>
  )
}
