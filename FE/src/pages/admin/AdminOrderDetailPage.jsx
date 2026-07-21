import { useCallback, useEffect, useMemo, useState } from 'react'
import { ArrowLeft, Check, Copy, CreditCard, ImageOff, MapPin, Package, RefreshCw, UserRound } from 'lucide-react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import Card from '../../components/common/Card'
import Badge from '../../components/common/Badge'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog'
import StatusBadge from '../../components/admin/StatusBadge'
import { getAdminOrder, updateAdminOrderStatus } from '../../features/orders/admin-order.api'
import { formatStatusLabel, normalizeOrderStatus } from '../../features/orders/orderStatus'
import { getAdminErrorMessage } from '../../features/admin/admin-error-messages'
import { formatCurrency } from '../../utils/formatCurrency'
import { formatDateTime } from '../../utils/formatDate'
import {
  displayValue,
  getOrderItemDisplay,
  getOrderItemLineTotal,
  getOrderSubtotal,
  resolveAdminOrderBackUrl,
} from './adminOrderDetail.utils'
import { getAdminOrderStatusOptions, getStatusUpdateErrorMessage } from './adminOrderStatus.utils'

function DetailSection({ title, icon: Icon, children, className = '' }) {
  return (
    <Card className={`p-5 ${className}`}>
      <div className="flex items-center gap-2 mb-4">
        {Icon && <Icon size={17} className="text-tertiary" aria-hidden="true" />}
        <h2 className="font-title-lg text-primary">{title}</h2>
      </div>
      {children}
    </Card>
  )
}

function InfoRow({ label, value, mono = false }) {
  return (
    <div className="flex items-start justify-between gap-4 py-2 border-b border-outline-variant/10 last:border-0">
      <dt className="text-sm text-on-surface-variant">{label}</dt>
      <dd className={`text-sm text-right text-on-surface break-words ${mono ? 'font-mono text-xs' : ''}`}>{displayValue(value)}</dd>
    </div>
  )
}

function ImageWithFallback({ src, alt }) {
  const [failed, setFailed] = useState(!src)
  if (failed) {
    return (
      <div className="w-20 h-20 rounded-lg bg-surface-container-high flex items-center justify-center text-on-surface-variant" aria-label={`${alt} - chưa có hình ảnh`}>
        <ImageOff size={20} aria-hidden="true" />
      </div>
    )
  }
  return <img src={src} alt={alt} className="w-20 h-20 rounded-lg object-cover bg-surface-container-high" onError={() => setFailed(true)} />
}

function OrderDetailSkeleton() {
  return (
    <div className="space-y-6 animate-pulse" aria-label="Đang tải chi tiết đơn hàng">
      <div className="h-24 rounded-xl bg-surface-container-high" />
      <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,1.85fr)_minmax(280px,1fr)] gap-6">
        <div className="space-y-6"><div className="h-64 rounded-xl bg-surface-container-high" /><div className="h-36 rounded-xl bg-surface-container-high" /></div>
        <div className="space-y-6"><div className="h-48 rounded-xl bg-surface-container-high" /><div className="h-56 rounded-xl bg-surface-container-high" /></div>
      </div>
    </div>
  )
}

function DetailError({ error, onRetry }) {
  const isNotFound = error?.status === 404
  const isForbidden = error?.status === 403
  const title = isNotFound ? 'Không tìm thấy đơn hàng' : isForbidden ? 'Không có quyền xem đơn hàng' : error?.title
  const message = isNotFound
    ? 'Mã đơn hàng không tồn tại hoặc đã bị xóa.'
    : isForbidden
      ? 'Tài khoản hiện tại không có quyền truy cập thông tin quản trị này.'
      : error?.message

  return (
    <div className="min-h-[50vh] flex items-center justify-center">
      <Card className="max-w-lg w-full p-8 text-center">
        <h1 className="font-title-lg text-primary">{title}</h1>
        <p className="mt-2 text-sm text-on-surface-variant">{message}</p>
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          {!isNotFound && !isForbidden && (
            <button type="button" onClick={onRetry} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-on-primary hover:opacity-90">
              <RefreshCw size={15} aria-hidden="true" /> Thử lại
            </button>
          )}
          <Link to="/admin/orders" className="inline-flex items-center gap-2 rounded-lg border border-outline-variant/30 px-4 py-2 text-sm font-medium text-primary hover:bg-surface-container-high">
            <ArrowLeft size={15} aria-hidden="true" /> Danh sách đơn hàng
          </Link>
        </div>
      </Card>
    </div>
  )
}

