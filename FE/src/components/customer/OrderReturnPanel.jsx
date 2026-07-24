import { useEffect, useState } from 'react'
import Badge from '../common/Badge'
import { formatDateTime } from '../../utils/formatDate'
import {
  formatReturnReason,
  formatReturnStatus,
  getManualBankInfoValidationMessage,
  getReturnStatusVariant,
  groupReturnAttachments,
  needsReturnBankInfo,
  toManualBankInfoPayload,
} from '../../features/orders/order-return.utils'

function DetailRow({ label, value }) {
  if (value === null || value === undefined || value === '') return null
  return (
    <div className="flex items-start justify-between gap-4 py-1.5 text-sm">
      <span className="text-on-surface-variant">{label}</span>
      <span className="max-w-[60%] break-words text-right text-primary">{value}</span>
    </div>
  )
}

function AttachmentGroup({ title, attachments }) {
  if (!attachments?.length) return null
  return (
    <div className="space-y-2">
      <p className="text-xs font-medium uppercase tracking-[0.14em] text-on-surface-variant">{title}</p>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {attachments.map((attachment) => (
          <a key={attachment.id} href={attachment.imageDataUrl} target="_blank" rel="noreferrer" className="group overflow-hidden rounded-xl border border-outline-variant/20 bg-surface-container-low">
            <img src={attachment.imageDataUrl} alt={attachment.fileName || title} className="aspect-square w-full object-cover transition-transform group-hover:scale-105" />
          </a>
        ))}
      </div>
    </div>
  )
}

