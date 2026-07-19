import { useState, useEffect, useCallback } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ShoppingCart, TrendingDown, Clock, DollarSign, Eye, RefreshCw, Filter, Search } from 'lucide-react'
import MetricCard from '../../components/admin/MetricCard'
import StatusBadge from '../../components/admin/StatusBadge'
import { getAdminOrders } from '../../features/orders/admin-order.api'
import {
  formatStatusLabel,
  ORDER_STATUS_TRANSITIONS,
  ORDER_REVENUE_STATUSES,
  normalizeOrderStatus,
} from '../../features/orders/orderStatus'
import { getAdminErrorMessage } from '../../features/admin/admin-error-messages'
import { formatCurrency } from '../../utils/formatCurrency'
import { formatDate } from '../../utils/formatDate'
import { buildAdminOrderDetailPath } from './adminOrderDetail.utils'

const STATUS_OPTIONS = ['All', ...Object.keys(ORDER_STATUS_TRANSITIONS)]
const STATUS_FILTER_LABELS = {
  All: 'Mọi trạng thái',
}

export default function OrderManagementPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [listError, setListError] = useState(null)
  const [statusFilter, setStatusFilter] = useState('All')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')
  const [customerInput, setCustomerInput] = useState('')
  const [customerFilter, setCustomerFilter] = useState('')

  const fetchOrders = useCallback(async () => {
    setLoading(true)
    setListError(null)
    try {
      const data = await getAdminOrders({
        status: statusFilter === 'All' ? undefined : statusFilter,
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
        userId: customerFilter || undefined,
      })
      setOrders(data.content || data || [])
    } catch (err) {
      const friendly = getAdminErrorMessage(err, {
        fallbackTitle: 'Không thể tải danh sách đơn hàng',
        fallbackMessage: 'Hệ thống chưa thể tải danh sách đơn hàng. Vui lòng làm mới trang hoặc thử lại sau.',
      })
      setListError(friendly)
    } finally {
      setLoading(false)
    }
  }, [statusFilter, fromDate, toDate, customerFilter])

  useEffect(() => {
    fetchOrders()
  }, [fetchOrders])

  const handleCustomerSearch = (e) => {
    e.preventDefault()
    setCustomerFilter(customerInput.trim())
  }

  const getOrderStatus = (order) => normalizeOrderStatus(order?.orderStatus || 'PENDING')
  const revenueTotal = orders.reduce((sum, order) => {
    const status = normalizeOrderStatus(order.orderStatus)
    if (!ORDER_REVENUE_STATUSES.has(status)) return sum
    return sum + (order.totalAmount || order.total || 0)
  }, 0)

  const navigateToDetail = (orderId, event) => {
    event?.stopPropagation()
    navigate(buildAdminOrderDetailPath(orderId, location.search))
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="font-headline-md text-primary">Quản lý đơn hàng</h1>
        <button onClick={fetchOrders} disabled={loading} className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-high transition-colors disabled:opacity-40">
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} /> Làm mới
        </button>
      </div>

      {listError && (
        <div className="rounded-lg border border-error/20 bg-error-container/40 px-4 py-3 text-sm text-error">
          <p className="font-medium">{listError.title}</p>
          <p className="mt-0.5">{listError.message}</p>
        </div>
      )}

      <div className="flex flex-col lg:flex-row gap-3 lg:items-center">
        <div className="flex items-center gap-2">
          <Filter size={16} className="text-on-surface-variant shrink-0" />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-surface-container-low border border-outline-variant/20 rounded-lg px-3 py-1.5 text-xs font-medium text-on-surface focus:outline-none focus:border-tertiary-container"
          >
            {STATUS_OPTIONS.map((status) => (
              <option key={status} value={status}>
                {STATUS_FILTER_LABELS[status] || formatStatusLabel(status)}
              </option>
            ))}
          </select>
        </div>
        <div className="flex items-center gap-2">
          <input
            type="date"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
            className="bg-surface-container-low border border-outline-variant/20 rounded-lg px-3 py-1.5 text-xs text-on-surface focus:outline-none focus:border-tertiary-container"
          />
          <span className="text-xs text-on-surface-variant">đến</span>
          <input
            type="date"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
            className="bg-surface-container-low border border-outline-variant/20 rounded-lg px-3 py-1.5 text-xs text-on-surface focus:outline-none focus:border-tertiary-container"
          />
        </div>
        <form onSubmit={handleCustomerSearch} className="relative flex-1 max-w-xs">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant" />
          <input
            value={customerInput}
            onChange={(e) => setCustomerInput(e.target.value)}
            placeholder="Tìm theo khách hàng hoặc mã người dùng…"
            className="w-full pl-8 pr-4 py-1.5 bg-surface-container-low rounded-lg text-xs border border-outline-variant/20 outline-none focus:border-tertiary-container"
          />
        </form>
        {customerFilter && (
          <button
            onClick={() => { setCustomerInput(''); setCustomerFilter('') }}
            className="text-xs text-on-surface-variant hover:text-primary underline"
          >
            Xóa bộ lọc khách hàng
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard title="Giao dịch" value={orders.length} change={0} icon={ShoppingCart} />
        <MetricCard title="Tỷ lệ thất bại" value="0%" change={0} icon={TrendingDown} status="good" />
        <MetricCard title="Thời gian xử lý TB" value="-" change={0} icon={Clock} status="good" />
        <MetricCard title="Doanh thu" value={formatCurrency(revenueTotal)} change={0} icon={DollarSign} />
      </div>

      <div className="bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-surface-container-low/50">
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Mã đơn hàng</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Khách hàng</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Ngày tạo</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Tổng tiền</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Trạng thái</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/5">
              {loading && orders.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-sm text-on-surface-variant">Đang tải đơn hàng...</td>
                </tr>
              ) : orders.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-sm text-on-surface-variant">Không tìm thấy đơn hàng nào.</td>
                </tr>
              ) : orders.map((order) => (
                <tr key={order.id} className="hover:bg-surface-container-high/30">
                  <td className="px-4 py-3 text-sm font-medium text-primary">{order.id}</td>
                  <td className="px-4 py-3 text-sm text-on-surface">{order.customerName || order.userId || 'Khách vãng lai'}</td>
                  <td className="px-4 py-3 text-sm text-on-surface-variant">{formatDate(order.createdAt || order.date)}</td>
                  <td className="px-4 py-3 text-sm text-primary font-medium">{formatCurrency(order.totalAmount || order.total || 0)}</td>
                  <td className="px-4 py-3"><StatusBadge status={getOrderStatus(order).toLowerCase()} /></td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      onClick={(event) => navigateToDetail(order.id, event)}
                      aria-label={`Xem chi tiết đơn hàng ${String(order.id).slice(0, 12)}`}
                      className="p-1.5 rounded hover:bg-surface-container-high focus:outline-none focus-visible:ring-2 focus-visible:ring-tertiary-container"
                    >
                      <Eye size={14} className="text-on-surface-variant" aria-hidden="true" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

    </div>
  )
}
