import { useState } from 'react'
import { Bell, CheckCheck, Trash2, Filter, Search, ShoppingCart, User, Package, AlertTriangle } from 'lucide-react'

const TYPE_CONFIG = {
  order:   { icon: ShoppingCart, color: 'text-blue-500',   bg: 'bg-blue-500/10'   },
  user:    { icon: User,         color: 'text-purple-500', bg: 'bg-purple-500/10' },
  product: { icon: Package,      color: 'text-green-500',  bg: 'bg-green-500/10'  },
  system:  { icon: AlertTriangle,color: 'text-amber-500',  bg: 'bg-amber-500/10'  },
}

const MOCK_NOTIFICATIONS = [
  { id: '1', type: 'order',   title: 'New order placed',            body: 'Order #ORD-2026-001 from khoiminh@gmail.com — ₫1,250,000',  read: false, time: '2 min ago' },
  { id: '2', type: 'user',    title: 'New user registered',         body: 'test@example.com just created an account.',                  read: false, time: '15 min ago' },
  { id: '3', type: 'product', title: 'Low stock alert',             body: 'Áo len cashmere (S/Trắng) — only 2 units left.',             read: false, time: '1 hour ago' },
  { id: '4', type: 'order',   title: 'Order payment failed',        body: 'Order #ORD-2026-002 payment declined — gateway timeout.',    read: true,  time: '3 hours ago' },
  { id: '5', type: 'system',  title: 'Auth service restarted',      body: 'auth-service restarted after health check failure.',         read: true,  time: '5 hours ago' },
  { id: '6', type: 'product', title: 'Product published',           body: 'Váy lụa tơ tằm (Đen) is now live in the catalog.',          read: true,  time: 'Yesterday' },
  { id: '7', type: 'order',   title: 'Order fulfilled',             body: 'Order #ORD-2025-987 marked as FULFILLED.',                  read: true,  time: 'Yesterday' },
  { id: '8', type: 'user',    title: 'User role changed',           body: 'admin2@stylemind.ai promoted to ADMIN.',                    read: true,  time: '2 days ago' },
]

const FILTERS = ['All', 'Unread', 'order', 'user', 'product', 'system']

export default function NotificationManagementPage() {
  const [notifications, setNotifications] = useState(MOCK_NOTIFICATIONS)
  const [activeFilter, setActiveFilter] = useState('All')
  const [search, setSearch] = useState('')

  const unreadCount = notifications.filter((n) => !n.read).length

  const filtered = notifications.filter((n) => {
    const matchFilter =
      activeFilter === 'All' ? true :
      activeFilter === 'Unread' ? !n.read :
      n.type === activeFilter
    const matchSearch =
      !search ||
      n.title.toLowerCase().includes(search.toLowerCase()) ||
      n.body.toLowerCase().includes(search.toLowerCase())
    return matchFilter && matchSearch
  })

  const markAllRead = () =>
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))

  const markRead = (id) =>
    setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)))

  const deleteOne = (id) =>
    setNotifications((prev) => prev.filter((n) => n.id !== id))

  const deleteAll = () =>
    setNotifications((prev) => prev.filter((n) => !n.read))

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-headline-md text-primary">Notifications</h1>
          {unreadCount > 0 && (
            <p className="text-sm text-on-surface-variant mt-1">{unreadCount} unread</p>
          )}
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={markAllRead}
            disabled={unreadCount === 0}
            className="flex items-center gap-2 px-4 py-2 text-sm text-on-surface-variant hover:bg-surface-container-high rounded-lg transition-colors disabled:opacity-40"
          >
            <CheckCheck size={16} />
            Mark all read
          </button>
          <button
            onClick={deleteAll}
            disabled={notifications.every((n) => !n.read)}
            className="flex items-center gap-2 px-4 py-2 text-sm text-error hover:bg-error-container/20 rounded-lg transition-colors disabled:opacity-40"
          >
            <Trash2 size={16} />
            Clear read
          </button>
        </div>
      </div>

      {/* Search + Filter */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search notifications…"
            className="w-full pl-9 pr-4 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none focus:ring-1 focus:ring-tertiary-container"
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter size={16} className="text-on-surface-variant shrink-0" />
          <div className="flex gap-1 flex-wrap">
            {FILTERS.map((f) => (
              <button
                key={f}
                onClick={() => setActiveFilter(f)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium capitalize transition-colors ${
                  activeFilter === f
                    ? 'bg-primary text-on-primary'
                    : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-high'
                }`}
              >
                {f}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Notification list */}
      <div className="bg-surface-container-lowest rounded-xl ambient-shadow divide-y divide-outline-variant/10">
        {filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-on-surface-variant">
            <Bell size={40} className="mb-3 opacity-30" />
            <p className="text-sm">No notifications found</p>
          </div>
        ) : (
          filtered.map((n) => {
            const cfg = TYPE_CONFIG[n.type] || TYPE_CONFIG.system
            const Icon = cfg.icon
            return (
              <div
                key={n.id}
                className={`flex items-start gap-4 px-5 py-4 transition-colors hover:bg-surface-container-low ${
                  !n.read ? 'bg-primary/[0.03]' : ''
                }`}
              >
                <div className={`mt-0.5 w-9 h-9 rounded-full flex items-center justify-center shrink-0 ${cfg.bg}`}>
                  <Icon size={16} className={cfg.color} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className={`text-sm font-medium text-on-surface ${!n.read ? 'font-semibold' : ''}`}>
                      {n.title}
                    </p>
                    {!n.read && (
                      <span className="w-1.5 h-1.5 rounded-full bg-primary shrink-0" />
                    )}
                  </div>
                  <p className="text-xs text-on-surface-variant mt-0.5 line-clamp-1">{n.body}</p>
                  <p className="text-xs text-on-surface-variant/60 mt-1">{n.time}</p>
                </div>
                <div className="flex items-center gap-1 shrink-0">
                  {!n.read && (
                    <button
                      onClick={() => markRead(n.id)}
                      className="p-1.5 rounded-lg hover:bg-surface-container-high transition-colors"
                      title="Mark as read"
                    >
                      <CheckCheck size={14} className="text-on-surface-variant" />
                    </button>
                  )}
                  <button
                    onClick={() => deleteOne(n.id)}
                    className="p-1.5 rounded-lg hover:bg-error-container/20 transition-colors"
                    title="Delete"
                  >
                    <Trash2 size={14} className="text-error" />
                  </button>
                </div>
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}
