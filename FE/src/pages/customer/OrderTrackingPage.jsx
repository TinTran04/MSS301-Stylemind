import { useCallback, useEffect, useRef, useState } from 'react'
import { ArrowRight, Image as ImageIcon, Loader2, Package, Truck, Upload } from 'lucide-react'
import { useLocation, useNavigate } from 'react-router-dom'
import Badge from '../../components/common/Badge'
import Drawer from '../../components/common/Drawer'
import Pagination from '../../components/common/Pagination'
import { getOrderById, getOrders, uploadDeliveryImage } from '../../features/orders/order.api'
import { formatDateTime } from '../../utils/formatDate'
import { formatCurrency } from '../../utils/formatCurrency'
import { normalizeOrderStatus } from '../../features/orders/orderStatus'
import { isCodPaymentMethod, TAX_LABEL } from '../../features/cart/cart.utils'
import OrderCancellationDialog from '../../components/customer/OrderCancellationDialog'
import OrderCancellationPanel from '../../components/customer/OrderCancellationPanel'
import OrderReturnDialog from '../../components/customer/OrderReturnDialog'
import OrderReturnPanel from '../../components/customer/OrderReturnPanel'
import { requestOrderCancellation } from '../../features/orders/order.api'
import { canDirectCancel, canRequestCancellation, isCancellationRequested } from '../../features/orders/order-cancellation.utils'
import { createOrderReturnRequest, submitReturnBankInfo } from '../../features/orders/order-return.api'
import { canRequestCodReturn, getOrderStatusDisplay } from '../../features/orders/order-return.utils'
import { mergeOrderSummaryUpdate } from '../../features/orders/order.mapper'

const statusTabs = [
  { key: 'All', label: 'Tất cả' },
  { key: 'Processing', label: 'Đang xử lý' },
  { key: 'Shipped', label: 'Đang giao' },
  { key: 'Completed', label: 'Đã giao thành công' },
]

const ORDER_IMAGE_FALLBACK = 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=200&h=260&fit=crop'
const DELIVERY_IMAGE_ACCEPT = 'image/jpeg,image/png,image/webp'
const DELIVERY_IMAGE_ALLOWED_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])
const DELIVERY_IMAGE_MAX_BYTES = 3 * 1024 * 1024
const DELIVERY_IMAGE_LIMIT = 5

function handleImageError(event) {
  if (event.currentTarget.src !== ORDER_IMAGE_FALLBACK) event.currentTarget.src = ORDER_IMAGE_FALLBACK
}

function OrderSummaryCard({ order, onOpen, buttonRef }) {
  const statusDisplay = getOrderStatusDisplay(order)
  const itemCount = order.itemCount ?? order.items.reduce((count, item) => count + Number(item.quantity || 0), 0)

  return (
    <article className="rounded-2xl border border-outline-variant/20 bg-surface-container-lowest p-5 transition-colors hover:border-outline-variant/50">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-primary">Mã đơn hàng: {order.id}</p>
          <p className="mt-1 text-xs text-on-surface-variant">Đặt lúc: {formatDateTime(order.date)}</p>
        </div>
        <Badge variant={statusDisplay.variant}>{statusDisplay.label}</Badge>
      </div>

      <div className="mt-5 flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <div className="flex min-w-0 items-end gap-3">
          <div className="flex -space-x-2" aria-label={`${itemCount} sản phẩm`}>
            {order.items.slice(0, 3).map((item, index) => (
              <img
                key={item.id || index}
                src={item.image}
                alt=""
                aria-hidden="true"
                onError={handleImageError}
                className="h-14 w-12 rounded-xl border-2 border-surface-container-lowest object-cover bg-surface-container-high"
              />
            ))}
          </div>
          <span className="pb-1 text-sm text-on-surface-variant">{itemCount} sản phẩm</span>
        </div>

        <div className="flex items-end justify-between gap-5 sm:justify-end">
          <div className="text-left sm:text-right">
            <p className="text-[11px] uppercase tracking-[0.16em] text-on-surface-variant">Tổng cộng</p>
            <p className="mt-1 text-lg font-semibold text-primary">{formatCurrency(order.total)}</p>
          </div>
          <button
            ref={buttonRef}
            type="button"
            onClick={onOpen}
            aria-label={`Xem chi tiết đơn hàng ${order.id}`}
            className="inline-flex shrink-0 items-center gap-2 rounded-full border border-primary px-4 py-2.5 text-sm font-medium text-primary transition-colors hover:bg-primary hover:text-on-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          >
            Xem chi tiết <ArrowRight size={15} aria-hidden="true" />
          </button>
        </div>
      </div>
    </article>
  )
}

