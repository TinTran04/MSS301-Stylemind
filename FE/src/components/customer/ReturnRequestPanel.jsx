import React, { useEffect, useState } from 'react'
import { RotateCcw, Truck, CheckCircle2, XCircle, Clock, CreditCard } from 'lucide-react'
import Badge from '../common/Badge'
import { submitReturnShipment, cancelReturnRequest, getPayoutDestination, savePayoutDestination } from '../../features/orders/return.api'
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

  const [payoutInfo, setPayoutInfo] = useState(null)
  const [bankCode, setBankCode] = useState('VCB')
  const [accountHolder, setAccountHolder] = useState('')
  const [accountNumber, setAccountNumber] = useState('')
  const [payoutSubmitting, setPayoutSubmitting] = useState(false)

  const fetchPayout = async () => {
    if (!returnRequest) return
    try {
      const res = await getPayoutDestination(returnRequest.id)
      setPayoutInfo(res?.data || res)
    } catch {
      setPayoutInfo(null)
    }
  }

  useEffect(() => {
    fetchPayout()
  }, [returnRequest?.id])

  if (!returnRequest) return null

  const config = STATUS_CONFIG[returnRequest.status] || { label: returnRequest.status, variant: 'default', icon: Clock }
  const Icon = config.icon

  const handlePayoutSubmit = async (e) => {
    e.preventDefault()
    if (!accountHolder.trim() || !accountNumber.trim()) {
      alert('Vui lòng nhập tên chủ tài khoản và số tài khoản.')
      return
    }
    setPayoutSubmitting(true)
    try {
      await savePayoutDestination(returnRequest.id, {
        bankCode,
        accountHolder: accountHolder.trim().toUpperCase(),
        accountNumber: accountNumber.trim(),
      })
      fetchPayout()
    } catch (err) {
      alert(err?.response?.data?.message || 'Không thể lưu thông tin STK.')
    } finally {
      setPayoutSubmitting(false)
    }
  }

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

      {/* Thông tin STK nhận tiền hoàn */}
      <div className="bg-white p-3 rounded-xl border border-emerald-200 text-xs space-y-2">
        <div className="flex items-center justify-between font-semibold text-emerald-950">
          <span className="flex items-center gap-1"><CreditCard size={14} /> Tài khoản nhận tiền hoàn:</span>
        </div>

        {payoutInfo?.status === 'PROVIDED' ? (
          <div className="space-y-0.5 text-gray-700">
            <p><span className="text-gray-500">Ngân hàng:</span> <span className="font-semibold">{payoutInfo.bankCode}</span></p>
            <p><span className="text-gray-500">Chủ tài khoản:</span> <span className="font-semibold uppercase">{payoutInfo.accountHolder}</span></p>
            <p><span className="text-gray-500">Số tài khoản:</span> <span className="font-mono font-semibold">{payoutInfo.maskedAccountNumber}</span></p>
          </div>
        ) : (
          <form onSubmit={handlePayoutSubmit} className="space-y-2 pt-1 border-t border-gray-100">
            <p className="text-[11px] text-amber-700 font-medium">Bạn chưa nhập STK. Vui lòng nhập để shop chuyển khoản hoàn tiền:</p>
            <div className="grid grid-cols-2 gap-2">
              <select value={bankCode} onChange={(e) => setBankCode(e.target.value)} className="p-1.5 border rounded text-xs">
                <option value="VCB">Vietcombank</option>
                <option value="MB">MBBank</option>
                <option value="TCB">Techcombank</option>
                <option value="ACB">ACB</option>
                <option value="VPB">VPBank</option>
                <option value="BIDV">BIDV</option>
              </select>
              <input type="text" placeholder="CHỦ TÀI KHOẢN" value={accountHolder} onChange={(e) => setAccountHolder(e.target.value.toUpperCase())} className="p-1.5 border rounded text-xs uppercase" />
            </div>
            <div className="flex gap-2">
              <input type="text" placeholder="Số tài khoản" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)} className="flex-1 p-1.5 border rounded text-xs" />
              <button type="submit" disabled={payoutSubmitting} className="px-3 py-1.5 bg-emerald-600 text-white font-semibold rounded text-xs hover:bg-emerald-700">Lưu STK</button>
            </div>
          </form>
        )}

        {/* Khung hiển thị thông tin Bill chuyển tiền của Admin cho Khách hàng */}
        {(payoutInfo?.refundStatus === 'REFUNDED' || payoutInfo?.providerReference || payoutInfo?.proofUrl) && (
          <div className="mt-3 p-3 rounded-lg bg-emerald-50 border border-emerald-300 text-emerald-950 space-y-1.5">
            <div className="flex items-center gap-1.5 font-bold text-emerald-900 text-xs">
              <CheckCircle2 size={16} className="text-emerald-600" /> Shop đã chuyển tiền hoàn thành công!
            </div>
            {payoutInfo.providerReference && (
              <p><span className="text-gray-600">Mã giao dịch / Mã tham chiếu:</span> <span className="font-mono font-bold text-emerald-900">{payoutInfo.providerReference}</span></p>
            )}
            {payoutInfo.note && (
              <p><span className="text-gray-600">Ghi chú từ Shop:</span> {payoutInfo.note}</p>
            )}
            {payoutInfo.proofUrl && (
              <div className="pt-1">
                <p className="font-semibold text-gray-700 mb-1">Bill chuyển tiền đính kèm:</p>
                <a href={payoutInfo.proofUrl} target="_blank" rel="noopener noreferrer" className="inline-block">
                  <img src={payoutInfo.proofUrl} alt="Bill hoàn tiền" className="w-32 h-32 object-cover rounded-lg border border-emerald-300 shadow-sm hover:opacity-90" />
                </a>
              </div>
            )}
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
