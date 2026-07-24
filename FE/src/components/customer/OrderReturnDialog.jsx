import { useEffect, useState } from 'react'
import Modal from '../common/Modal'
import { RETURN_REASON_OPTIONS } from '../../features/orders/order-return.utils'

const ACCEPTED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])
const IMAGE_ACCEPT = 'image/jpeg,image/png,image/webp'
const MAX_IMAGE_BYTES = 3 * 1024 * 1024
const MAX_IMAGES = 5

export default function OrderReturnDialog({ isOpen, loading = false, onClose, onConfirm }) {
  const [reasonCode, setReasonCode] = useState('PRODUCT_NOT_AS_DESCRIBED')
  const [note, setNote] = useState('')
  const [images, setImages] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    if (!isOpen) return
    setReasonCode('PRODUCT_NOT_AS_DESCRIBED')
    setNote('')
    setImages([])
    setError('')
  }, [isOpen])

  const handleFiles = (event) => {
    const files = Array.from(event.target.files || [])
    event.target.value = ''
    setError('')
    if (files.length > MAX_IMAGES) {
      setError('Chỉ hỗ trợ tối đa 5 ảnh bằng chứng.')
      return
    }
    const invalidType = files.find((file) => !ACCEPTED_IMAGE_TYPES.has(file.type))
    if (invalidType) {
      setError('Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP.')
      return
    }
    const oversized = files.find((file) => file.size > MAX_IMAGE_BYTES)
    if (oversized) {
      setError('Ảnh bằng chứng không được vượt quá 3MB.')
      return
    }
    setImages(files)
  }

  const handleSubmit = () => {
    const trimmedNote = note.trim()
    if (images.length === 0) {
      setError('Vui lòng tải lên ít nhất 1 ảnh bằng chứng.')
      return
    }
    if (reasonCode === 'OTHER' && !trimmedNote) {
      setError('Vui lòng nhập ghi chú khi chọn lý do khác.')
      return
    }
    onConfirm({ reasonCode, customerNote: trimmedNote, images })
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Yêu cầu hoàn hàng" className="max-w-xl">
      <div className="space-y-4">
        <div>
          <label className="mb-2 block text-sm font-medium text-on-surface">Lý do hoàn hàng</label>
          <select
            value={reasonCode}
            onChange={(event) => setReasonCode(event.target.value)}
            disabled={loading}
            className="w-full rounded-xl border border-outline-variant/30 bg-surface-container-low px-4 py-3 text-sm text-on-surface outline-none focus:border-primary disabled:opacity-60"
          >
            {RETURN_REASON_OPTIONS.map((reason) => <option key={reason.value} value={reason.value}>{reason.label}</option>)}
          </select>
        </div>
        <div>
          <label className="mb-2 block text-sm font-medium text-on-surface">Ghi chú</label>
          <textarea
            value={note}
            onChange={(event) => setNote(event.target.value)}
            rows={4}
            disabled={loading}
            className="w-full rounded-xl border border-outline-variant/30 bg-surface-container-low px-4 py-3 text-sm text-on-surface outline-none focus:border-primary disabled:opacity-60"
            placeholder={reasonCode === 'OTHER' ? 'Vui lòng mô tả lý do hoàn hàng...' : 'Có thể để trống'}
          />
        </div>
        <div>
          <label className="mb-2 block text-sm font-medium text-on-surface">Ảnh bằng chứng</label>
          <input
            type="file"
            accept={IMAGE_ACCEPT}
            multiple
            disabled={loading}
            onChange={handleFiles}
            className="block w-full rounded-xl border border-outline-variant/30 bg-surface-container-low px-4 py-3 text-sm text-on-surface file:mr-3 file:rounded-full file:border-0 file:bg-primary file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-on-primary"
          />
          <p className="mt-2 text-xs text-on-surface-variant">Bắt buộc ít nhất 1 ảnh. Hỗ trợ JPG, PNG, WEBP, tối đa 3MB/ảnh.</p>
          {images.length > 0 && (
            <ul className="mt-2 space-y-1 text-xs text-on-surface-variant">
              {images.map((file) => <li key={`${file.name}-${file.size}`} className="break-all">{file.name}</li>)}
            </ul>
          )}
        </div>
        {error && <p role="alert" className="text-sm text-error">{error}</p>}
        <div className="flex flex-wrap justify-end gap-3">
          <button type="button" onClick={onClose} disabled={loading} className="rounded-full border border-outline-variant/30 px-4 py-2 text-sm font-medium text-on-surface-variant disabled:opacity-50">
            Đóng
          </button>
          <button type="button" onClick={handleSubmit} disabled={loading} className="rounded-full bg-primary px-4 py-2 text-sm font-medium text-on-primary disabled:opacity-50">
            {loading ? 'Đang gửi...' : 'Gửi yêu cầu'}
          </button>
        </div>
      </div>
    </Modal>
  )
}
