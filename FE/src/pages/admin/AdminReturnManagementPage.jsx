import React, { useEffect, useState } from 'react'
import { RotateCcw, CheckCircle2, XCircle, CreditCard, Eye, ExternalLink } from 'lucide-react'
import Badge from '../../components/common/Badge'
import Modal from '../../components/common/Modal'
import { adminGetReturns, adminReviewReturn, adminReceiveAndQc, getPayoutDestination, adminCompleteRefund, adminGetRefunds } from '../../features/orders/return.api'
import { formatDateTime } from '../../utils/formatDate'

const STATUS_OPTIONS = [
  { key: '', label: 'Tất cả trạng thái' },
  { key: 'REQUESTED', label: 'Chờ duyệt' },
  { key: 'APPROVED', label: 'Đã chấp nhận' },
  { key: 'RETURN_IN_TRANSIT', label: 'Đang vận chuyển' },
  { key: 'QC_PASSED', label: 'QC Đạt (Chờ chuyển tiền)' },
  { key: 'QC_FAILED', label: 'QC Thất bại' },
  { key: 'REJECTED', label: 'Bị từ chối' },
]

export default function AdminReturnManagementPage() {
  const [activeTab, setActiveTab] = useState('returns') // 'returns' | 'refunds'
  const [returns, setReturns] = useState([])
  const [refunds, setRefunds] = useState([])
  const [selectedStatus, setSelectedStatus] = useState('')
  const [loading, setLoading] = useState(true)
  const [refundsLoading, setRefundsLoading] = useState(false)
  const [error, setError] = useState('')

  const [selectedReturn, setSelectedReturn] = useState(null)
  const [payoutInfo, setPayoutInfo] = useState(null)
  const [payoutLoading, setPayoutLoading] = useState(false)

  const [reviewModalOpen, setReviewModalOpen] = useState(false)
  const [qcModalOpen, setQcModalOpen] = useState(false)
  const [detailModalOpen, setDetailModalOpen] = useState(false)

  const [adminNote, setAdminNote] = useState('')
  const [rejectionReason, setRejectionReason] = useState('')
  const [isPhysicalReturn, setIsPhysicalReturn] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)

  const [providerRef, setProviderRef] = useState('')
  const [proofUrl, setProofUrl] = useState('')
  const [refundNote, setRefundNote] = useState('')
  const [refundSubmitting, setRefundSubmitting] = useState(false)

  const fetchReturns = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await adminGetReturns(selectedStatus)
      const content = res?.data?.content || res?.content || []
      setReturns(content)
    } catch (err) {
      setError('Không thể tải danh sách yêu cầu trả hàng.')
    } finally {
      setLoading(false)
    }
  }

  const fetchRefunds = async () => {
    setRefundsLoading(true)
    try {
      const res = await adminGetRefunds()
      setRefunds(res?.data || res || [])
    } catch {
      setRefunds([])
    } finally {
      setRefundsLoading(false)
    }
  }

  useEffect(() => {
    fetchReturns()
    fetchRefunds()
  }, [selectedStatus, activeTab])

  const handleOpenDetail = async (ret) => {
    setSelectedReturn(ret)
    setPayoutInfo(null)
    setPayoutLoading(true)
    setDetailModalOpen(true)
    try {
      const res = await getPayoutDestination(ret.id)
      setPayoutInfo(res?.data || res)
    } catch {
      setPayoutInfo(null)
    } finally {
      setPayoutLoading(false)
    }
  }

  const handleReviewSubmit = async (action) => {
    if (!selectedReturn) return
    setActionLoading(true)
    try {
      await adminReviewReturn(selectedReturn.id, {
        action,
        isPhysicalReturn,
        adminNote: adminNote.trim(),
        rejectionReason: action === 'REJECT' ? rejectionReason.trim() : null,
      })
      setReviewModalOpen(false)
      fetchReturns()
      fetchRefunds()
    } catch (err) {
      alert(err?.response?.data?.message || 'Không thể duyệt yêu cầu trả hàng.')
    } finally {
      setActionLoading(false)
    }
  }

  const handleQcSubmit = async (qcPassed) => {
    if (!selectedReturn) return
    setActionLoading(true)
    try {
      await adminReceiveAndQc(selectedReturn.id, {
        qcPassed,
        adminNote: adminNote.trim(),
      })
      setQcModalOpen(false)
      fetchReturns()
      fetchRefunds()
    } catch (err) {
      alert(err?.response?.data?.message || 'Có lỗi khi kiểm định QC trả hàng.')
    } finally {
      setActionLoading(false)
    }
  }

  const handleCompleteRefundSubmit = async (e) => {
    e.preventDefault()
    const targetRefundId = selectedReturn?.refundId || payoutInfo?.refundId || selectedReturn?.id || selectedReturn?.orderId
    if (!targetRefundId) {
      alert('Không tìm thấy thông tin để thực hiện hoàn tiền.')
      return
    }
    if (!providerRef.trim()) {
      alert('Vui lòng nhập Mã giao dịch / Tham chiếu ngân hàng.')
      return
    }
    setRefundSubmitting(true)
    try {
      await adminCompleteRefund(targetRefundId, {
        providerReference: providerRef.trim(),
        proofUrl: proofUrl.trim(),
        note: refundNote.trim(),
        processedBy: 'Admin',
      })
      alert('Đã xác nhận hoàn tiền & chuyển Bill cho khách thành công!')
      setDetailModalOpen(false)
      fetchReturns()
      fetchRefunds()
    } catch (err) {
      alert(err?.response?.data?.message || 'Không thể xác nhận hoàn tiền.')
    } finally {
      setRefundSubmitting(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <RotateCcw className="text-emerald-600" /> Quản lý Trả hàng & Hoàn tiền
          </h1>
          <p className="text-xs text-gray-500 mt-1">Duyệt yêu cầu trả hàng, kiểm định QC, xem STK & quản lý nhật ký hoàn tiền</p>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex bg-gray-100 p-1 rounded-lg border border-gray-200">
            <button
              onClick={() => setActiveTab('returns')}
              className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                activeTab === 'returns' ? 'bg-white text-emerald-700 shadow-sm' : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              📋 Danh sách Yêu cầu Trả hàng
            </button>
            <button
              onClick={() => setActiveTab('refunds')}
              className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors ${
                activeTab === 'refunds' ? 'bg-white text-emerald-700 shadow-sm' : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              💳 Nhật ký Hoàn tiền (Refund Records)
            </button>
          </div>

          {activeTab === 'returns' && (
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg text-xs font-semibold text-gray-700 bg-white"
            >
              {STATUS_OPTIONS.map((opt) => (
                <option key={opt.key} value={opt.key}>
                  {opt.label}
                </option>
              ))}
            </select>
          )}
        </div>
      </div>

      {activeTab === 'returns' && (
        <>
          {loading && <div className="py-12 text-center text-sm text-gray-500">Đang tải yêu cầu trả hàng...</div>}
          {error && <div className="p-4 bg-red-50 text-red-600 text-sm rounded-lg border border-red-200">{error}</div>}

          {!loading && !error && returns.length === 0 && (
            <div className="py-16 text-center text-gray-400 bg-white rounded-xl border border-gray-100">
              <RotateCcw size={40} className="mx-auto mb-2 opacity-30" />
              <p className="text-sm font-medium">Không có yêu cầu trả hàng nào.</p>
            </div>
          )}

          {!loading && !error && returns.length > 0 && (
            <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
              <table className="w-full text-left text-xs">
                <thead className="bg-gray-50 border-b border-gray-200 text-gray-600 uppercase font-semibold">
                  <tr>
                    <th className="p-3">Mã Yêu Cầu</th>
                    <th className="p-3">Đơn Hàng</th>
                    <th className="p-3">Khách Hàng ID</th>
                    <th className="p-3">Lý Do Trả</th>
                    <th className="p-3">Ngày Gửi</th>
                    <th className="p-3">Trạng Thái</th>
                    <th className="p-3 text-right">Thao Tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 font-medium text-gray-800">
                  {returns.map((ret) => {
                    const matchingRefund = refunds.find(rf => (rf.returnRequestId === ret.id || rf.orderId === ret.orderId || rf.id === ret.refundId))
                    const isRefunded = matchingRefund ? matchingRefund.status === 'REFUNDED' : false
                    return (
                      <tr key={ret.id} className="hover:bg-gray-50/50">
                        <td className="p-3 font-semibold text-emerald-700">{ret.id}</td>
                        <td className="p-3">{ret.orderId}</td>
                        <td className="p-3 text-gray-500">{ret.userId}</td>
                        <td className="p-3">{ret.reason}</td>
                        <td className="p-3 text-gray-500">{formatDateTime(ret.requestedAt)}</td>
                        <td className="p-3">
                          {ret.status === 'QC_PASSED' ? (
                            <Badge variant={isRefunded ? 'success' : 'warning'}>
                              {isRefunded ? 'QC Đạt (Đã hoàn tiền)' : 'QC Đạt (Chờ chuyển tiền)'}
                            </Badge>
                          ) : (
                            <Badge variant={ret.status === 'APPROVED' ? 'success' : ret.status === 'REJECTED' ? 'error' : 'warning'}>
                              {ret.status}
                            </Badge>
                          )}
                        </td>
                        <td className="p-3 text-right space-x-1.5">
                          <button
                            onClick={() => handleOpenDetail(ret)}
                            className="px-2.5 py-1 bg-gray-100 text-gray-700 hover:bg-gray-200 rounded font-medium inline-flex items-center gap-1"
                          >
                            <Eye size={12} /> Chi tiết & STK
                          </button>

                          {ret.status === 'REQUESTED' && (
                            <button
                              onClick={() => {
                                setSelectedReturn(ret)
                                setAdminNote('')
                                setRejectionReason('')
                                setReviewModalOpen(true)
                              }}
                              className="px-2.5 py-1 bg-emerald-600 text-white rounded font-medium hover:bg-emerald-700"
                            >
                              Duyệt / Từ chối
                            </button>
                          )}

                          {['APPROVED', 'RETURN_IN_TRANSIT'].includes(ret.status) && (
                            <button
                              onClick={() => {
                                setSelectedReturn(ret)
                                setAdminNote('')
                                setQcModalOpen(true)
                              }}
                              className="px-2.5 py-1 bg-blue-600 text-white rounded font-medium hover:bg-blue-700"
                            >
                              Nhận hàng & QC
                            </button>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {/* Tab Nhật ký Hoàn tiền (Refund Records) */}
      {activeTab === 'refunds' && (
        <>
          {refundsLoading && <div className="py-12 text-center text-sm text-gray-500">Đang tải nhật ký hoàn tiền...</div>}
          {!refundsLoading && refunds.length === 0 && (
            <div className="py-16 text-center text-gray-400 bg-white rounded-xl border border-gray-100">
              <CreditCard size={40} className="mx-auto mb-2 opacity-30" />
              <p className="text-sm font-medium">Chưa có giao dịch hoàn tiền nào.</p>
            </div>
          )}

          {!refundsLoading && refunds.length > 0 && (
            <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
              <table className="w-full text-left text-xs">
                <thead className="bg-gray-50 border-b border-gray-200 text-gray-600 uppercase font-semibold">
                  <tr>
                    <th className="p-3">Refund ID</th>
                    <th className="p-3">Đơn Hàng ID</th>
                    <th className="p-3">Mã Trả Hàng</th>
                    <th className="p-3">Số Tiền Hoàn</th>
                    <th className="p-3">Ngân Hàng & STK</th>
                    <th className="p-3">Mã Giao Dịch</th>
                    <th className="p-3">Bill Chuyển Tiền</th>
                    <th className="p-3">Trạng Thái</th>
                    <th className="p-3">Thời Gian</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 font-medium text-gray-800">
                  {refunds.map((rf) => {
                    const matchingReturn = returns.find(r => r.id === rf.returnRequestId || r.refundId === rf.id)
                    const displayOrderId = (matchingReturn?.orderId && (rf.orderId === 'order_001' || !rf.orderId)) ? matchingReturn.orderId : rf.orderId
                    return (
                      <tr key={rf.id} className="hover:bg-gray-50/50">
                        <td className="p-3 font-mono font-semibold text-emerald-700">{rf.id}</td>
                        <td className="p-3 font-mono font-semibold text-gray-900">{displayOrderId}</td>
                        <td className="p-3 font-semibold text-emerald-700">{rf.returnRequestId || 'N/A'}</td>
                        <td className="p-3 font-bold text-gray-900">{rf.amount ? Number(rf.amount).toLocaleString('vi-VN') + ' đ' : '0 đ'}</td>
                      <td className="p-3">
                        {rf.bankCode ? (
                          <span><span className="font-bold">{rf.bankCode}</span> - {rf.accountHolder} ({rf.accountNumber})</span>
                        ) : (
                          <span className="text-gray-400 italic">Chưa nhập STK</span>
                        )}
                      </td>
                      <td className="p-3 font-mono text-emerald-900 font-semibold">{rf.providerReference || '---'}</td>
                      <td className="p-3">
                        {rf.proofUrl ? (
                          <a href={rf.proofUrl} target="_blank" rel="noopener noreferrer" className="text-emerald-600 hover:underline flex items-center gap-1 font-semibold">
                            <ExternalLink size={12} /> Xem Bill
                          </a>
                        ) : (
                          <span className="text-gray-400">Không có</span>
                        )}
                      </td>
                      <td className="p-3">
                        <Badge variant={rf.status === 'REFUNDED' ? 'success' : rf.status === 'REFUND_FAILED' ? 'error' : 'warning'}>
                          {rf.status === 'REFUNDED' ? 'Đã Chuyển Tiền' : rf.status === 'REFUND_FAILED' ? 'Thất Bại' : 'Đang Chờ'}
                        </Badge>
                      </td>
                      <td className="p-3 text-gray-500">{rf.processedAt ? formatDateTime(rf.processedAt) : formatDateTime(rf.createdAt)}</td>
                    </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {/* Detail & Payout Info Modal */}
      {selectedReturn && (
        <Modal isOpen={detailModalOpen} onClose={() => setDetailModalOpen(false)} title={`Chi Tiết & Thông Tin Chuyển Tiền #${selectedReturn.id}`}>
          <div className="space-y-4 text-xs">
            <div className="bg-gray-50 p-3 rounded-lg border border-gray-200 space-y-1">
              <p><span className="font-semibold">Mã đơn hàng:</span> {selectedReturn.orderId}</p>
              <p><span className="font-semibold">Khách hàng ID:</span> {selectedReturn.userId}</p>
              <p><span className="font-semibold">Lý do trả:</span> <span className="text-emerald-700 font-semibold">{selectedReturn.reason}</span></p>
              <p><span className="font-semibold">Ghi chú từ khách:</span> {selectedReturn.customerNote || 'Không có'}</p>
              <p><span className="font-semibold">Thời gian gửi:</span> {formatDateTime(selectedReturn.requestedAt)}</p>
            </div>

            {/* Thông tin tài khoản ngân hàng của khách */}
            <div className="bg-emerald-50 border border-emerald-200 p-3.5 rounded-lg text-emerald-950 space-y-2">
              <div className="flex items-center gap-1.5 font-bold text-emerald-900 border-b border-emerald-200 pb-1.5">
                <CreditCard size={16} /> Thông Tin Ngân Hàng Nhận Tiền Hoàn (Payout Destination)
              </div>
              {payoutLoading && <p className="text-gray-500 italic">Đang tải thông tin STK...</p>}
              {!payoutLoading && payoutInfo?.status === 'PROVIDED' && (
                <div className="space-y-1 font-medium">
                  <p><span className="text-gray-600">Ngân hàng:</span> <span className="font-bold text-emerald-900">{payoutInfo.bankCode}</span></p>
                  <p><span className="text-gray-600">Chủ tài khoản:</span> <span className="font-bold text-emerald-900">{payoutInfo.accountHolder}</span></p>
                  <p><span className="text-gray-600">Số tài khoản:</span> <span className="font-mono font-bold text-emerald-900">{payoutInfo.maskedAccountNumber}</span></p>
                </div>
              )}
              {!payoutLoading && payoutInfo?.status !== 'PROVIDED' && (
                <p className="text-gray-500 italic">Khách hàng chưa cập nhật thông tin STK ngân hàng.</p>
              )}
            </div>

            {/* Form xác nhận chuyển khoản & đính kèm Bill */}
            {(selectedReturn.refundId || payoutInfo?.refundId || selectedReturn.status === 'QC_PASSED') && (
              <form onSubmit={handleCompleteRefundSubmit} className="bg-emerald-500/10 p-3.5 rounded-lg border border-emerald-300 space-y-2">
                <p className="font-bold text-emerald-900 flex items-center gap-1.5">
                  <CheckCircle2 size={16} /> Xác nhận đã chuyển khoản cho khách (Đính kèm Bill chuyển tiền):
                </p>
                <div className="grid grid-cols-2 gap-2">
                  <input
                    type="text"
                    placeholder="Mã giao dịch / Mã tham chiếu ngân hàng (bắt buộc)"
                    value={providerRef}
                    onChange={(e) => setProviderRef(e.target.value)}
                    className="p-2 border border-gray-300 rounded text-xs bg-white"
                    required
                  />
                  <input
                    type="text"
                    placeholder="URL ảnh Bill chuyển khoản (Cloudinary URL)"
                    value={proofUrl}
                    onChange={(e) => setProofUrl(e.target.value)}
                    className="p-2 border border-gray-300 rounded text-xs bg-white"
                  />
                </div>
                <input
                  type="text"
                  placeholder="Ghi chú hoàn tiền (Tùy chọn)"
                  value={refundNote}
                  onChange={(e) => setRefundNote(e.target.value)}
                  className="w-full p-2 border border-gray-300 rounded text-xs bg-white"
                />
                <button
                  type="submit"
                  disabled={refundSubmitting}
                  className="w-full py-2 bg-emerald-600 text-white font-bold rounded hover:bg-emerald-700 transition-colors"
                >
                  {refundSubmitting ? 'Đang xử lý...' : 'Xác nhận Đã Chuyển Tiền & Gửi Bill Cho Khách'}
                </button>
              </form>
            )}

            {selectedReturn.shipment && (
              <div className="bg-blue-50 border border-blue-200 p-3 rounded-lg text-blue-900 space-y-1">
                <p className="font-semibold">Thông tin bưu gửi của khách:</p>
                <p>Đơn vị: {selectedReturn.shipment.carrier}</p>
                <p>Mã vận đơn: <span className="font-mono font-semibold">{selectedReturn.shipment.trackingCode}</span></p>
              </div>
            )}

            {selectedReturn.evidences?.length > 0 && (
              <div>
                <p className="font-semibold mb-1">Ảnh bằng chứng:</p>
                <div className="flex flex-wrap gap-2">
                  {selectedReturn.evidences.map((ev) => (
                    <img key={ev.id} src={ev.secureUrl} alt="evidence" className="w-16 h-16 object-cover rounded border" />
                  ))}
                </div>
              </div>
            )}
          </div>
        </Modal>
      )}

      {/* Review Modal */}
      {selectedReturn && (
        <Modal isOpen={reviewModalOpen} onClose={() => setReviewModalOpen(false)} title={`Duyệt Yêu Cầu Trả Hàng #${selectedReturn.id}`}>
          <div className="space-y-4 text-xs">
            <div className="bg-gray-50 p-3 rounded-lg border border-gray-200">
              <p className="font-semibold text-gray-800">Lý do trả: <span className="text-emerald-700">{selectedReturn.reason}</span></p>
              <p className="text-gray-600 mt-1">Ghi chú khách: {selectedReturn.customerNote || 'Không có'}</p>
            </div>

            {selectedReturn.evidences?.length > 0 && (
              <div>
                <p className="font-semibold mb-1">Ảnh bằng chứng:</p>
                <div className="flex flex-wrap gap-2">
                  {selectedReturn.evidences.map((ev) => (
                    <img key={ev.id} src={ev.secureUrl} alt="evidence" className="w-16 h-16 object-cover rounded border" />
                  ))}
                </div>
              </div>
            )}

            <div>
              <label className="block font-semibold mb-1">Hình thức xử lý</label>
              <div className="flex gap-4">
                <label className="flex items-center gap-1.5 cursor-pointer">
                  <input type="radio" checked={isPhysicalReturn} onChange={() => setIsPhysicalReturn(true)} />
                  <span>Yêu cầu gửi hàng về kho</span>
                </label>
                <label className="flex items-center gap-1.5 cursor-pointer">
                  <input type="radio" checked={!isPhysicalReturn} onChange={() => setIsPhysicalReturn(false)} />
                  <span>Hoàn tiền không cần trả hàng</span>
                </label>
              </div>
            </div>

            <div>
              <label className="block font-semibold mb-1">Ghi chú Admin</label>
              <textarea
                rows="2"
                value={adminNote}
                onChange={(e) => setAdminNote(e.target.value)}
                className="w-full p-2 border rounded"
                placeholder="Ghi chú nội bộ..."
              />
            </div>

            <div>
              <label className="block font-semibold mb-1 text-red-600">Lý do từ chối (Nếu từ chối)</label>
              <input
                type="text"
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                className="w-full p-2 border rounded"
                placeholder="Nhập lý do từ chối..."
              />
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t">
              <button
                disabled={actionLoading}
                onClick={() => handleReviewSubmit('REJECT')}
                className="px-3 py-1.5 bg-red-600 text-white rounded font-semibold hover:bg-red-700"
              >
                Từ Chối Yêu Cầu
              </button>
              <button
                disabled={actionLoading}
                onClick={() => handleReviewSubmit('APPROVE')}
                className="px-3 py-1.5 bg-emerald-600 text-white rounded font-semibold hover:bg-emerald-700"
              >
                Đồng Ý Duyệt Trả
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* QC Modal */}
      {selectedReturn && (
        <Modal isOpen={qcModalOpen} onClose={() => setQcModalOpen(false)} title={`Kiểm Định QC Bưu Gửi #${selectedReturn.id}`}>
          <div className="space-y-4 text-xs">
            <p className="text-gray-600">Xác nhận nhận bưu gửi từ khách hàng và đánh giá chất lượng sản phẩm.</p>

            <div>
              <label className="block font-semibold mb-1">Ghi chú kiểm định QC</label>
              <textarea
                rows="2"
                value={adminNote}
                onChange={(e) => setAdminNote(e.target.value)}
                className="w-full p-2 border rounded"
                placeholder="Mô tả kết quả kiểm định sản phẩm..."
              />
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t">
              <button
                disabled={actionLoading}
                onClick={() => handleQcSubmit(false)}
                className="px-3 py-1.5 bg-red-600 text-white rounded font-semibold hover:bg-red-700"
              >
                QC Thất Bại (Trả về)
              </button>
              <button
                disabled={actionLoading}
                onClick={() => handleQcSubmit(true)}
                className="px-3 py-1.5 bg-emerald-600 text-white rounded font-semibold hover:bg-emerald-700"
              >
                QC Đạt (Cộng Kho & Tạo Refund)
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  )
}