function OrderTimeline({ order }) {
  if (!order.timeline?.length) return null

  return (
    <section aria-labelledby="order-timeline-title" className="mb-8">
      <h3 id="order-timeline-title" className="mb-4 text-[11px] font-medium uppercase tracking-[0.16em] text-on-surface-variant">Tiến trình đơn hàng</h3>
      <div className="flex items-start justify-between gap-2">
        {order.timeline.map((step, index) => (
          <div key={`${step.status}-${index}`} className="relative flex min-w-0 flex-1 flex-col items-center text-center">
            {index < order.timeline.length - 1 && <div className={`absolute left-1/2 top-3.5 h-px w-full ${step.completed ? 'bg-primary' : 'bg-outline-variant/30'}`} aria-hidden="true" />}
            <div className={`relative z-10 flex h-7 w-7 items-center justify-center rounded-full text-[10px] font-medium ${step.completed ? 'bg-primary text-on-primary' : 'border border-outline-variant bg-surface-container-lowest text-on-surface-variant'}`}>
              {step.completed ? '✓' : index + 1}
            </div>
            <span className="mt-2 max-w-20 text-[10px] leading-tight text-on-surface-variant">{step.label}</span>
            {step.date && <span className="mt-1 text-[9px] leading-tight text-on-surface-variant/70">{formatDateTime(step.date)}</span>}
          </div>
        ))}
      </div>
    </section>
  )
}

