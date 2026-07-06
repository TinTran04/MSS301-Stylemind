import { useState, useEffect, useCallback } from 'react'
import { ShoppingCart, TrendingDown, Clock, DollarSign, Eye, RefreshCw, Filter, Search } from 'lucide-react'
import MetricCard from '../../components/admin/MetricCard'
import StatusBadge from '../../components/admin/StatusBadge'
import Drawer from '../../components/common/Drawer'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog'
import { getAdminOrders, updateAdminOrderStatus } from '../../features/orders/admin-order.api'
import { getAvailableTransitions, formatStatusLabel, ORDER_STATUS_TRANSITIONS } from '../../features/orders/orderStatus'
import { getAdminErrorMessage } from '../../features/admin/admin-error-messages'
import { formatCurrency } from '../../utils/formatCurrency'
import { formatDate } from '../../utils/formatDate'

const STATUS_OPTIONS = ['All', ...Object.keys(ORDER_STATUS_TRANSITIONS)]

const ORDER_KNOWN_ERROR_CODES = {
  INVALID_ORDER_STATUS_TRANSITION: {
    title: 'Không thể cập nhật trạng thái đơn hàng',
    message: 'Trạng thái đơn hàng không thể chuyển theo hướng này. Vui lòng chọn trạng thái hợp lệ.',
  },
}