function OrderItemsSection({ items }) {
  if (!items?.length) {
    return <DetailSection title="Sản phẩm trong đơn" icon={Package}><p className="text-sm text-on-surface-variant">Đơn hàng chưa có sản phẩm.</p></DetailSection>
  }

  return (
    <DetailSection title="Sản phẩm trong đơn" icon={Package}>
      <div className="space-y-4">
        {items.map((item) => {
          const display = getOrderItemDisplay(item)
          return (
            <article key={item.id || item.variantId} className="flex flex-col sm:flex-row gap-4 border-b border-outline-variant/10 pb-4 last:border-0 last:pb-0">
              <ImageWithFallback src={display.imageUrl} alt={display.name} />
              <div className="min-w-0 flex-1">
                <h3 className="font-medium text-primary break-words">{display.name}</h3>
                <p className="mt-1 text-xs text-on-surface-variant">Mã sản phẩm: {display.productCode}</p>
                <p className="text-xs text-on-surface-variant">Mã biến thể: {display.variantCode}</p>
                <p className="text-xs text-on-surface-variant">SKU: {display.sku}</p>
                <dl className="mt-2 grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-1 text-xs text-on-surface-variant">
                  <div><dt className="inline">Màu sắc: </dt><dd className="inline text-on-surface">{display.color}</dd></div>
                  <div><dt className="inline">Kích thước: </dt><dd className="inline text-on-surface">{display.size}</dd></div>
                  <div><dt className="inline">Chất liệu: </dt><dd className="inline text-on-surface">{display.material}</dd></div>
                </dl>
              </div>
              <div className="sm:text-right shrink-0">
                <p className="text-sm text-on-surface">{item.quantity ?? 0} × {formatCurrency(item.priceAtPurchase)}</p>
                <p className="mt-1 font-semibold text-primary">{formatCurrency(getOrderItemLineTotal(item))}</p>
              </div>
            </article>
          )
        })}
      </div>
    </DetailSection>
  )
}

function OrderStatusHistory({ history }) {
  return (
    <DetailSection title="Lịch sử trạng thái">
      {!history?.length ? (
        <p className="text-sm text-on-surface-variant">Lịch sử trạng thái chưa được lưu hoặc chưa được cung cấp.</p>
      ) : (
        <ol className="space-y-4">
          {history.map((entry, index) => (
            <li key={entry.id || index} className="relative pl-5 border-l-2 border-tertiary-container">
              <p className="text-sm font-medium text-primary">
                {entry.previousStatus ? `${formatStatusLabel(entry.previousStatus)} → ` : ''}{formatStatusLabel(entry.newStatus)}
              </p>
              <p className="text-xs text-on-surface-variant">{formatDateTime(entry.timestamp)}</p>
              {entry.actor && <p className="mt-1 text-xs text-on-surface-variant">Nguồn: {displayValue(entry.actor)}</p>}
            </li>
          ))}
        </ol>
      )}
    </DetailSection>
  )
}