function DeliveryImagesSection({ order, onOrderUpdated }) {
  const images = order.deliveryImages || []
  const isCompleted = normalizeOrderStatus(order.orderStatus || order.status) === 'COMPLETED'
  const canUpload = isCompleted && images.length < DELIVERY_IMAGE_LIMIT
  const [selectedFile, setSelectedFile] = useState(null)
  const [previewUrl, setPreviewUrl] = useState('')
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState('')
  const [uploadSuccess, setUploadSuccess] = useState('')

  useEffect(() => {
    setSelectedFile(null)
    setPreviewUrl('')
    setUploadError('')
    setUploadSuccess('')
  }, [order.id])

  useEffect(() => () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl)
  }, [previewUrl])

  if (!isCompleted && images.length === 0) return null

  const handleFileChange = (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    setUploadError('')
    setUploadSuccess('')

    if (!file) {
      setSelectedFile(null)
      setPreviewUrl('')
      return
    }

    if (!DELIVERY_IMAGE_ALLOWED_TYPES.has(file.type)) {
      setSelectedFile(null)
      setPreviewUrl('')
      setUploadError('Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP.')
      return
    }

    if (file.size > DELIVERY_IMAGE_MAX_BYTES) {
      setSelectedFile(null)
      setPreviewUrl('')
      setUploadError('Ảnh nhận hàng không được vượt quá 3MB.')
      return
    }

    setSelectedFile(file)
    setPreviewUrl(URL.createObjectURL(file))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (!selectedFile || uploading) return

    setUploading(true)
    setUploadError('')
    setUploadSuccess('')
    try {
      const updatedOrder = await uploadDeliveryImage(order.id, selectedFile)
      onOrderUpdated?.(updatedOrder)
      setSelectedFile(null)
      setPreviewUrl('')
      setUploadSuccess('Đã tải ảnh nhận hàng.')
    } catch (err) {
      setUploadError(err?.message || 'Không thể tải ảnh nhận hàng.')
    } finally {
      setUploading(false)
    }
  }

  return (
    <section aria-labelledby="order-delivery-images-title" className="border-t border-outline-variant/20 pt-5">
      <div className="mb-3 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2 text-on-surface-variant">
          <ImageIcon size={15} aria-hidden="true" />
          <h3 id="order-delivery-images-title" className="text-[11px] font-medium uppercase tracking-[0.16em]">Ảnh nhận hàng</h3>
        </div>
        {images.length > 0 && <span className="text-xs text-on-surface-variant">{images.length}/{DELIVERY_IMAGE_LIMIT}</span>}
      </div>

      {images.length > 0 ? (
        <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
          {images.map((image) => (
            <a
              key={image.id}
              href={image.imageDataUrl}
              target="_blank"
              rel="noreferrer"
              className="group block overflow-hidden rounded-xl border border-outline-variant/20 bg-surface-container-low"
            >
              <img
                src={image.imageDataUrl}
                alt={image.fileName || 'Ảnh nhận hàng'}
                className="aspect-square w-full object-cover transition-transform group-hover:scale-105"
              />
            </a>
          ))}
        </div>
      ) : (
        <p className="mb-4 text-sm text-on-surface-variant">Chưa có ảnh nhận hàng.</p>
      )}

      {canUpload && (
        <form onSubmit={handleSubmit} className="rounded-xl border border-dashed border-outline-variant/40 bg-surface-container-low p-3">
          {previewUrl && (
            <img
              src={previewUrl}
              alt={selectedFile?.name || 'Ảnh nhận hàng'}
              className="mb-3 aspect-video w-full rounded-lg object-cover bg-surface-container-high"
            />
          )}
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <label className="inline-flex cursor-pointer items-center justify-center gap-2 rounded-full border border-primary px-4 py-2.5 text-sm font-medium text-primary transition-colors hover:bg-primary hover:text-on-primary focus-within:outline focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-primary">
              <Upload size={15} aria-hidden="true" />
              Chọn ảnh
              <input type="file" accept={DELIVERY_IMAGE_ACCEPT} onChange={handleFileChange} className="sr-only" />
            </label>
            <button
              type="submit"
              disabled={!selectedFile || uploading}
              className="inline-flex items-center justify-center gap-2 rounded-full bg-primary px-4 py-2.5 text-sm font-medium text-on-primary transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
            >
              {uploading ? <Loader2 size={15} className="animate-spin" aria-hidden="true" /> : <Upload size={15} aria-hidden="true" />}
              Tải ảnh lên
            </button>
          </div>
          {selectedFile && <p className="mt-2 break-all text-xs text-on-surface-variant">{selectedFile.name}</p>}
          {uploadError && <p role="alert" className="mt-2 text-sm text-error">{uploadError}</p>}
          {uploadSuccess && <p role="status" className="mt-2 text-sm text-green-status">{uploadSuccess}</p>}
        </form>
      )}
      {isCompleted && !canUpload && <p className="text-sm text-on-surface-variant">Đã đạt giới hạn 5 ảnh nhận hàng.</p>}
    </section>
  )
}