export default function OrderManagementPage() {
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [updating, setUpdating] = useState(false)
  const [toast, setToast] = useState(null)
  const [listError, setListError] = useState(null)
  const [statusActionError, setStatusActionError] = useState(null)
  const [confirmDialog, setConfirmDialog] = useState(null)
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

  const showToast = (message) => {
    setToast(message)
    setTimeout(() => setToast(null), 4000)
  }

  const getOrderStatus = (order) => String(order?.orderStatus || 'PENDING').toUpperCase()

  const requestStatusChange = (order, newStatus) => {
    if (!newStatus) return
    setStatusActionError(null)
    setConfirmDialog({
      title: 'Cập nhật trạng thái đơn hàng?',
      message: 'Bạn có chắc chắn muốn chuyển đơn hàng sang trạng thái đã chọn không?',
      confirmLabel: 'Cập nhật trạng thái',
      onConfirm: () => submitStatusChange(order, newStatus),
    })
  }

  const submitStatusChange = async (order, newStatus) => {
    setUpdating(true)
    try {
      const updated = await updateAdminOrderStatus(order.id, { orderStatus: newStatus })
      showToast(`Đã chuyển đơn hàng ${order.id} sang trạng thái ${formatStatusLabel(newStatus)}`)
      setOrders((prev) => prev.map((o) => (o.id === order.id ? { ...o, ...updated } : o)))
      setSelectedOrder((prev) => (prev && prev.id === order.id ? { ...prev, ...updated } : prev))
    } catch (err) {
      // Keep the drawer open and show the error near the status control —
      // do not close the drawer or drop the selected order on a recoverable error.
      const friendly = getAdminErrorMessage(err, {
        knownCodes: ORDER_KNOWN_ERROR_CODES,
        fallbackTitle: 'Cập nhật đơn hàng thất bại',
        fallbackMessage: 'Hệ thống chưa thể cập nhật đơn hàng. Vui lòng thử lại sau.',
      })
      setStatusActionError(friendly)
    } finally {
      setUpdating(false)
    }
  }

  return (
    <div className="space-y-6">
      {toast && (
        <div className="fixed top-4 right-4 z-50 bg-surface-container-highest text-on-surface px-4 py-2 rounded-lg shadow-lg text-sm font-medium border border-outline/20">
          {toast}
        </div>
      )}

      <div className="flex items-center justify-between">
        <h1 className="font-headline-md text-primary">Order Management</h1>
        <button onClick={fetchOrders} disabled={loading} className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-high transition-colors disabled:opacity-40">
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} /> Refresh
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
                {status === 'All' ? 'All statuses' : formatStatusLabel(status)}
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
          <span className="text-xs text-on-surface-variant">to</span>
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
            placeholder="Search by customer/user ID…"
            className="w-full pl-8 pr-4 py-1.5 bg-surface-container-low rounded-lg text-xs border border-outline-variant/20 outline-none focus:border-tertiary-container"
          />
        </form>
        {customerFilter && (
          <button
            onClick={() => { setCustomerInput(''); setCustomerFilter('') }}
            className="text-xs text-on-surface-variant hover:text-primary underline"
          >
            Clear customer filter
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard title="Transactions" value={orders.length} change={0} icon={ShoppingCart} />
        <MetricCard title="Failure Rate" value="0%" change={0} icon={TrendingDown} status="good" />
        <MetricCard title="Avg Processing" value="-" change={0} icon={Clock} status="good" />
        <MetricCard title="Revenue" value={formatCurrency(orders.reduce((sum, o) => sum + (o.totalAmount || o.total || 0), 0))} change={0} icon={DollarSign} />
      </div>

      <div className="bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-surface-container-low/50">
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Order ID</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Customer</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Date</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Total</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Status</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/5">
              {loading && orders.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-sm text-on-surface-variant">Loading orders...</td>
                </tr>
              ) : orders.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-sm text-on-surface-variant">No orders found.</td>
                </tr>
              ) : orders.map((order) => (
                <tr key={order.id} className="hover:bg-surface-container-high/30 cursor-pointer" onClick={() => { setStatusActionError(null); setSelectedOrder(order) }}>
                  <td className="px-4 py-3 text-sm font-medium text-primary">{order.id}</td>
                  <td className="px-4 py-3 text-sm text-on-surface">{order.customerName || order.userId || 'Guest'}</td>
                  <td className="px-4 py-3 text-sm text-on-surface-variant">{formatDate(order.createdAt || order.date)}</td>
                  <td className="px-4 py-3 text-sm text-primary font-medium">{formatCurrency(order.totalAmount || order.total || 0)}</td>
                  <td className="px-4 py-3"><StatusBadge status={getOrderStatus(order).toLowerCase()} /></td>
                  <td className="px-4 py-3"><button className="p-1.5 rounded hover:bg-surface-container-high"><Eye size={14} className="text-on-surface-variant" /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Drawer isOpen={!!selectedOrder} onClose={() => setSelectedOrder(null)} title="Order Details">
        {selectedOrder && (
          <div className="space-y-6">
            <div>
              <h3 className="font-title-lg text-primary">{selectedOrder.id}</h3>
              <p className="text-sm text-on-surface-variant">{selectedOrder.customerName || selectedOrder.userId || 'Guest'} &middot; {formatDate(selectedOrder.createdAt || selectedOrder.date)}</p>
              <p className="text-lg font-semibold text-primary mt-2">{formatCurrency(selectedOrder.totalAmount || selectedOrder.total || 0)}</p>
            </div>

            <div className="border-t border-outline-variant/20 pt-4">
              <h4 className="font-label-sm uppercase text-on-surface-variant mb-3">Current Status</h4>
              <StatusBadge status={getOrderStatus(selectedOrder).toLowerCase()} />
            </div>

            <div>
              <h4 className="font-label-sm uppercase text-on-surface-variant mb-3">Change Status</h4>
              {statusActionError && (
                <div className="mb-3 rounded-lg border border-error/20 bg-error-container/40 px-3 py-2 text-xs text-error">
                  <p className="font-medium">{statusActionError.title}</p>
                  <p className="mt-0.5">{statusActionError.message}</p>
                </div>
              )}
              {(() => {
                const nextOptions = getAvailableTransitions(selectedOrder)
                if (nextOptions.length === 0) {
                  return (
                    <p className="text-sm text-on-surface-variant italic">
                      This order is in a terminal state — no further transitions are allowed.
                    </p>
                  )
                }
                return (
                  <select
                    disabled={updating}
                    value=""
                    onChange={(e) => requestStatusChange(selectedOrder, e.target.value)}
                    className="w-full bg-surface-container-low border border-outline-variant/20 rounded-lg px-3 py-2 text-sm text-on-surface focus:outline-none focus:border-tertiary-container disabled:opacity-50"
                  >
                    <option value="" disabled>Select next status&hellip;</option>
                    {nextOptions.map((status) => (
                      <option key={status} value={status}>{formatStatusLabel(status)}</option>
                    ))}
                  </select>
                )
              })()}
            </div>
          </div>
        )}
      </Drawer>

      <AdminConfirmDialog
        open={!!confirmDialog}
        title={confirmDialog?.title}
        message={confirmDialog?.message}
        confirmLabel={confirmDialog?.confirmLabel}
        loading={updating}
        onConfirm={async () => {
          const dialog = confirmDialog
          setConfirmDialog(null)
          if (dialog) await dialog.onConfirm()
        }}
        onCancel={() => setConfirmDialog(null)}
      />
    </div>
  )
}
