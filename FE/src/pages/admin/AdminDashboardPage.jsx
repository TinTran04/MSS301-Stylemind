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
        .catch((err) => setter({ loading: false, error: err?.message || 'Failed to load', data: null }))

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
          <h1 className="font-headline-md text-primary">Dashboard</h1>
          <p className="text-sm text-on-surface-variant mt-1">Live operational metrics</p>
        </div>
        <div className="flex items-center gap-4">
          <span className="text-xs text-on-surface-variant">
            {anyLoading ? 'Loading…' : loadedAt ? `Last updated: ${loadedAt.toLocaleTimeString()}` : ''}
          </span>
          <button
            onClick={load}
            disabled={anyLoading}
            className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-high transition-colors disabled:opacity-50"
          >
            <RefreshCw size={14} className={anyLoading ? 'animate-spin' : ''} />
            Refresh
          </button>
        </div>
      </div>

      {/* Orders & revenue */}
      <section className="space-y-4">
        <h2 className="font-title-lg text-primary">Orders &amp; Revenue</h2>
        {orders.error && <ErrorBanner message={`Couldn't load order metrics: ${orders.error}`} />}
        {noOrders && (
          <p className="text-sm text-on-surface-variant">No orders yet — metrics will populate as orders come in.</p>
        )}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <MetricCard title="Total Orders" value={statValue(orders, (d) => d.totalOrders)} icon={ShoppingCart} />
          <MetricCard title="Total Revenue" value={statValue(orders, (d) => d.totalRevenue, formatCurrency)} icon={DollarSign} />
          <MetricCard title="Today's Orders" value={statValue(orders, (d) => d.todayOrders)} icon={ShoppingCart} />
          <MetricCard title="Today's Revenue" value={statValue(orders, (d) => d.todayRevenue, formatCurrency)} icon={DollarSign} />
        </div>
        <ChartCard title="Order Status Breakdown">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[
              { label: 'Pending', pick: (d) => d.pendingOrders },
              { label: 'Paid', pick: (d) => d.paidOrders },
              { label: 'Completed', pick: (d) => d.completedOrders },
              { label: 'Cancelled', pick: (d) => d.cancelledOrders },
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
        <h2 className="font-title-lg text-primary">Catalogue</h2>
        {products.error && <ErrorBanner message={`Couldn't load product metrics: ${products.error}`} />}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <MetricCard title="Total Products" value={statValue(products, (d) => d.totalProducts)} icon={Package} />
          <MetricCard title="Active" value={statValue(products, (d) => d.activeProducts)} icon={Package} />
          <MetricCard title="Inactive" value={statValue(products, (d) => d.inactiveProducts)} icon={Package} />
        </div>
      </section>

      {/* Users */}
      <section className="space-y-4">
        <h2 className="font-title-lg text-primary">Users</h2>
        {users.error && <ErrorBanner message={`Couldn't load user metrics: ${users.error}`} />}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <MetricCard title="Total Users" value={statValue(users, (d) => d.totalUsers)} icon={Users} />
          <MetricCard title="Customers" value={statValue(users, (d) => d.totalCustomers)} icon={Users} />
          <MetricCard title="Admins" value={statValue(users, (d) => d.totalAdmins)} icon={Users} />
        </div>
      </section>

      {/* Notifications */}
      <section className="space-y-4">
        <h2 className="font-title-lg text-primary">Notifications</h2>
        {notifications.error && <ErrorBanner message={`Couldn't load notification metrics: ${notifications.error}`} />}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <MetricCard
            title="Failed Notifications"
            value={statValue(notifications, (d) => d.failedNotifications)}
            icon={Bell}
            status={!notifications.loading && !notifications.error && notifications.data?.failedNotifications > 0 ? 'warning' : undefined}
          />
        </div>
      </section>
    </div>
  )
}