function OrderDetailContent({ order, onContinuePayment, onRequestCancellation, onRequestReturn, onSubmitReturnBankInfo, onOrderUpdated, bankInfoLoading }) {
  const canContinueSepayPayment = normalizeOrderStatus(order.status) === 'PAYMENT_PENDING'
    && String(order.paymentMethod || '').toLowerCase() === 'sepay'
  const allowCancellation = !isCancellationRequested(order) && (canDirectCancel(order) || canRequestCancellation(order))
  const allowReturn = canRequestCodReturn(order)
  const statusDisplay = getOrderStatusDisplay(order)

  return (
    <div className="space-y-7">
      <header className="border-b border-outline-variant/20 pb-5">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-[11px] font-medium uppercase tracking-[0.16em] text-on-surface-variant">Mã đơn hàng</p>
            <h3 className="mt-2 break-all text-lg font-semibold text-primary">{order.id}</h3>
            <p className="mt-1 text-xs text-on-surface-variant">Đặt lúc {formatDateTime(order.date)}</p>
          </div>
          {order.carrier && (
            <div className="text-right text-xs text-on-surface-variant">
              <div className="flex items-center justify-end gap-1 text-sm text-primary"><Truck size={14} aria-hidden="true" />{order.carrier}</div>
              <p className="mt-1">{order.tracking}</p>
            </div>
          )}
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <Badge variant={statusDisplay.variant}>{statusDisplay.label}</Badge>
          {order.paymentMethod && <Badge variant="default">{isCodPaymentMethod(order.paymentMethod) ? 'Thanh toán khi nhận hàng' : 'SePay'}</Badge>}
        </div>
        <div className="mt-4">
          <OrderCancellationPanel cancellation={order.latestCancellation} refund={order.refund} />
        </div>
      </header>

      <OrderTimeline order={order} />

      <section aria-labelledby="order-items-title">
        <h3 id="order-items-title" className="mb-3 text-[11px] font-medium uppercase tracking-[0.16em] text-on-surface-variant">Nội dung kiện hàng</h3>
        <div className="space-y-3">
          {order.items.map((item, index) => (
            <article key={item.id || index} className="flex gap-3 rounded-xl bg-surface-container-low p-3">
              <img src={item.image} alt={item.name} onError={handleImageError} className="h-20 w-16 shrink-0 rounded-lg object-cover bg-surface-container-high" />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium text-primary">{item.name}</p>
                <p className="mt-1 text-xs text-on-surface-variant">Kích cỡ: {item.size} / Màu sắc: {item.color}</p>
                <p className="mt-1 text-xs text-on-surface-variant">{item.quantity || 1} × {formatCurrency(item.price)}</p>
                <p className="mt-1 text-sm font-semibold text-primary">{formatCurrency(item.price * (item.quantity || 1))}</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section aria-labelledby="order-summary-title" className="border-t border-outline-variant/20 pt-5">
        <h3 id="order-summary-title" className="mb-3 text-[11px] font-medium uppercase tracking-[0.16em] text-on-surface-variant">Tóm tắt thanh toán</h3>
        <div className="space-y-2 text-sm">
          <div className="flex justify-between gap-4"><span className="text-on-surface-variant">Tạm tính</span><span className="text-primary">{formatCurrency(order.subtotal)}</span></div>
          {order.discountAmount > 0 && <div className="flex justify-between gap-4"><span className="text-on-surface-variant">Giảm giá</span><span className="text-green-status">-{formatCurrency(order.discountAmount)}</span></div>}
          {order.hasPricingBreakdown && <>
            <div className="flex justify-between gap-4"><span className="text-on-surface-variant">Phí vận chuyển</span><span className={order.shippingFee === 0 ? 'text-green-status' : 'text-primary'}>{order.shippingFee === 0 ? 'Miễn phí' : formatCurrency(order.shippingFee)}</span></div>
            <div className="flex justify-between gap-4"><span className="text-on-surface-variant">{TAX_LABEL}</span><span className="text-primary">{formatCurrency(order.taxAmount)}</span></div>
          </>}
          <div className="flex justify-between gap-4 border-t border-outline-variant/20 pt-3 font-semibold text-primary"><span>{isCodPaymentMethod(order.paymentMethod) ? 'Số tiền thu hộ COD' : 'Tổng cộng'}</span><span>{formatCurrency(order.total)}</span></div>
        </div>
      </section>

      <section aria-labelledby="order-delivery-title" className="border-t border-outline-variant/20 pt-5">
        <h3 id="order-delivery-title" className="mb-3 text-[11px] font-medium uppercase tracking-[0.16em] text-on-surface-variant">Giao hàng</h3>
        <p className="text-sm font-medium text-primary">{order.shippingRecipientName || 'Người nhận'}</p>
        {order.shippingPhone && <p className="mt-1 text-sm text-on-surface-variant">{order.shippingPhone}</p>}
        <p className="mt-1 text-sm leading-relaxed text-on-surface-variant">{order.shippingAddress || 'Chưa có thông tin'}</p>
      </section>

      <DeliveryImagesSection order={order} onOrderUpdated={onOrderUpdated} />

      {order.latestReturnRequest && (
        <section className="border-t border-outline-variant/20 pt-5">
          <OrderReturnPanel
            returnRequest={order.latestReturnRequest}
            bankInfoLoading={bankInfoLoading}
            onSubmitBankInfo={onSubmitReturnBankInfo}
          />
        </section>
      )}

      {allowReturn && (
        <section className="border-t border-outline-variant/20 pt-5">
          <button
            type="button"
            onClick={onRequestReturn}
            className="inline-flex w-full items-center justify-center gap-2 rounded-full border border-primary px-5 py-3 text-sm font-medium text-primary transition-colors hover:bg-primary hover:text-on-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          >
            Yêu cầu hoàn hàng
          </button>
        </section>
      )}

      {allowCancellation && (
        <section className="border-t border-outline-variant/20 pt-5">
          <button
            type="button"
            onClick={onRequestCancellation}
            className="inline-flex w-full items-center justify-center gap-2 rounded-full border border-outline-variant/30 px-5 py-3 text-sm font-medium text-primary transition-colors hover:bg-surface-container-high focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
          >
            {canDirectCancel(order) ? 'Hủy đơn' : 'Yêu cầu hủy'}
          </button>
        </section>
      )}

      {canContinueSepayPayment && (
        <section className="border-t border-outline-variant/20 pt-5">
          {order.paymentExpiresAt && <p className="mb-3 text-xs leading-relaxed text-on-surface-variant">Phiên thanh toán hết hạn lúc {formatDateTime(order.paymentExpiresAt)}.</p>}
          <button type="button" onClick={onContinuePayment} className="inline-flex w-full items-center justify-center gap-2 rounded-full bg-primary px-5 py-3 text-sm font-medium text-on-primary transition-opacity hover:opacity-90 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary">Tiếp tục thanh toán <ArrowRight size={16} aria-hidden="true" /></button>
        </section>
      )}
    </div>
  )
}

function OrderDetailLoading() {
  return <div className="space-y-4 animate-pulse" aria-label="Đang tải chi tiết đơn hàng"><div className="h-20 rounded-xl bg-surface-container-high" /><div className="h-32 rounded-xl bg-surface-container-high" /><div className="h-24 rounded-xl bg-surface-container-high" /></div>
}

export default function OrderTrackingPage() {
  const [orders, setOrders] = useState([])
  const [selectedTab, setSelectedTab] = useState('All')
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [selectedOrderId, setSelectedOrderId] = useState(null)
  const [detailOrder, setDetailOrder] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [cancellationDialogOpen, setCancellationDialogOpen] = useState(false)
  const [cancellationSubmitting, setCancellationSubmitting] = useState(false)
  const [returnDialogOpen, setReturnDialogOpen] = useState(false)
  const [returnSubmitting, setReturnSubmitting] = useState(false)
  const [bankInfoSubmitting, setBankInfoSubmitting] = useState(false)
  const [actionError, setActionError] = useState('')
  const triggerRefs = useRef(new Map())
  const selectedOrderIdRef = useRef(null)
  const detailRequestIdRef = useRef(0)
  const listRequestIdRef = useRef(0)
  const focusRestoreTimerRef = useRef(null)
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    const requestId = listRequestIdRef.current + 1
    listRequestIdRef.current = requestId
    setLoading(true)
    setError('')
    getOrders({
      page: currentPage,
      size: 10,
      sort: 'createdAt,desc',
      status: selectedTab === 'All' ? undefined : selectedTab.toUpperCase(),
    })
      .then((pageData) => {
        if (listRequestIdRef.current !== requestId) return
        setOrders(pageData.content)
        setCurrentPage(pageData.page ?? currentPage)
        setTotalPages(pageData.totalPages ?? 0)
        setTotalElements(pageData.totalElements ?? 0)
      })
      .catch(() => {
        if (listRequestIdRef.current === requestId) setError('Không thể tải đơn hàng.')
      })
      .finally(() => {
        if (listRequestIdRef.current === requestId) setLoading(false)
      })
  }, [currentPage, selectedTab])

  useEffect(() => {
    if (totalPages > 0 && currentPage >= totalPages) setCurrentPage(totalPages - 1)
  }, [currentPage, totalPages])

  const openOrderDetail = async (order) => {
    const requestId = detailRequestIdRef.current + 1
    detailRequestIdRef.current = requestId
    selectedOrderIdRef.current = order.id
    setSelectedOrderId(order.id)
    setDetailOrder(null)
    setDetailError('')
    setActionError('')
    setDetailLoading(true)
    try {
      const nextOrder = await getOrderById(order.id)
      if (detailRequestIdRef.current === requestId && selectedOrderIdRef.current === order.id) {
        setDetailOrder(nextOrder)
        setOrders((currentOrders) => mergeOrderSummaryUpdate(currentOrders, nextOrder))
        return nextOrder
      }
    } catch {
      if (detailRequestIdRef.current === requestId && selectedOrderIdRef.current === order.id) setDetailError('Không thể tải chi tiết đơn hàng.')
    } finally {
      if (detailRequestIdRef.current === requestId && selectedOrderIdRef.current === order.id) setDetailLoading(false)
    }
    return null
  }

  const syncOrderListItem = (updatedOrder) => {
    if (!updatedOrder?.id) return
    setOrders((currentOrders) => mergeOrderSummaryUpdate(currentOrders, updatedOrder))
  }

  const closeOrderDetail = useCallback(() => {
    const orderId = selectedOrderIdRef.current
    detailRequestIdRef.current += 1
    selectedOrderIdRef.current = null
    setSelectedOrderId(null)
    setDetailOrder(null)
    setDetailError('')
    if (focusRestoreTimerRef.current) window.clearTimeout(focusRestoreTimerRef.current)
    focusRestoreTimerRef.current = window.setTimeout(() => triggerRefs.current.get(orderId)?.focus(), 250)
  }, [])

  const retryOrderDetail = () => {
    const order = orders.find((candidate) => candidate.id === selectedOrderId)
    if (order) openOrderDetail(order)
  }

  useEffect(() => () => {
    if (focusRestoreTimerRef.current) window.clearTimeout(focusRestoreTimerRef.current)
  }, [])

  const handleContinuePayment = () => {
    if (!detailOrder) return
    navigate('/checkout', { state: { resumePaymentOrder: detailOrder } })
  }

  const handleRequestCancellation = async (payload) => {
    if (!detailOrder || cancellationSubmitting) return
    setCancellationSubmitting(true)
    setActionError('')
    try {
      await requestOrderCancellation(detailOrder.id, payload, { idempotencyKey: `cancel-${detailOrder.id}` })
      const updatedOrder = await openOrderDetail({ id: detailOrder.id })
      syncOrderListItem(updatedOrder)
      setCancellationDialogOpen(false)
    } catch (err) {
      setActionError(err?.message || 'Không thể gửi yêu cầu hủy đơn. Vui lòng thử lại sau.')
    } finally {
      setCancellationSubmitting(false)
    }
  }

  const handleRequestReturn = async (payload) => {
    if (!detailOrder || returnSubmitting) return
    setReturnSubmitting(true)
    setActionError('')
    try {
      await createOrderReturnRequest(detailOrder.id, payload, { idempotencyKey: `return-${detailOrder.id}` })
      const updatedOrder = await openOrderDetail({ id: detailOrder.id })
      syncOrderListItem(updatedOrder)
      setReturnDialogOpen(false)
    } catch (err) {
      setActionError(err?.message || 'Không thể gửi yêu cầu hoàn hàng. Vui lòng thử lại sau.')
    } finally {
      setReturnSubmitting(false)
    }
  }

  const handleSubmitReturnBankInfo = async (payload) => {
    if (!detailOrder?.latestReturnRequest?.id || bankInfoSubmitting) return
    setBankInfoSubmitting(true)
    setActionError('')
    try {
      await submitReturnBankInfo(detailOrder.id, detailOrder.latestReturnRequest.id, payload)
      const updatedOrder = await openOrderDetail({ id: detailOrder.id })
      syncOrderListItem(updatedOrder)
    } catch (err) {
      setActionError(err?.message || 'Không thể gửi thông tin ngân hàng. Vui lòng thử lại sau.')
    } finally {
      setBankInfoSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-5xl px-5 py-10 sm:px-8 lg:py-14">
      <h1 className="font-headline-md text-primary">Đơn hàng của tôi</h1>

      {location.state?.flashMessage && <div role="status" className="mt-6 rounded-xl border border-tertiary-container/30 bg-tertiary-container/20 px-4 py-3 text-sm text-primary">{location.state.flashMessage}</div>}
      {actionError && <div role="alert" className="mt-6 rounded-xl border border-error/20 bg-error-container/30 px-4 py-3 text-sm text-error">{actionError}</div>}

      <section className="mt-8" aria-label="Danh sách đơn hàng">
        <div className="flex flex-wrap gap-2 border-b border-outline-variant/20 pb-5">
          {statusTabs.map((tab) => <button key={tab.key} type="button" onClick={() => { setSelectedTab(tab.key); setCurrentPage(0) }} aria-pressed={selectedTab === tab.key} className={`rounded-full px-4 py-2 text-xs font-medium transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${selectedTab === tab.key ? 'bg-primary text-on-primary' : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-high'}`}>{tab.label}</button>)}
        </div>

        <div aria-live="polite">
          {loading && <div className="py-16 text-center text-sm text-on-surface-variant">Đang tải đơn hàng...</div>}
          {!loading && error && <div role="alert" className="mt-8 rounded-xl border border-error/20 bg-error-container/30 p-6 text-sm text-error">{error}</div>}
          {!loading && !error && totalElements === 0 && <div className="py-20 text-center"><Package size={48} className="mx-auto mb-4 text-on-surface-variant/30" /><p className="text-on-surface-variant">{selectedTab === 'All' ? 'Bạn chưa có đơn hàng nào.' : 'Không có đơn hàng trong trạng thái này.'}</p></div>}
          {!loading && !error && totalElements > 0 && <div className="mt-6 space-y-4">
            {orders.map((order) => <OrderSummaryCard key={order.id} order={order} onOpen={() => openOrderDetail(order)} buttonRef={(node) => { if (node) triggerRefs.current.set(order.id, node) }} />)}
          </div>}
        </div>
        <Pagination page={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} label="Phân trang đơn hàng" />
        {totalElements > 0 && <p className="pb-3 text-center text-xs text-on-surface-variant">Trang {currentPage + 1} / {totalPages} · {totalElements} đơn hàng</p>}
      </section>

      <Drawer isOpen={Boolean(selectedOrderId)} onClose={closeOrderDetail} title="Chi tiết đơn hàng" panelClassName="max-w-2xl">
        {detailLoading && <OrderDetailLoading />}
        {!detailLoading && detailError && <div role="alert" className="py-12 text-center"><p className="text-sm text-error">{detailError}</p><button type="button" onClick={retryOrderDetail} className="mt-4 rounded-full border border-primary px-4 py-2 text-sm font-medium text-primary">Thử lại</button></div>}
        {!detailLoading && actionError && <div role="alert" className="mb-4 rounded-xl border border-error/20 bg-error-container/30 px-4 py-3 text-sm text-error">{actionError}</div>}
        {!detailLoading && !detailError && detailOrder && (
          <OrderDetailContent
            order={detailOrder}
            onContinuePayment={handleContinuePayment}
            onRequestCancellation={() => setCancellationDialogOpen(true)}
            onRequestReturn={() => setReturnDialogOpen(true)}
            onSubmitReturnBankInfo={handleSubmitReturnBankInfo}
            bankInfoLoading={bankInfoSubmitting}
            onOrderUpdated={setDetailOrder}
          />
        )}
      </Drawer>

      <OrderCancellationDialog
        isOpen={cancellationDialogOpen}
        loading={cancellationSubmitting}
        onClose={() => setCancellationDialogOpen(false)}
        onConfirm={handleRequestCancellation}
      />
      <OrderReturnDialog
        isOpen={returnDialogOpen}
        loading={returnSubmitting}
        onClose={() => setReturnDialogOpen(false)}
        onConfirm={handleRequestReturn}
      />
    </div>
  )
}