function BankInfoForm({ loading, onSubmit }) {
  const [form, setForm] = useState({ bankName: '', bankAccountNumber: '', bankAccountHolder: '' })
  const [confirmed, setConfirmed] = useState(false)
  const [pendingPayload, setPendingPayload] = useState(null)
  const [error, setError] = useState('')

  const update = (key, value) => {
    setForm((current) => ({ ...current, [key]: value }))
    setPendingPayload(null)
    if (error) setError('')
  }

  const submit = () => {
    const validationMessage = getManualBankInfoValidationMessage(form)
    if (validationMessage) {
      setError(validationMessage)
      return
    }
    if (!confirmed) {
      setError('Vui lòng xác nhận thông tin ngân hàng đã chính xác.')
      return
    }
    setPendingPayload(toManualBankInfoPayload(form))
  }

  const confirmSubmit = async () => {
    if (!pendingPayload) return
    await onSubmit(pendingPayload)
    setPendingPayload(null)
  }

  return (
    <div className="rounded-xl border border-tertiary-container/30 bg-tertiary-container/10 p-4">
      <h4 className="text-sm font-semibold text-primary">Thông tin nhận hoàn tiền</h4>
      <div className="mt-3 grid gap-3">
        <input value={form.bankName} onChange={(event) => update('bankName', event.target.value)} disabled={loading} placeholder="Tên ngân hàng" className="rounded-lg border border-outline-variant/30 bg-surface-container-lowest px-3 py-2 text-sm outline-none focus:border-primary disabled:opacity-60" />
        <input value={form.bankAccountNumber} onChange={(event) => update('bankAccountNumber', event.target.value)} disabled={loading} placeholder="Số tài khoản" className="rounded-lg border border-outline-variant/30 bg-surface-container-lowest px-3 py-2 text-sm outline-none focus:border-primary disabled:opacity-60" />
        <input value={form.bankAccountHolder} onChange={(event) => update('bankAccountHolder', event.target.value)} disabled={loading} placeholder="Tên chủ tài khoản" className="rounded-lg border border-outline-variant/30 bg-surface-container-lowest px-3 py-2 text-sm outline-none focus:border-primary disabled:opacity-60" />
      </div>
      <label className="mt-3 flex items-start gap-2 text-xs leading-relaxed text-on-surface-variant">
        <input type="checkbox" checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} disabled={loading} className="mt-0.5" />
        Tôi xác nhận thông tin ngân hàng là chính xác. Nếu sai số tài khoản, tên chủ tài khoản hoặc ngân hàng, cửa hàng không chịu trách nhiệm cho việc hoàn tiền sai thông tin.
      </label>
      {error && <p role="alert" className="mt-2 text-sm text-error">{error}</p>}
      <button type="button" onClick={submit} disabled={loading} className="mt-3 inline-flex w-full justify-center rounded-full bg-primary px-4 py-2.5 text-sm font-medium text-on-primary disabled:opacity-50">
        {loading ? 'Đang gửi...' : 'Kiểm tra lại thông tin'}
      </button>
      {pendingPayload && (
        <div role="alertdialog" aria-labelledby="bank-confirm-title" className="mt-4 rounded-xl border border-primary/20 bg-surface-container-lowest p-4">
          <h5 id="bank-confirm-title" className="text-sm font-semibold text-primary">Xác nhận lần cuối</h5>
          <p className="mt-2 text-xs leading-relaxed text-on-surface-variant">
            Vui lòng kiểm tra thật kỹ thông tin bên dưới. Nếu nhập sai ngân hàng, số tài khoản hoặc tên chủ tài khoản, cửa hàng không chịu trách nhiệm cho việc hoàn tiền sai thông tin.
          </p>
          <div className="mt-3 rounded-lg bg-surface-container-low p-3">
            <DetailRow label="Ngân hàng" value={pendingPayload.bankName} />
            <DetailRow label="Số tài khoản" value={pendingPayload.bankAccountNumber} />
            <DetailRow label="Chủ tài khoản" value={pendingPayload.bankAccountHolder} />
          </div>
          <div className="mt-3 flex flex-wrap justify-end gap-2">
            <button type="button" onClick={() => setPendingPayload(null)} disabled={loading} className="rounded-full border border-outline-variant/30 px-4 py-2 text-sm font-medium text-on-surface-variant disabled:opacity-50">
              Chỉnh sửa
            </button>
            <button type="button" onClick={confirmSubmit} disabled={loading} className="rounded-full bg-primary px-4 py-2 text-sm font-medium text-on-primary disabled:opacity-50">
              {loading ? 'Đang gửi...' : 'Tôi chắc chắn, gửi thông tin'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

export default function OrderReturnPanel({ returnRequest, bankInfoLoading = false, onSubmitBankInfo }) {
  const [success, setSuccess] = useState('')

  useEffect(() => setSuccess(''), [returnRequest?.id, returnRequest?.status])

  if (!returnRequest) return null

  const attachments = groupReturnAttachments(returnRequest)
  const showBankForm = needsReturnBankInfo(returnRequest) && onSubmitBankInfo

  const submitBankInfo = async (payload) => {
    await onSubmitBankInfo(payload)
    setSuccess('Đã gửi thông tin ngân hàng cho admin xử lý hoàn tiền.')
  }

  return (
    <section className="space-y-4 rounded-xl border border-outline-variant/20 bg-surface-container-low p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-primary">Yêu cầu hoàn hàng</p>
          <p className="mt-1 text-xs text-on-surface-variant">{formatDateTime(returnRequest.requestedAt || returnRequest.createdAt)}</p>
        </div>
        <Badge variant={getReturnStatusVariant(returnRequest.status)}>{formatReturnStatus(returnRequest.status)}</Badge>
      </div>

      <div className="rounded-xl bg-surface-container-lowest p-3">
        <DetailRow label="Lý do" value={formatReturnReason(returnRequest.reasonCode)} />
        <DetailRow label="Ghi chú khách" value={returnRequest.customerNote} />
        <DetailRow label="Lý do từ chối" value={returnRequest.rejectionReason} />
        <DetailRow label="Ngân hàng" value={returnRequest.bankName} />
        <DetailRow label="Số tài khoản" value={returnRequest.bankAccountNumber} />
        <DetailRow label="Chủ tài khoản" value={returnRequest.bankAccountHolder} />
        <DetailRow label="Mã bill hoàn tiền" value={returnRequest.refundReference} />
        <DetailRow label="Ghi chú hoàn tiền" value={returnRequest.refundNote} />
        <DetailRow label="Hoàn tiền lúc" value={formatDateTime(returnRequest.processedAt)} />
      </div>

      <AttachmentGroup title="Ảnh khách hàng gửi" attachments={attachments.customerProofs} />
      <AttachmentGroup title="Ảnh admin phản hồi" attachments={attachments.adminRejections} />
      <AttachmentGroup title="Bill hoàn tiền" attachments={attachments.adminBills} />

      {showBankForm && <BankInfoForm loading={bankInfoLoading} onSubmit={submitBankInfo} />}
      {success && <p role="status" className="rounded-lg bg-success/10 px-3 py-2 text-sm text-success">{success}</p>}
    </section>
  )
}
