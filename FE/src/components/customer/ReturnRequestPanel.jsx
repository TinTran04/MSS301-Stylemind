import React, { useState } from 'react'
import { RotateCcw, Truck, CheckCircle2, XCircle, Clock, AlertCircle } from 'lucide-react'
import Badge from '../common/Badge'
import { submitReturnShipment, cancelReturnRequest } from '../../features/orders/return.api'
import { formatDateTime } from '../../utils/formatDate'

const STATUS_CONFIG = {
  REQUESTED: { label: 'Chờ duyệt', variant: 'warning', icon: Clock },
  APPROVED: { label: 'Admin đã chấp nhận', variant: 'success', icon: CheckCircle2 },
  RETURN_IN_TRANSIT: { label: 'Đang gửi hàng về kho', variant: 'warning', icon: Truck },
  QC_PASSED: { label: 'QC Đạt (Đã hoàn tiền)', variant: 'success', icon: CheckCircle2 },
  QC_FAILED: { label: 'QC Thất bại (Trả hàng lại)', variant: 'error', icon: XCircle },
  REJECTED: { label: 'Bị từ chối', variant: 'error', icon: XCircle },
}

export default function ReturnRequestPanel({ returnRequest, onUpdate }) {
  const [carrier, setCarrier] = useState('')
  const [trackingCode, setTrackingCode] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [cancelling, setCancelling] = useState(false)
  const [error, setError] = useState('')

  if (!returnRequest) return null

  const config = STATUS_CONFIG[returnRequest.status] || { label: returnRequest.status, variant: 'default', icon: Clock }
  const Icon = config.icon

  const handleShipmentSubmit = async (e) => {
    e.preventDefault()
    if (!carrier.trim() || !trackingCode.trim()) {
      setError('Vui lòng nhập đầy đủ Đơn vị vận chuyển và Mã vận đơn.')
      return
    }
    setSubmitting(true)
    setError('')
    try {
      await submitReturnShipment(returnRequest.id, {
        carrier: carrier.trim(),
        trackingCode: trackingCode.trim(),
      })
      if (onUpdate) onUpdate()
    } catch (err) {
      setError(err?.response?.data?.message || 'Không thể cập nhật mã vận đơn.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleCancelReturn = async () => {
    if (!window.confirm('Bạn có chắc chắn muốn hủy yêu cầu trả hàng này?')) return
    setCancelling(true)
    try {
      await cancelReturnRequest(returnRequest.id)
      if (onUpdate) onUpdate()
    } catch (err) {
      alert(err?.response?.data?.message || 'Không thể hủy yêu cầu trả hàng.')
    } finally {
      setCancelling(false)
    }
  }

  return (
    <div className="rounded-2xl border border-emerald-200 bg-emerald-50/40 p-5 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-emerald-200/60 pb-3">
        <div className="flex items-center gap-2">
          <RotateCcw className="text-emerald-700" size={18} />
          <h4 className="font-semibold text-sm text-emerald-950">Yêu cầu Trả hàng & Hoàn tiền</h4>
        </div>
        <Badge variant={config.variant}>
          <span className="flex items-center gap-1">
            <Icon size={12} /> {config.label}
          </span>
        </Badge>
      </div>

      <div className="space-y-2 text-xs text-gray-700">
        <p><span className="font-semibold">Lý do:</span> {returnRequest.reason}</p>
        {returnRequest.customerNote && <p><span className="font-semibold">Ghi chú của bạn:</span> {returnRequest.customerNote}</p>}
        <p><span className="font-semibold">Thời gian tạo:</span> {formatDateTime(returnRequest.requestedAt)}</p>

        {returnRequest.adminNote && (
          <div className="mt-2 p-2.5 rounded-lg bg-white border border-emerald-100 text-emerald-900">
            <p className="font-semibold text-xs text-emerald-800">Phản hồi từ Admin:</p>
            <p className="mt-0.5 text-xs">{returnRequest.adminNote}</p>
          </div>
        )}

        {returnRequest.rejectionReason && (
          <div className="mt-2 p-2.5 rounded-lg bg-red-50 border border-red-200 text-red-800">
            <p className="font-semibold text-xs text-red-900">Lý do từ chối:</p>
            <p className="mt-0.5 text-xs">{returnRequest.rejectionReason}</p>
          </div>
        )}
      </div>

      {/* Form nhập vận đơn trả hàng nếu Admin đã APPROVED và cần gửi hàng */}
      {returnRequest.status === 'APPROVED' && returnRequest.isPhysicalReturn && !returnRequest.shipment && (
        <form onSubmit={handleShipmentSubmit} className="mt-3 bg-white p-4 rounded-xl border border-emerald-200 space-y-3">
          <p className="font-semibold text-xs text-emerald-900 flex items-center gap-1.5">
            <Truck size={14} /> Gửi bưu gửi trả hàng về kho:
          </p>
          <p className="text-xs text-gray-500">Admin đã phê duyệt. Vui lòng gửi hàng và nhập mã vận đơn bên dưới:</p>
          
          {error && <p className="text-xs text-red-600 font-medium">{error}</p>}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            <input
              type="text"
              placeholder="Đơn vị vận chuyển (VD: Viettel Post, GHTK)"
              value={carrier}
              onChange={(e) => setCarrier(e.target.value)}
              className="p-2 border border-gray-300 rounded-lg text-xs"
            />
            <input
              type="text"
              placeholder="Mã vận đơn"
              value={trackingCode}
              onChange={(e) => setTrackingCode(e.target.value)}
              className="p-2 border border-gray-300 rounded-lg text-xs"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full py-2 bg-emerald-600 text-white rounded-lg text-xs font-semibold hover:bg-emerald-700 transition-colors"
          >
            {submitting ? 'Đang cập nhật...' : 'Xác nhận Đã Gửi Hàng'}
          </button>
        </form>
      )}

      {/* Thông tin vận đơn đã gửi */}
      {returnRequest.shipment && (
        <div className="bg-white p-3 rounded-xl border border-emerald-200 text-xs space-y-1">
          <p className="font-semibold text-emerald-900 flex items-center gap-1">
            <Truck size={14} /> Thông tin bưu gửi trả:
          </p>
          <p><span className="text-gray-500">Đơn vị:</span> {returnRequest.shipment.carrier}</p>
          <p><span className="text-gray-500">Mã vận đơn:</span> <span className="font-mono font-semibold">{returnRequest.shipment.trackingCode}</span></p>
        </div>
      )}

      {/* Nút hủy yêu cầu trả hàng nếu đang ở trạng thái REQUESTED */}
      {returnRequest.status === 'REQUESTED' && (
        <div className="pt-2">
          <button
            type="button"
            disabled={cancelling}
            onClick={handleCancelReturn}
            className="text-xs text-red-600 hover:text-red-700 underline font-medium"
          >
            {cancelling ? 'Đang hủy...' : 'Hủy yêu cầu trả hàng này'}
          </button>
        </div>
      )}
    </div>
  )
}
