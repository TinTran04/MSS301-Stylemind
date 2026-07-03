import { useState, useEffect } from 'react'
import { Bell, ShoppingCart, AlertTriangle } from 'lucide-react'
import Badge from '../../components/common/Badge'
import { getMyNotifications } from '../../features/notifications/notification.api'
import { formatDateTime } from '../../utils/formatDate'

const STATUS_VARIANT = {
  SENT: 'success',
  PENDING: 'warning',
  FAILED: 'error',
  SKIPPED: 'default',
}

const TYPE_ICON = {
  ORDER: ShoppingCart,
  ORDER_CONFIRMATION: ShoppingCart,
}

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getMyNotifications()
      .then(setNotifications)
      .catch((err) => setError(err.message || 'Unable to load notifications.'))
      .finally(() => setLoading(false))
  }, [])

  const sorted = [...notifications].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))

  return (
    <div className="max-w-[900px] mx-auto px-6 md:px-16 py-8">
      <h1 className="font-headline-md text-primary mb-8">Notifications</h1>

      {loading && <div className="py-20 text-center text-on-surface-variant">Loading notifications...</div>}
      {error && (
        <div role="alert" className="rounded-xl border border-error/20 bg-error-container/30 p-6 text-sm text-error">
          {error}
        </div>
      )}
      {!loading && !error && sorted.length === 0 && (
        <div className="py-20 text-center">
          <Bell size={48} className="text-on-surface-variant/30 mx-auto mb-4" />
          <p className="text-on-surface-variant">You do not have any notifications yet.</p>
        </div>
      )}

      {!loading && !error && sorted.length > 0 && (
        <div className="bg-surface-container-lowest rounded-xl ambient-shadow divide-y divide-outline-variant/10">
          {sorted.map((n) => {
            const Icon = TYPE_ICON[n.type] || AlertTriangle
            return (
              <div key={n.id} className="flex items-start gap-4 px-5 py-4">
                <div className="mt-0.5 w-9 h-9 rounded-full flex items-center justify-center shrink-0 bg-primary/10">
                  <Icon size={16} className="text-primary" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-medium text-on-surface">{n.title || n.type}</p>
                    <Badge variant={STATUS_VARIANT[n.status] || 'default'}>{n.status}</Badge>
                  </div>
                  {n.content && <p className="text-xs text-on-surface-variant mt-0.5">{n.content}</p>}
                  <p className="text-xs text-on-surface-variant/60 mt-1">{formatDateTime(n.createdAt)}</p>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
