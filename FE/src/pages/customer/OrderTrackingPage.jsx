import { useState, useEffect } from 'react'
import { Package, Download, Truck } from 'lucide-react'
import Badge from '../../components/common/Badge'
import { getOrders } from '../../features/orders/order.api'
import { formatDate } from '../../utils/formatDate'
import { formatCurrency } from '../../utils/formatCurrency'

const statusTabs = ['All', 'Processing', 'Shipped', 'Delivered']

export default function OrderTrackingPage() {
  const [orders, setOrders] = useState([])
  const [selectedTab, setSelectedTab] = useState('All')
  const [selectedOrder, setSelectedOrder] = useState(null)

  useEffect(() => {
    getOrders().then((o) => {
      setOrders(o)
      if (o.length > 0) setSelectedOrder(o[0])
    })
  }, [])

  const filteredOrders = selectedTab === 'All'
    ? orders
    : orders.filter((o) => o.status === selectedTab.toLowerCase())

  const timelineSteps = [
    { label: 'Pending', status: 'pending' },
    { label: 'Confirmed', status: 'confirmed' },
    { label: 'Processing', status: 'processing' },
    { label: 'Shipped', status: 'shipped' },
    { label: 'Delivered', status: 'delivered' },
  ]

  const getCurrentStepIndex = (order) => {
    return order.timeline.findIndex((t) => !t.completed)
  }

  return (
    <div className="max-w-[1440px] mx-auto px-6 md:px-16 py-8">
      <h1 className="font-headline-md text-primary mb-8">Order Tracking</h1>

      <div className="flex gap-6">
        {/* Left: Order List */}
        <div className="w-full lg:w-5/12 space-y-4">
          {/* Filter Tabs */}
          <div className="flex gap-2">
            {statusTabs.map((tab) => (
              <button
                key={tab}
                onClick={() => setSelectedTab(tab)}
                className={`px-4 py-2 rounded-full text-xs font-medium transition-all ${
                  selectedTab === tab
                    ? 'bg-primary text-on-primary'
                    : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-high'
                }`}
              >
                {tab}
              </button>
            ))}
          </div>

          {/* Order Cards */}
          <div className="space-y-3">
            {filteredOrders.map((order) => (
              <button
                key={order.id}
                onClick={() => setSelectedOrder(order)}
                className={`w-full text-left bg-surface-container-lowest rounded-xl p-4 transition-all border-l-4 ${
                  selectedOrder?.id === order.id
                    ? 'border-primary ambient-shadow'
                    : 'border-transparent hover:bg-surface-container-low'
                }`}
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-primary">{order.id}</span>
                  <Badge variant={order.status === 'delivered' ? 'success' : order.status === 'shipped' ? 'warning' : 'secondary'}>
                    {order.status}
                  </Badge>
                </div>
                <p className="text-xs text-on-surface-variant">{formatDate(order.date)}</p>
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
            <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow sticky top-28">
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h2 className="font-title-lg text-primary">{selectedOrder.id}</h2>
                  <p className="text-xs text-on-surface-variant mt-1">Placed on {formatDate(selectedOrder.date)}</p>
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
                  {selectedOrder.timeline.map((step, idx) => {
                    const stepIdx = timelineSteps.findIndex((s) => s.status === step.status)
                    return (
                      <div key={idx} className="relative flex flex-col items-center z-10">
                        <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-medium ${
                          step.completed ? 'bg-primary text-on-primary' : 'bg-surface-container-lowest border-2 border-outline-variant text-on-surface-variant'
                        }`}>
                          {step.completed ? '✓' : idx + 1}
                        </div>
                        <span className="mt-2 text-[10px] text-on-surface-variant whitespace-nowrap">{step.label}</span>
                      </div>
                    )
                  })}
                </div>
              </div>

              {/* Items */}
              <h3 className="font-label-sm uppercase tracking-wider text-on-surface-variant mb-3">Package Contents</h3>
              <div className="space-y-3 mb-6">
                {selectedOrder.items.map((item, idx) => (
                  <div key={idx} className="flex gap-3 p-3 bg-surface-container-low rounded-lg">
                    <img src={item.image} alt={item.name} className="w-16 h-20 object-cover rounded-lg" />
                    <div>
                      <p className="text-sm font-medium text-primary">{item.name}</p>
                      <p className="text-xs text-on-surface-variant">Size: {item.size} / {item.color}</p>
                      <p className="text-sm font-semibold text-primary mt-1">{formatCurrency(item.price)}</p>
                    </div>
                  </div>
                ))}
              </div>

              {/* Summary */}
              <div className="border-t border-outline-variant/20 pt-4">
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-on-surface-variant">Total</span>
                  <span className="font-semibold text-primary">{formatCurrency(selectedOrder.total)}</span>
                </div>
              </div>

              <button className="w-full mt-4 bg-surface-container text-on-surface rounded-lg py-2.5 text-sm font-medium hover:bg-surface-container-high transition-colors flex items-center justify-center gap-2">
                <Download size={14} /> Download Invoice
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