export default function AdminOrderDetailPage() {
  const { orderId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [copied, setCopied] = useState(false)
  const [selectedStatus, setSelectedStatus] = useState('')
  const [statusDialogOpen, setStatusDialogOpen] = useState(false)
  const [statusUpdating, setStatusUpdating] = useState(false)
  const [statusError, setStatusError] = useState(null)
  const [statusToast, setStatusToast] = useState('')
  const backUrl = useMemo(() => resolveAdminOrderBackUrl(location.search), [location.search])

  const fetchOrder = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setOrder(await getAdminOrder(orderId))
    } catch (err) {
      setOrder(null)
      if (err?.status === 404 || err?.status === 403) {
        setError(err)
      } else {
        setError(getAdminErrorMessage(err, {
          fallbackTitle: 'Không thể tải chi tiết đơn hàng',
          fallbackMessage: 'Hệ thống chưa thể tải thông tin đơn hàng. Vui lòng thử lại sau.',
        }))
      }
    } finally {
      setLoading(false)
    }
  }, [orderId])

  useEffect(() => { fetchOrder() }, [fetchOrder])

  const copyOrderId = async () => {
    try {
      await navigator.clipboard.writeText(order?.id || orderId)
      setCopied(true)
      setTimeout(() => setCopied(false), 1800)
    } catch {
      setCopied(false)
    }
  }

  if (loading) return <OrderDetailSkeleton />
  if (error) return <DetailError error={error} onRetry={fetchOrder} />
  if (!order) return <DetailError error={{ status: 404 }} onRetry={fetchOrder} />

  const orderStatus = normalizeOrderStatus(order.orderStatus)
  const statusOptions = getAdminOrderStatusOptions(order)
  const paymentStatus = order.paymentStatus ? normalizeOrderStatus(order.paymentStatus) : null
  const items = order.items || []
  const subtotal = getOrderSubtotal(items)
  const paymentMethodValue = order.paymentMethod?.toLowerCase()
  const paymentMethod = paymentMethodValue === 'cod'
    ? 'COD'
    : paymentMethodValue?.includes('sepay')
      ? 'SePay'
      : order.paymentMethod

  const handleStatusSelection = (event) => {
    const nextStatus = event.target.value
    setStatusError(null)
    setStatusToast('')
    if (!nextStatus) {
      setSelectedStatus('')
      return
    }
    setSelectedStatus(nextStatus)
    setStatusDialogOpen(true)
  }

  const closeStatusDialog = () => {
    if (statusUpdating) return
    setStatusDialogOpen(false)
    setSelectedStatus('')
  }

  const handleStatusConfirm = async () => {
    if (!selectedStatus || statusUpdating) return
    setStatusUpdating(true)
    setStatusError(null)
    setStatusToast('')
    try {
      await updateAdminOrderStatus(orderId, { orderStatus: selectedStatus })
      await fetchOrder()
      setStatusDialogOpen(false)
      setSelectedStatus('')
      setStatusToast('Đã cập nhật trạng thái đơn hàng.')
    } catch (err) {
      const friendly = getStatusUpdateErrorMessage(err)
      setStatusError(friendly)
      if (friendly.shouldRefetch) await fetchOrder()
    } finally {
      setStatusUpdating(false)
    }
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <button type="button" onClick={() => navigate(backUrl)} className="inline-flex items-center gap-2 text-sm text-on-surface-variant hover:text-primary focus:outline-none focus-visible:ring-2 focus-visible:ring-tertiary-container rounded">
            <ArrowLeft size={16} aria-hidden="true" /> Quay lại danh sách đơn hàng
          </button>
          <div className="mt-4 flex flex-wrap items-center gap-3">
            <h1 className="font-headline-md text-primary">Chi tiết đơn hàng</h1>
            <StatusBadge status={orderStatus.toLowerCase()} label={formatStatusLabel(orderStatus)} />
          </div>
          <div className="mt-2 flex flex-wrap items-center gap-2 text-sm text-on-surface-variant">
            <span className="font-mono">#{String(order.id).slice(0, 12)}</span>
            <button type="button" onClick={copyOrderId} aria-label="Sao chép mã đơn hàng đầy đủ" className="inline-flex items-center gap-1 rounded px-1.5 py-1 hover:bg-surface-container-high focus:outline-none focus-visible:ring-2 focus-visible:ring-tertiary-container">
              {copied ? <Check size={14} aria-hidden="true" /> : <Copy size={14} aria-hidden="true" />}
              {copied ? 'Đã sao chép' : 'Sao chép mã đầy đủ'}
            </button>
          </div>
          <p className="mt-1 text-xs text-on-surface-variant">Tạo lúc {formatDateTime(order.createdAt)}</p>
        </div>
        <div className="flex flex-col items-stretch gap-4 lg:items-end">
          <div className="w-full lg:w-64">
            {statusOptions.length > 0 ? (
              <>
                <label htmlFor="admin-order-status" className="mb-2 block text-xs font-medium text-on-surface-variant">
                  Cập nhật trạng thái
                </label>
                <select
                  id="admin-order-status"
                  value={selectedStatus}
                  onChange={handleStatusSelection}
                  disabled={statusUpdating}
                  className="w-full rounded-lg border border-outline-variant/30 bg-surface-container-lowest px-3 py-2 text-sm text-primary outline-none focus:border-tertiary-container disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <option value="">Chọn trạng thái mới</option>
                  {statusOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
              </>
            ) : null}
          </div>
        </div>
      </header>

      {statusToast && <p role="status" className="rounded-lg bg-success/10 px-4 py-3 text-sm text-success">{statusToast}</p>}
      {statusError && (
        <div role="alert" className="rounded-lg bg-error/10 px-4 py-3 text-sm text-error">
          <strong>{statusError.title}.</strong> {statusError.message}
        </div>
      )}

      <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,1.85fr)_minmax(280px,1fr)] gap-6 items-start">
        <div className="space-y-6 min-w-0">
          <OrderItemsSection items={items} />
          <DetailSection title="Tóm tắt giá">
            <dl>
              <InfoRow label="Tạm tính" value={formatCurrency(subtotal)} />
              <InfoRow label="Tổng cộng" value={formatCurrency(order.totalAmount)} />
            </dl>
          </DetailSection>
          <OrderStatusHistory history={order.statusHistory} />
        </div>

        <aside className="space-y-6 min-w-0">
          <DetailSection title="Khách hàng" icon={UserRound}>
            <dl>
              <InfoRow label="Email" value={order.customerEmail} />
              <InfoRow label="Mã người dùng" value={order.userId} mono />
              <InfoRow label="Họ tên" value={order.shippingRecipientName} />
              <InfoRow label="Số điện thoại" value={order.shippingPhone} />
            </dl>
          </DetailSection>

          <DetailSection title="Địa chỉ giao hàng" icon={MapPin}>
            <p className="text-sm leading-6 text-on-surface whitespace-pre-wrap">{displayValue(order.shippingAddress)}</p>
          </DetailSection>

          <DetailSection title="Thanh toán" icon={CreditCard}>
            <dl>
              <InfoRow label="Phương thức" value={paymentMethod} />
              <InfoRow label="Trạng thái" value={paymentStatus ? formatStatusLabel(paymentStatus) : null} />
              <InfoRow label="Số tiền" value={formatCurrency(order.totalAmount)} />
              <InfoRow label="Mã tham chiếu" value={order.paymentReference} mono />
              <InfoRow label="Nội dung chuyển khoản" value={order.transferContent} mono />
              <InfoRow label="Mã giao dịch cổng" value={order.gatewayTransactionId} mono />
              <InfoRow label="Thời gian thanh toán" value={order.paidAt ? formatDateTime(order.paidAt) : null} />
              <InfoRow label="Hết hạn lúc" value={order.paymentExpiresAt ? formatDateTime(order.paymentExpiresAt) : null} />
            </dl>
          </DetailSection>
        </aside>
      </div>

      <AdminConfirmDialog
        open={statusDialogOpen}
        title="Xác nhận cập nhật trạng thái"
        message={`Chuyển trạng thái đơn hàng từ “${formatStatusLabel(orderStatus)}” sang “${formatStatusLabel(selectedStatus)}”? Thay đổi này sẽ được ghi vào lịch sử đơn hàng.`}
        confirmLabel="Xác nhận cập nhật"
        cancelLabel="Hủy"
        loading={statusUpdating}
        onConfirm={handleStatusConfirm}
        onCancel={closeStatusDialog}
      />
    </div>
  )
}
