import React, { useEffect, useState } from 'react'
import { CreditCard, CheckCircle2 } from 'lucide-react'
import Badge from '../common/Badge'
import { formatCancellationReason, formatCancellationStatus, formatCancellationType, formatRefundStatus } from '../../features/orders/order-cancellation.utils'
import { formatCurrency } from '../../utils/formatCurrency'
import { formatDateTime } from '../../utils/formatDate'
import { getPayoutDestination, savePayoutDestination } from '../../features/orders/return.api'

function DetailRow({ label, value }) {
  if (value === null || value === undefined || value === '') return null
  return (
    <div className="flex items-start justify-between gap-4 border-t border-outline-variant/10 pt-2 first:border-0 first:pt-0">
      <dt className="text-xs text-on-surface-variant">{label}</dt>
      <dd className="max-w-[65%] text-right text-xs leading-5 text-on-surface break-words">{value}</dd>
    </div>
  )
}

export default function OrderCancellationPanel({ cancellation, refund, orderId }) {
  if (!cancellation && !refund) return null

  const targetId = cancellation?.orderId || refund?.orderId || orderId
  const [payoutInfo, setPayoutInfo] = useState(null)
  const [bankCode, setBankCode] = useState('VCB')
  const [accountHolder, setAccountHolder] = useState('')
  const [accountNumber, setAccountNumber] = useState('')
  const [payoutSubmitting, setPayoutSubmitting] = useState(false)

  const fetchPayout = async () => {
    if (!targetId) return
    try {
      const res = await getPayoutDestination(targetId)
      setPayoutInfo(res?.data || res)
    } catch {
      setPayoutInfo(null)
    }
  }

  useEffect(() => {
    fetchPayout()
  }, [targetId])

  const handlePayoutSubmit = async (e) => {
    e.preventDefault()
    if (!accountHolder.trim() || !accountNumber.trim()) {
      alert('Vui lòng nhập tên chủ tài khoản và số tài khoản.')
      return
    }
    setPayoutSubmitting(true)
    try {
      await savePayoutDestination(targetId, {
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

  return (
    <section className="space-y-4 rounded-2xl border border-outline-variant/20 bg-surface-container-lowest p-4">
      {cancellation && (
        <div className="space-y-3">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm font-medium text-primary">Yêu cầu hủy đơn hàng</p>
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

      {/* Thông tin STK nhận tiền hoàn cho Đơn Hủy */}
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
            <p className="text-[11px] text-amber-700 font-medium">Nếu bạn đã thanh toán, vui lòng nhập STK để shop chuyển khoản hoàn tiền:</p>
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
        {(payoutInfo?.refundStatus === 'REFUNDED' || refund?.status === 'REFUNDED' || payoutInfo?.proofUrl || refund?.proofUrl) && (
          <div className="mt-3 p-3 rounded-lg bg-emerald-50 border border-emerald-300 text-emerald-950 space-y-1.5">
            <div className="flex items-center gap-1.5 font-bold text-emerald-900 text-xs">
              <CheckCircle2 size={16} className="text-emerald-600" /> Shop đã chuyển tiền hoàn thành công!
            </div>
            {(payoutInfo?.providerReference || refund?.providerReference) && (
              <p><span className="text-gray-600">Mã giao dịch hoàn:</span> <span className="font-mono font-bold text-emerald-900">{payoutInfo?.providerReference || refund?.providerReference}</span></p>
            )}
            {(payoutInfo?.note || refund?.note) && (
              <p><span className="text-gray-600">Ghi chú từ Shop:</span> {payoutInfo?.note || refund?.note}</p>
            )}
            {(payoutInfo?.proofUrl || refund?.proofUrl) && (
              <div className="pt-1">
                <p className="font-semibold text-gray-700 mb-1">Bill chuyển tiền đính kèm:</p>
                <a href={payoutInfo?.proofUrl || refund?.proofUrl} target="_blank" rel="noopener noreferrer" className="inline-block">
                  <img src={payoutInfo?.proofUrl || refund?.proofUrl} alt="Bill hoàn tiền" className="w-32 h-32 object-cover rounded-lg border border-emerald-300 shadow-sm hover:opacity-90" />
                </a>
              </div>
            )}
          </div>
        )}
      </div>

      {refund && (
        <div className="border-t border-outline-variant/20 pt-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm font-medium text-primary">Trạng thái Hoàn tiền</p>
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
