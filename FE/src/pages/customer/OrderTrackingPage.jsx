import { useState, useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ArrowRight, Package, Truck } from 'lucide-react'
import Badge from '../../components/common/Badge'
import { getOrders } from '../../features/orders/order.api'
import { formatDateTime } from '../../utils/formatDate'
import { formatCurrency } from '../../utils/formatCurrency'
import { formatStatusLabel, normalizeOrderStatus } from '../../features/orders/orderStatus'
import { isCodPaymentMethod, TAX_LABEL } from '../../features/cart/cart.utils'

const statusTabs = [
  { key: 'All', label: 'Tất cả' },
  { key: 'Processing', label: 'Đang xử lý' },
  { key: 'Shipped', label: 'Đang giao' },
  { key: 'Completed', label: 'Đã giao thành công' },
]

const badgeVariants = {
  completed: 'success',
  shipped: 'warning',
  processing: 'secondary',
  confirmed: 'secondary',
  paid: 'success',
  pending: 'default',
  payment_pending: 'warning',
  cancelled: 'error',
  expired: 'default',
  failed: 'error',
}

export default function OrderTrackingPage() {
  const [orders, setOrders] = useState([])
  const [selectedTab, setSelectedTab] = useState('All')
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    getOrders()
      .then((o) => {
        setOrders(o)
        if (o.length > 0) setSelectedOrder(o[0])
      })
      .catch(() => setError('Không thể tải đơn hàng.'))
      .finally(() => setLoading(false))
  }, [])

  const filteredOrders = selectedTab === 'All'
    ? orders
    : orders.filter((o) => normalizeOrderStatus(o.status).toLowerCase() === selectedTab.toLowerCase())
  const selectedOrderStatus = normalizeOrderStatus(selectedOrder?.status)
  const canContinueSepayPayment = selectedOrder
    && selectedOrderStatus === 'PAYMENT_PENDING'
    && String(selectedOrder.paymentMethod || '').toLowerCase() === 'sepay'

  const handleContinuePayment = () => {
    if (!selectedOrder) return
    navigate('/checkout', {
      state: { resumePaymentOrder: selectedOrder },
    })
  }

  return (
    <div className="max-w-[1440px] mx-auto px-6 md:px-16 py-8">
      <h1 className="font-headline-md text-primary mb-8">Đơn hàng của tôi</h1>

      {location.state?.flashMessage && (
        <div role="status" className="mb-6 rounded-lg border border-tertiary-container/30 bg-tertiary-container/20 px-4 py-3 text-sm text-primary">
          {location.state.flashMessage}
        </div>
      )}

      {loading && <div className="py-20 text-center text-on-surface-variant">Đang tải đơn hàng...</div>}
      {error && (
        <div role="alert" className="rounded-xl border border-error/20 bg-error-container/30 p-6 text-sm text-error">
          {error}
        </div>
      )}
      {!loading && !error && orders.length === 0 && (
        <div className="py-20 text-center">
          <Package size={48} className="text-on-surface-variant/30 mx-auto mb-4" />
          <p className="text-on-surface-variant">Bạn chưa có đơn hàng nào.</p>
        </div>
      )}

      {!loading && !error && orders.length > 0 && (
      <div className="flex gap-6">
        {/* Left: Order List */}
          <div className="w-full lg:w-5/12 space-y-4">
            {/* Filter Tabs */}
            <div className="flex gap-2">
            {statusTabs.map((tab) => (
                <button
                key={tab.key}
                onClick={() => setSelectedTab(tab.key)}
                  className={`px-4 py-2 rounded-full text-xs font-medium transition-all ${
                  selectedTab === tab.key
                    ? 'bg-primary text-on-primary'
                    : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-high'
                }`}
              >
                {tab.label}
                </button>
              ))}
            </div>

          {/* Order Cards */}
          <div className="space-y-3">
            {filteredOrders.map((order) => (
              <button
                key={order.id}
                onClick={() => {
                  setSelectedOrder(order)
                }}
                  className={`w-full text-left bg-surface-container-lowest rounded-xl p-4 transition-all border-l-4 ${
                    selectedOrder?.id === order.id
                      ? 'border-primary ambient-shadow'
                      : 'border-transparent hover:bg-surface-container-low'
                  }`}
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-primary">{order.id}</span>
                  <Badge variant={badgeVariants[normalizeOrderStatus(order.status).toLowerCase()] || 'default'}>
                    {formatStatusLabel(order.status)}
                  </Badge>
                </div>
                <p className="text-xs text-on-surface-variant">{formatDateTime(order.date)}</p>
                <div className="flex gap-2 mt-2">
                  {order.items.slice(0, 3).map((item, idx) => (
                    <img key={idx} src={item.image} alt={item.name} className="w-10 h-10 object-cover rounded-lg" />
                  ))}
                </div>
                <p className="text-sm font-semibold text-primary mt-2">{formatCurrency(order.total)}</p>
              </button>
            ))}
          </div>
        </div>

        {/* Right: Order Detail */}
        {selectedOrder && (
          <div className="hidden lg:block w-7/12">
            <div className="custom-scrollbar sticky top-28 max-h-[calc(100vh-8rem)] overflow-y-auto bg-surface-container-lowest rounded-xl p-6 ambient-shadow">
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h2 className="font-title-lg text-primary">{selectedOrder.id}</h2>
                  <p className="text-xs text-on-surface-variant mt-1">Đặt lúc {formatDateTime(selectedOrder.date)}</p>
                </div>
                {selectedOrder.carrier && (
                  <div className="text-right">
                    <div className="flex items-center gap-1 text-sm text-primary">
                      <Truck size={14} /> {selectedOrder.carrier}
                    </div>
                    <p className="text-xs text-on-surface-variant mt-0.5">{selectedOrder.tracking}</p>
                  </div>
                )}
              </div>

              {/* Timeline */}
              <div className="mb-8">
                <div className="flex items-center justify-between relative">
                  <div className="absolute top-4 left-4 right-4 h-px bg-outline-variant/30" />
                  {selectedOrder.timeline.map((step, idx) => (
                      <div
                        key={idx}
                        className={`relative flex min-w-0 flex-col items-center z-10 text-center ${
                          selectedOrder.timeline.length <= 5 ? 'max-w-28' : 'max-w-20'
                        }`}
                      >
                        <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-medium ${
                          step.completed ? 'bg-primary text-on-primary' : 'bg-surface-container-lowest border-2 border-outline-variant text-on-surface-variant'
                        }`}>
                          {step.completed ? '✓' : idx + 1}
                        </div>
                      <span className="mt-2 text-[10px] leading-tight text-on-surface-variant break-words">{step.label}</span>
                      {step.date && <span className="mt-1 text-[9px] leading-tight text-on-surface-variant/70">{formatDateTime(step.date)}</span>}
                      </div>
                  ))}
                </div>
              </div>

              {/* Items */}
              <h3 className="font-label-sm uppercase tracking-wider text-on-surface-variant mb-3">Nội dung kiện hàng</h3>
              <div className="space-y-3 mb-6">
                {selectedOrder.items.map((item, idx) => (
                  <div key={idx} className="flex gap-3 p-3 bg-surface-container-low rounded-lg">
                    <img src={item.image} alt={item.name} className="w-16 h-20 object-cover rounded-lg" />
                    <div>
                      <p className="text-sm font-medium text-primary">{item.name}</p>
                      <p className="text-xs text-on-surface-variant">Kích cỡ: {item.size} / Màu sắc: {item.color}</p>
                      <p className="text-xs text-on-surface-variant mt-1">
                        {item.quantity || 1} × {formatCurrency(item.price)}
                      </p>
                      <p className="text-sm font-semibold text-primary mt-1">{formatCurrency(item.price * (item.quantity || 1))}</p>
                    </div>
                  </div>
                ))}
              </div>

              {/* Summary */}
              <div className="border-t border-outline-variant/20 pt-4">
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <span className="text-on-surface-variant">Tạm tính</span>
                    <span className="text-primary">{formatCurrency(selectedOrder.subtotal)}</span>
                  </div>
                  {selectedOrder.hasPricingBreakdown && (
                    <>
                      <div className="flex justify-between">
                        <span className="text-on-surface-variant">Phí vận chuyển</span>
                        <span className={selectedOrder.shippingFee === 0 ? 'text-green-status' : 'text-primary'}>
                          {selectedOrder.shippingFee === 0 ? 'Miễn phí' : formatCurrency(selectedOrder.shippingFee)}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-on-surface-variant">{TAX_LABEL}</span>
                        <span className="text-primary">{formatCurrency(selectedOrder.taxAmount)}</span>
                      </div>
                    </>
                  )}
                  <div className="border-t border-outline-variant/20 pt-2 flex justify-between font-semibold text-primary">
                    <span>{isCodPaymentMethod(selectedOrder.paymentMethod) ? 'Số tiền thu hộ COD' : 'Tổng cộng'}</span>
                    <span>{formatCurrency(selectedOrder.total)}</span>
                  </div>
                </div>
              </div>

              {canContinueSepayPayment && (
                <div className="mt-6 border-t border-outline-variant/20 pt-5">
                  {selectedOrder.paymentExpiresAt && (
                    <p className="mb-3 text-xs leading-relaxed text-on-surface-variant">
                      Phiên thanh toán hết hạn lúc {formatDateTime(selectedOrder.paymentExpiresAt)}.
                    </p>
                  )}
                  <button
                    type="button"
                    onClick={handleContinuePayment}
                    className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-primary px-5 py-3 text-sm font-medium text-on-primary transition-opacity hover:opacity-90 active:scale-95"
                  >
                    Tiếp tục thanh toán <ArrowRight size={16} />
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
      )}
    </div>
  )
}
