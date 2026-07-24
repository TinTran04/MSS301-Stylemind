import React, { useState } from 'react'
import Modal from '../common/Modal'

const RETURN_REASONS = [
  { value: 'DEFECTIVE', label: 'Sản phẩm lỗi / Kỹ thuật' },
  { value: 'DAMAGED', label: 'Sản phẩm bị hư hỏng khi vận chuyển' },
  { value: 'WRONG_ITEM', label: 'Gửi sai sản phẩm / Kích thước' },
  { value: 'MISSING_ITEM', label: 'Gửi thiếu sản phẩm' },
  { value: 'SIZE_NOT_FIT', label: 'Kích thước không vừa' },
  { value: 'CHANGED_MIND', label: 'Đổi ý không còn nhu cầu' },
  { value: 'OTHER', label: 'Lý do khác' },
]

export default function ReturnRequestModal({ isOpen, onClose, order, remainingQuantities = {}, onSubmit }) {
  const [selectedItems, setSelectedItems] = useState({})
  const [reason, setReason] = useState('DEFECTIVE')
  const [note, setNote] = useState('')
  const [evidenceUrl, setEvidenceUrl] = useState('')
  const [evidences, setEvidences] = useState([])
  const [bankCode, setBankCode] = useState('VCB')
  const [accountHolder, setAccountHolder] = useState('')
  const [accountNumber, setAccountNumber] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  if (!order) return null

  const handleItemQuantityChange = (itemId, qty) => {
    const parsed = parseInt(qty, 10)
    if (isNaN(parsed) || parsed <= 0) {
      const next = { ...selectedItems }
      delete next[itemId]
      setSelectedItems(next)
    } else {
      setSelectedItems({ ...selectedItems, [itemId]: parsed })
    }
  }

  const handleAddEvidence = () => {
    if (!evidenceUrl.trim()) return
    setEvidences([...evidences, { secureUrl: evidenceUrl.trim(), resourceType: 'image' }])
    setEvidenceUrl('')
  }

  const handleRemoveEvidence = (idx) => {
    setEvidences(evidences.filter((_, i) => i !== idx))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)

    const itemsPayload = Object.entries(selectedItems).map(([orderItemId, quantity]) => ({
      orderItemId,
      quantity,
    }))

    if (itemsPayload.length === 0) {
      setError('Vui lòng chọn ít nhất 1 sản phẩm cần trả hàng.')
      return
    }

    const isEvidenceRequired = ['DEFECTIVE', 'DAMAGED', 'WRONG_ITEM', 'MISSING_ITEM'].includes(reason)
    if (isEvidenceRequired && evidences.length === 0) {
      setError('Lý do chọn yêu cầu ít nhất 1 ảnh/video bằng chứng.')
      return
    }

    if (!accountHolder.trim() || !accountNumber.trim()) {
      setError('Vui lòng điền đầy đủ thông tin tài khoản ngân hàng để nhận tiền hoàn.')
      return
    }

    setLoading(true)
    try {
      await onSubmit({
        reason,
        customerNote: note.trim(),
        items: itemsPayload,
        evidences,
        payoutDestination: {
          bankCode,
          accountHolder: accountHolder.trim().toUpperCase(),
          accountNumber: accountNumber.trim(),
        },
      })
      onClose()
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'Có lỗi xảy ra khi tạo yêu cầu trả hàng.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Yêu cầu Trả hàng & Hoàn tiền">
      <form onSubmit={handleSubmit} className="space-y-4 max-h-[75vh] overflow-y-auto pr-2 text-sm">
        {error && (
          <div className="p-3 bg-red-50 text-red-600 rounded-lg text-xs font-medium border border-red-200">
            {error}
          </div>
        )}

        <div>
          <label className="block font-semibold mb-1">1. Chọn sản phẩm cần trả</label>
          <div className="space-y-2 max-h-48 overflow-y-auto border border-gray-200 rounded-lg p-2 bg-gray-50">
            {order.items?.map((item) => {
              const remaining = remainingQuantities[item.id] !== undefined ? remainingQuantities[item.id] : item.quantity
              const isSelected = !!selectedItems[item.id]

              return (
                <div key={item.id} className="flex items-center justify-between bg-white p-2.5 rounded border border-gray-100 shadow-sm">
                  <div className="flex-1 pr-2">
                    <p className="font-medium text-gray-800 line-clamp-1">{item.name || item.productName || `Món hàng #${item.id}`}</p>
                    <p className="text-xs text-gray-500">
                      Mua: {item.quantity} | Còn có thể trả: <span className="font-semibold text-emerald-600">{remaining}</span>
                    </p>
                  </div>
                  {remaining > 0 ? (
                    <input
                      type="number"
                      min="0"
                      max={remaining}
                      placeholder="0"
                      value={selectedItems[item.id] || ''}
                      onChange={(e) => handleItemQuantityChange(item.id, e.target.value)}
                      className="w-16 px-2 py-1 border border-gray-300 rounded text-center font-semibold text-gray-700"
                    />
                  ) : (
                    <span className="text-xs text-red-500 font-medium italic">Đã hết số lượng</span>
                  )}
                </div>
              )
            })}
          </div>
        </div>

        <div>
          <label className="block font-semibold mb-1">2. Lý do trả hàng</label>
          <select
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg font-medium text-gray-800"
          >
            {RETURN_REASONS.map((r) => (
              <option key={r.value} value={r.value}>
                {r.label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block font-semibold mb-1">3. Ghi chú thêm</label>
          <textarea
            rows="2"
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder="Mô tả chi tiết tình trạng sản phẩm..."
            className="w-full px-3 py-2 border border-gray-300 rounded-lg"
          />
        </div>

        <div>
          <label className="block font-semibold mb-1">
            4. Ảnh / Video bằng chứng {['DEFECTIVE', 'DAMAGED', 'WRONG_ITEM', 'MISSING_ITEM'].includes(reason) && <span className="text-red-500">*</span>}
          </label>
          <div className="flex gap-2 mb-2">
            <input
              type="text"
              placeholder="Dán URL ảnh/video (ví dụ: Cloudinary URL)..."
              value={evidenceUrl}
              onChange={(e) => setEvidenceUrl(e.target.value)}
              className="flex-1 px-3 py-1.5 border border-gray-300 rounded-lg text-xs"
            />
            <button
              type="button"
              onClick={handleAddEvidence}
              className="px-3 py-1.5 bg-gray-800 text-white rounded-lg font-medium text-xs hover:bg-black"
            >
              Thêm URL
            </button>
          </div>

          {evidences.length > 0 && (
            <div className="flex flex-wrap gap-2 pt-1">
              {evidences.map((ev, i) => (
                <div key={i} className="relative group border border-gray-200 rounded p-1 bg-gray-50">
                  <img src={ev.secureUrl} alt="evidence" className="w-12 h-12 object-cover rounded" />
                  <button
                    type="button"
                    onClick={() => handleRemoveEvidence(i)}
                    className="absolute -top-1.5 -right-1.5 bg-red-500 text-white rounded-full w-4 h-4 text-[10px] flex items-center justify-center font-bold"
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="pt-2 border-t border-gray-200">
          <label className="block font-semibold mb-1 text-emerald-700">5. Thông tin Ngân hàng nhận tiền hoàn</label>
          <div className="grid grid-cols-2 gap-2">
            <div>
              <span className="text-xs font-medium text-gray-600">Ngân hàng</span>
              <select
                value={bankCode}
                onChange={(e) => setBankCode(e.target.value)}
                className="w-full px-2 py-1.5 border border-gray-300 rounded text-xs font-medium"
              >
                <option value="VCB">Vietcombank</option>
                <option value="MB">MBBank</option>
                <option value="TCB">Techcombank</option>
                <option value="ACB">ACB</option>
                <option value="VPB">VPBank</option>
                <option value="BIDV">BIDV</option>
                <option value="CTG">VietinBank</option>
              </select>
            </div>
            <div>
              <span className="text-xs font-medium text-gray-600">Chủ tài khoản (In hoa)</span>
              <input
                type="text"
                placeholder="NGUYEN VAN A"
                value={accountHolder}
                onChange={(e) => setAccountHolder(e.target.value.toUpperCase())}
                className="w-full px-2 py-1.5 border border-gray-300 rounded text-xs uppercase"
              />
            </div>
          </div>
          <div className="mt-2">
            <span className="text-xs font-medium text-gray-600">Số tài khoản</span>
            <input
              type="text"
              placeholder="0123456789"
              value={accountNumber}
              onChange={(e) => setAccountNumber(e.target.value)}
              className="w-full px-2 py-1.5 border border-gray-300 rounded text-xs"
            />
          </div>
        </div>

        <div className="pt-3 flex justify-end gap-2 border-t border-gray-100">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 bg-gray-100 text-gray-700 font-medium rounded-lg text-xs hover:bg-gray-200"
          >
            Hủy bỏ
          </button>
          <button
            type="submit"
            disabled={loading}
            className="px-4 py-2 bg-emerald-600 text-white font-semibold rounded-lg text-xs hover:bg-emerald-700 disabled:opacity-50"
          >
            {loading ? 'Đang gửi...' : 'Gửi yêu cầu Trả hàng'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
