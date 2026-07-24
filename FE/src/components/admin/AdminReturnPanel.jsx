import { useEffect, useState } from 'react'
import Modal from '../common/Modal'
import OrderReturnPanel from '../customer/OrderReturnPanel'

const IMAGE_ACCEPT = 'image/jpeg,image/png,image/webp'
const MAX_IMAGE_BYTES = 3 * 1024 * 1024
const ALLOWED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])

function validateImages(files) {
  const images = Array.from(files || [])
  if (images.length === 0) return 'Vui lòng tải lên ít nhất 1 ảnh.'
  if (images.some((file) => !ALLOWED_IMAGE_TYPES.has(file.type))) return 'Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP.'
  if (images.some((file) => file.size > MAX_IMAGE_BYTES)) return 'Ảnh không được vượt quá 3MB.'
  return ''
}

function ImageInput({ label, files, onChange, disabled }) {
  return (
    <div>
      <label className="mb-2 block text-sm font-medium text-on-surface">{label}</label>
      <input
        type="file"
        accept={IMAGE_ACCEPT}
        multiple
        disabled={disabled}
        onChange={(event) => {
          onChange(Array.from(event.target.files || []))
          event.target.value = ''
        }}
        className="block w-full rounded-xl border border-outline-variant/30 bg-surface-container-low px-4 py-3 text-sm text-on-surface file:mr-3 file:rounded-full file:border-0 file:bg-primary file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-on-primary"
      />
      {files.length > 0 && (
        <ul className="mt-2 space-y-1 text-xs text-on-surface-variant">
          {files.map((file) => <li key={`${file.name}-${file.size}`} className="break-all">{file.name}</li>)}
        </ul>
      )}
    </div>
  )
}

function RejectReturnDialog({ open, loading, onClose, onConfirm }) {
  const [reason, setReason] = useState('')
  const [images, setImages] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    if (!open) return
    setReason('')
    setImages([])
    setError('')
  }, [open])

  const submit = () => {
    const imageError = validateImages(images)
    if (!reason.trim()) {
      setError('Vui lòng nhập lý do từ chối hoàn hàng.')
      return
    }
    if (imageError) {
      setError(imageError)
      return
    }
    onConfirm({ rejectionReason: reason.trim(), images })
  }

  return (
    <Modal isOpen={open} onClose={onClose} title="Từ chối hoàn hàng" className="max-w-xl">
      <div className="space-y-4">
        <div>
          <label className="mb-2 block text-sm font-medium text-on-surface">Lý do từ chối</label>
          <textarea value={reason} onChange={(event) => { setReason(event.target.value); if (error) setError('') }} rows={4} disabled={loading} className="w-full rounded-xl border border-outline-variant/30 bg-surface-container-low px-4 py-3 text-sm outline-none focus:border-primary disabled:opacity-60" />
        </div>
        <ImageInput label="Ảnh phản hồi cho khách" files={images} onChange={(next) => { setImages(next); if (error) setError('') }} disabled={loading} />
        {error && <p role="alert" className="text-sm text-error">{error}</p>}
        <div className="flex flex-wrap justify-end gap-3">
          <button type="button" onClick={onClose} disabled={loading} className="rounded-lg px-4 py-2 text-sm font-medium text-on-surface-variant hover:bg-surface-container-high disabled:opacity-50">Đóng</button>
          <button type="button" onClick={submit} disabled={loading} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-on-primary disabled:opacity-50">{loading ? 'Đang xử lý...' : 'Xác nhận từ chối'}</button>
        </div>
      </div>
    </Modal>
  )
}

function CompleteReturnForm({ loading, onComplete }) {
  const [refundReference, setRefundReference] = useState('')
  const [refundNote, setRefundNote] = useState('')
  const [billImages, setBillImages] = useState([])
  const [error, setError] = useState('')

  const submit = () => {
    const imageError = validateImages(billImages)
    if (!refundReference.trim()) {
      setError('Vui lòng nhập mã bill hoặc mã giao dịch hoàn tiền.')
      return
    }
    if (imageError) {
      setError(imageError)
      return
    }
    onComplete({ refundReference: refundReference.trim(), refundNote: refundNote.trim(), billImages })
  }

  return (
    <div className="rounded-xl border border-tertiary-container/30 bg-tertiary-container/10 p-4">
      <h4 className="text-sm font-semibold text-primary">Hoàn tiền thủ công</h4>
      <div className="mt-3 space-y-3">
        <input value={refundReference} onChange={(event) => { setRefundReference(event.target.value); if (error) setError('') }} disabled={loading} placeholder="Mã bill / mã giao dịch hoàn tiền" className="w-full rounded-lg border border-outline-variant/30 bg-surface-container-lowest px-3 py-2 text-sm outline-none focus:border-primary disabled:opacity-60" />
        <textarea value={refundNote} onChange={(event) => setRefundNote(event.target.value)} disabled={loading} rows={3} placeholder="Ghi chú cho khách hàng" className="w-full rounded-lg border border-outline-variant/30 bg-surface-container-lowest px-3 py-2 text-sm outline-none focus:border-primary disabled:opacity-60" />
        <ImageInput label="Bill thanh toán" files={billImages} onChange={(next) => { setBillImages(next); if (error) setError('') }} disabled={loading} />
      </div>
      {error && <p role="alert" className="mt-2 text-sm text-error">{error}</p>}
      <button type="button" onClick={submit} disabled={loading} className="mt-3 inline-flex w-full justify-center rounded-lg bg-primary px-4 py-2 text-sm font-medium text-on-primary disabled:opacity-50">
        {loading ? 'Đang cập nhật...' : 'Xác nhận đã hoàn tiền'}
      </button>
    </div>
  )
}

export default function AdminReturnPanel({ returnRequest, loading = false, onApprove, onReject, onComplete }) {
  const [rejectOpen, setRejectOpen] = useState(false)

  if (!returnRequest) {
    return <p className="text-sm text-on-surface-variant">Chưa có yêu cầu hoàn hàng nào.</p>
  }

  const status = String(returnRequest.status || '').toUpperCase()
  const canReview = status === 'REQUESTED'
  const canComplete = status === 'BANK_INFO_SUBMITTED'

  const submitReject = async (payload) => {
    await onReject(payload)
    setRejectOpen(false)
  }

  return (
    <div className="space-y-4">
      <OrderReturnPanel returnRequest={returnRequest} />
      {canReview && (
        <div className="flex flex-wrap gap-3">
          <button type="button" onClick={onApprove} disabled={loading} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-on-primary disabled:opacity-50">
            Đồng ý hoàn hàng
          </button>
          <button type="button" onClick={() => setRejectOpen(true)} disabled={loading} className="rounded-lg border border-outline-variant/30 px-4 py-2 text-sm font-medium text-primary disabled:opacity-50">
            Từ chối
          </button>
        </div>
      )}
      {status === 'AWAITING_BANK_INFO' && (
        <p className="rounded-lg bg-tertiary-container/20 px-3 py-2 text-sm text-primary">Đang chờ khách hàng nhập thông tin ngân hàng.</p>
      )}
      {canComplete && <CompleteReturnForm loading={loading} onComplete={onComplete} />}
      <RejectReturnDialog open={rejectOpen} loading={loading} onClose={() => { if (!loading) setRejectOpen(false) }} onConfirm={submitReject} />
    </div>
  )
}
