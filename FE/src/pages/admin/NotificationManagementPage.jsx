import { useState, useEffect, useCallback } from 'react'
import { Bell, Filter, Search, ShoppingCart, RefreshCw, RotateCcw, AlertTriangle } from 'lucide-react'
import Badge from '../../components/common/Badge'
import { getAdminNotifications, retryAdminNotification } from '../../features/notifications/notification.api'
import { formatDateTime } from '../../utils/formatDate'

const STATUS_VARIANT = {
  SENT: 'success',
  PENDING: 'warning',
  FAILED: 'error',
  SKIPPED: 'default',
}

const STATUS_FILTERS = ['All', 'PENDING', 'SENT', 'FAILED', 'SKIPPED']

function typeIcon(type) {
  return String(type || '').toUpperCase().startsWith('ORDER') ? ShoppingCart : AlertTriangle
}

export default function NotificationManagementPage() {
  const [notifications, setNotifications] = useState([])
  const [statusFilter, setStatusFilter] = useState('All')
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [retryingId, setRetryingId] = useState(null)
  const [toast, setToast] = useState(null)

  const showToast = (message) => {
    setToast(message)
    setTimeout(() => setToast(null), 4000)
  }

  const fetchNotifications = useCallback(async () => {
    setLoading(true)
    try {
      const data = await getAdminNotifications(statusFilter === 'All' ? {} : { status: statusFilter })
      setNotifications(data.content || data || [])
    } catch (err) {
      showToast(err.message || 'Error loading notifications')
    } finally {
      setLoading(false)
    }
  }, [statusFilter])

  useEffect(() => {
    fetchNotifications()
  }, [fetchNotifications])

  const handleRetry = async (notification) => {
    setRetryingId(notification.id)
    try {
      const updated = await retryAdminNotification(notification.id)
      setNotifications((prev) => prev.map((n) => (n.id === notification.id ? { ...n, ...updated } : n)))
      showToast(`Notification #${notification.id} resent — status: ${updated.status}`)
    } catch (err) {
      showToast(err.message || 'Failed to retry notification')
    } finally {
      setRetryingId(null)
    }
  }

  const filtered = notifications.filter((n) => {
    if (!search) return true
    const haystack = `${n.title || ''} ${n.content || ''} ${n.recipientEmail || ''}`.toLowerCase()
    return haystack.includes(search.toLowerCase())
  })

  return (
    <div className="space-y-6">
      {toast && (
        <div className="fixed top-4 right-4 z-50 bg-surface-container-highest text-on-surface px-4 py-2 rounded-lg shadow-lg text-sm font-medium border border-outline/20">
          {toast}
        </div>
      )}

      <div className="flex items-center justify-between">
        <h1 className="font-headline-md text-primary">Notifications</h1>
        <button
          onClick={fetchNotifications}
          disabled={loading}
          className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-high transition-colors disabled:opacity-40"
        >
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} /> Refresh
        </button>
      </div>

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
            {STATUS_FILTERS.map((f) => (
              <button
                key={f}
                onClick={() => setStatusFilter(f)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  statusFilter === f
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

      <div className="bg-surface-container-lowest rounded-xl ambient-shadow divide-y divide-outline-variant/10">
        {loading && filtered.length === 0 ? (
          <div className="py-16 text-center text-sm text-on-surface-variant">Loading notifications...</div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-on-surface-variant">
            <Bell size={40} className="mb-3 opacity-30" />
            <p className="text-sm">No notifications found</p>
          </div>
        ) : (
          filtered.map((n) => {
            const Icon = typeIcon(n.type)
            return (
              <div key={n.id} className="flex items-start gap-4 px-5 py-4 hover:bg-surface-container-low transition-colors">
                <div className="mt-0.5 w-9 h-9 rounded-full flex items-center justify-center shrink-0 bg-blue-500/10">
                  <Icon size={16} className="text-blue-500" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-medium text-on-surface">{n.title || n.type}</p>
                    <Badge variant={STATUS_VARIANT[n.status] || 'default'}>{n.status}</Badge>
                  </div>
                  <p className="text-xs text-on-surface-variant mt-0.5 line-clamp-1">
                    {n.recipientEmail} — {n.content}
                  </p>
                  {n.errorMessage && (
                    <p className="text-xs text-error mt-1">{n.errorMessage}</p>
                  )}
                  <p className="text-xs text-on-surface-variant/60 mt-1">{formatDateTime(n.createdAt)}</p>
                </div>
                {n.status === 'FAILED' && (
                  <button
                    onClick={() => handleRetry(n)}
                    disabled={retryingId === n.id}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-primary hover:bg-primary/10 transition-colors disabled:opacity-40 shrink-0"
                  >
                    <RotateCcw size={12} className={retryingId === n.id ? 'animate-spin' : ''} /> Retry
                  </button>
                )}
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}
