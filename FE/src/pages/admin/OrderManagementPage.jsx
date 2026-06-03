import { useState } from 'react'
import { ShoppingCart, TrendingDown, Clock, DollarSign, Eye } from 'lucide-react'
import MetricCard from '../../components/admin/MetricCard'
import StatusBadge from '../../components/admin/StatusBadge'
import Drawer from '../../components/common/Drawer'
import { mockAnalytics } from '../../data/mockAnalytics'
import { formatCurrency } from '../../utils/formatCurrency'
import { formatDate } from '../../utils/formatDate'

export default function OrderManagementPage() {
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [orders, setOrders] = useState(mockAnalytics.adminOrders)
  const [toast, setToast] = useState(null)

  const showToast = (message) => {
    setToast(message)
    setTimeout(() => setToast(null), 2000)
  }

  const sagaSteps = ['Order Created', 'Stock Reserved', 'Payment Processing', 'Completed']

  const getSagaIndex = (state) => {
    const map = { completed: 3, processing: 2, stock_reserved: 1, payment_pending: 0.5, pending: 0, confirmed: 0.5, shipped: 2.5, delivered: 3 }
    return map[state] ?? 0
  }

  const statusFlow = ['pending', 'confirmed', 'processing', 'shipped', 'delivered']

  const advanceStatus = (orderId, action) => {
    setOrders(prev => prev.map(order => {
      if (order.id === orderId) {
        const currentIdx = statusFlow.indexOf(order.status)
        const nextIdx = Math.min(currentIdx + 1, statusFlow.length - 1)
        const newStatus = statusFlow[nextIdx]
        const newState = newStatus === 'delivered' ? 'completed' : newStatus === 'shipped' ? 'processing' : order.sagaState
        return { ...order, status: newStatus, sagaState: newState }
      }
      return order
    }))
    setSelectedOrder(prev => {
      if (prev && prev.id === orderId) {
        const currentIdx = statusFlow.indexOf(prev.status)
        const nextIdx = Math.min(currentIdx + 1, statusFlow.length - 1)
        const newStatus = statusFlow[nextIdx]
        const newState = newStatus === 'delivered' ? 'completed' : newStatus === 'shipped' ? 'processing' : prev.sagaState
        return { ...prev, status: newStatus, sagaState: newState }
      }
      return prev
    })
    showToast(`Order ${action}`)
  }

  return (
    <div className="space-y-6">
      {toast && (
        <div className="fixed top-4 right-4 z-50 bg-green-status text-white px-4 py-2 rounded-lg shadow-lg text-sm font-medium animate-pulse">
          {toast}
        </div>
      )}

      <h1 className="font-headline-md text-primary">Order Management</h1>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard title="Transactions" value="847" change={12.3} icon={ShoppingCart} />
        <MetricCard title="Failure Rate" value="2.1%" change={-15.4} icon={TrendingDown} status="good" />
        <MetricCard title="Avg Processing" value="3.2min" change={-8.2} icon={Clock} status="good" />
        <MetricCard title="Revenue" value={formatCurrency(128450)} change={14.4} icon={DollarSign} />
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
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Saga State</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/5">
              {orders.map((order) => (
                <tr key={order.id} className="hover:bg-surface-container-high/30 cursor-pointer" onClick={() => setSelectedOrder(order)}>
                  <td className="px-4 py-3 text-sm font-medium text-primary">{order.id}</td>
                  <td className="px-4 py-3 text-sm text-on-surface">{order.customer}</td>
                  <td className="px-4 py-3 text-sm text-on-surface-variant">{formatDate(order.date)}</td>
                  <td className="px-4 py-3 text-sm text-primary font-medium">{formatCurrency(order.total)}</td>
                  <td className="px-4 py-3"><StatusBadge status={order.status} /></td>
                  <td className="px-4 py-3"><StatusBadge status={order.sagaState} /></td>
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
              <p className="text-sm text-on-surface-variant">{selectedOrder.customer} &middot; {formatDate(selectedOrder.date)}</p>
              <p className="text-lg font-semibold text-primary mt-2">{formatCurrency(selectedOrder.total)}</p>
            </div>

            <div>
              <h4 className="font-label-sm uppercase text-on-surface-variant mb-3">Saga Timeline</h4>
              <div className="space-y-3">
                {sagaSteps.map((step, idx) => {
                  const completed = idx <= getSagaIndex(selectedOrder.sagaState)
                  return (
                    <div key={idx} className="flex items-center gap-3">
                      <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold ${
                        completed ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant'
                      }`}>
                        {completed ? '✓' : idx + 1}
                      </div>
                      <span className={`text-sm ${completed ? 'text-primary' : 'text-on-surface-variant'}`}>{step}</span>
                    </div>
                  )
                })}
              </div>
            </div>

            <div className="border-t border-outline-variant/20 pt-4">
              <StatusBadge status={selectedOrder.sagaState} />
            </div>

            <div className="flex gap-2 pt-2">
              {selectedOrder.status === 'pending' && (
                <button onClick={() => advanceStatus(selectedOrder.id, 'confirmed')} className="flex-1 bg-primary text-on-primary rounded-lg py-2 text-xs font-medium hover:opacity-90">Confirm</button>
              )}
              {selectedOrder.status === 'confirmed' && (
                <button onClick={() => advanceStatus(selectedOrder.id, 'shipped')} className="flex-1 bg-primary text-on-primary rounded-lg py-2 text-xs font-medium hover:opacity-90">Ship</button>
              )}
              {(selectedOrder.status === 'processing' || selectedOrder.status === 'shipped') && (
                <button onClick={() => advanceStatus(selectedOrder.id, 'delivered')} className="flex-1 bg-primary text-on-primary rounded-lg py-2 text-xs font-medium hover:opacity-90">Complete</button>
              )}
              {selectedOrder.status === 'delivered' && (
                <span className="text-sm text-green-status font-medium">Order completed</span>
              )}
            </div>
          </div>
        )}
      </Drawer>
    </div>
  )
}
