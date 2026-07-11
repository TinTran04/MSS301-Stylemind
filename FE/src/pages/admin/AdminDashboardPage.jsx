import { useCallback, useEffect, useState } from 'react'
import { DollarSign, ShoppingCart, Package, Users, Bell, RefreshCw } from 'lucide-react'
import MetricCard from '../../components/admin/MetricCard'
import ChartCard from '../../components/admin/ChartCard'
import {
  getOrderSummary,
  getProductSummary,
  getUserSummary,
  getNotificationSummary,
} from '../../features/admin/adminDashboard.api'
import { formatCurrency, formatNumber } from '../../utils/formatCurrency'

const INITIAL = { loading: true, error: null, data: null }

// Renders a metric value that reflects the slice's real state — never a fake
// fallback: '…' while loading, '—' on error, otherwise the formatted value.
function statValue(slice, pick, format = formatNumber) {
  if (slice.loading) return '…'
  if (slice.error || !slice.data) return '—'
  return format(pick(slice.data))
}

function ErrorBanner({ message }) {
  return (
    <div role="alert" className="rounded-lg border border-error/20 bg-error-container/40 px-4 py-3 text-sm text-error">
      {message}
    </div>
  )
}

export default function AdminDashboardPage() {
  const [orders, setOrders] = useState(INITIAL)
  const [products, setProducts] = useState(INITIAL)
  const [users, setUsers] = useState(INITIAL)
  const [notifications, setNotifications] = useState(INITIAL)
  const [loadedAt, setLoadedAt] = useState(null)

  const load = useCallback(() => {
    setOrders(INITIAL)
    setProducts(INITIAL)
    setUsers(INITIAL)
    setNotifications(INITIAL)

    const run = (promise, setter) =>
      promise
        .then((data) => setter({ loading: false, error: null, data }))
        .catch((err) => setter({ loading: false, error: err?.message || 'Không thể tải dữ liệu.', data: null }))

    Promise.allSettled([
      run(getOrderSummary(), setOrders),
      run(getProductSummary(), setProducts),
      run(getUserSummary(), setUsers),
      run(getNotificationSummary(), setNotifications),
    ]).finally(() => setLoadedAt(new Date()))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const anyLoading = orders.loading || products.loading || users.loading || notifications.loading
  const noOrders = !orders.loading && !orders.error && orders.data?.totalOrders === 0

  return (
    <div className="space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-headline-md text-primary">Tổng quan</h1>
          <p className="text-sm text-on-surface-variant mt-1">Chỉ số vận hành trực tiếp</p>
        </div>
        <div className="flex items-center gap-4">
          <span className="text-xs text-on-surface-variant">
            {anyLoading ? 'Đang tải…' : loadedAt ? `Cập nhật lần cuối: ${loadedAt.toLocaleTimeString('vi-VN')}` : ''}
          </span>
          <button
            onClick={load}
            disabled={anyLoading}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-high transition-colors disabled:opacity-50"
          >
            <RefreshCw size={14} className={anyLoading ? 'animate-spin' : ''} />
            Làm mới
          </button>
        </div>
      </div>

      {/* Orders & revenue */}
      <section className="space-y-4">
        <h2 className="font-title-lg text-primary">Đơn hàng &amp; doanh thu</h2>
        {orders.error && <ErrorBanner message={`Không thể tải chỉ số đơn hàng: ${orders.error}`} />}
        {noOrders && (
          <p className="text-sm text-on-surface-variant">Chưa có đơn hàng nào — chỉ số sẽ tự cập nhật khi có đơn mới.</p>
        )}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <MetricCard title="Tổng đơn hàng" value={statValue(orders, (d) => d.totalOrders)} icon={ShoppingCart} />
          <MetricCard title="Tổng doanh thu" value={statValue(orders, (d) => d.totalRevenue, formatCurrency)} icon={DollarSign} />
          <MetricCard title="Đơn hàng hôm nay" value={statValue(orders, (d) => d.todayOrders)} icon={ShoppingCart} />
          <MetricCard title="Doanh thu hôm nay" value={statValue(orders, (d) => d.todayRevenue, formatCurrency)} icon={DollarSign} />
        </div>
        <ChartCard title="Phân bổ trạng thái đơn hàng">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[
              { label: 'Đang chờ', pick: (d) => d.pendingOrders },
              { label: 'Đã thanh toán', pick: (d) => d.paidOrders },
              { label: 'Hoàn tất', pick: (d) => d.completedOrders },
              { label: 'Đã hủy', pick: (d) => d.cancelledOrders },
            ].map(({ label, pick }) => (
              <div key={label} className="text-center">
                <p className="font-headline-md text-primary">{statValue(orders, pick)}</p>
                <p className="font-label-sm uppercase text-on-surface-variant mt-1">{label}</p>
              </div>
            ))}
          </div>
        </ChartCard>
      </section>

      {/* Catalogue */}
      <section className="space-y-4">
        <h2 className="font-title-lg text-primary">Danh mục sản phẩm</h2>
        {products.error && <ErrorBanner message={`Không thể tải chỉ số sản phẩm: ${products.error}`} />}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <MetricCard title="Tổng sản phẩm" value={statValue(products, (d) => d.totalProducts)} icon={Package} />
          <MetricCard title="Đang bán" value={statValue(products, (d) => d.activeProducts)} icon={Package} />
          <MetricCard title="Ngừng bán" value={statValue(products, (d) => d.inactiveProducts)} icon={Package} />
        </div>
      </section>

      {/* Users */}
      <section className="space-y-4">
        <h2 className="font-title-lg text-primary">Người dùng</h2>
        {users.error && <ErrorBanner message={`Không thể tải chỉ số người dùng: ${users.error}`} />}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <MetricCard title="Tổng người dùng" value={statValue(users, (d) => d.totalUsers)} icon={Users} />
          <MetricCard title="Khách hàng" value={statValue(users, (d) => d.totalCustomers)} icon={Users} />
          <MetricCard title="Quản trị viên" value={statValue(users, (d) => d.totalAdmins)} icon={Users} />
        </div>
      </section>

      {/* Notifications */}
      <section className="space-y-4">
        <h2 className="font-title-lg text-primary">Thông báo</h2>
        {notifications.error && <ErrorBanner message={`Không thể tải chỉ số thông báo: ${notifications.error}`} />}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <MetricCard
            title="Thông báo thất bại"
            value={statValue(notifications, (d) => d.failedNotifications)}
            icon={Bell}
            status={!notifications.loading && !notifications.error && notifications.data?.failedNotifications > 0 ? 'warning' : undefined}
          />
        </div>
      </section>
    </div>
  )
}
