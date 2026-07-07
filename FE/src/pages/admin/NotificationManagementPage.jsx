import { useState, useEffect, useCallback } from 'react'
import { Bell, Filter, Search, ShoppingCart, RefreshCw, RotateCcw, AlertTriangle } from 'lucide-react'
import Badge from '../../components/common/Badge'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog'
import { getAdminNotifications, retryAdminNotification } from '../../features/notifications/notification.api'
import { getAdminErrorMessage } from '../../features/admin/admin-error-messages'
import { formatDateTime } from '../../utils/formatDate'

const STATUS_VARIANT = {
  SENT: 'success',
  PENDING: 'warning',
  FAILED: 'error',
  SKIPPED: 'default',
}

const STATUS_FILTERS = [
  { value: 'All', label: 'Tất cả' },
  { value: 'PENDING', label: 'Đang chờ' },
  { value: 'SENT', label: 'Đã gửi' },
  { value: 'FAILED', label: 'Thất bại' },
  { value: 'SKIPPED', label: 'Đã bỏ qua' },
]

const STATUS_LABELS = {
  PENDING: 'Đang chờ',
  SENT: 'Đã gửi',
  FAILED: 'Thất bại',
  SKIPPED: 'Đã bỏ qua',
}

const NOTIFICATION_TITLE_LABELS = {
  'Order confirmed': 'Đơn hàng đã xác nhận',
  SYSTEM: 'Hệ thống',
  'Authentication failed': 'Xác thực thất bại',
  'Set password StyleMind': 'Thiết lập mật khẩu StyleMind',
}

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
  const [listError, setListError] = useState(null)
  const [rowErrors, setRowErrors] = useState({})
  const [confirmDialog, setConfirmDialog] = useState(null)

  const showToast = (message) => {
    setToast(message)
    setTimeout(() => setToast(null), 4000)
  }

  const fetchNotifications = useCallback(async () => {
    setLoading(true)
    setListError(null)
    try {
      const data = await getAdminNotifications(statusFilter === 'All' ? {} : { status: statusFilter })
      setNotifications(data.content || data || [])
    } catch (err) {
      const friendly = getAdminErrorMessage(err, {
        fallbackTitle: 'Không thể tải danh sách thông báo',
        fallbackMessage: 'Hệ thống chưa thể tải danh sách thông báo. Vui lòng làm mới trang hoặc thử lại sau.',
      })
      setListError(friendly)
    } finally {
      setLoading(false)
    }
  }, [statusFilter])

  useEffect(() => {
    fetchNotifications()
  }, [fetchNotifications])

  const requestRetry = (notification) => {
    setRowErrors((prev) => ({ ...prev, [notification.id]: null }))
    setConfirmDialog({
      title: 'Gửi lại thông báo?',
      message: 'Hệ thống sẽ thử gửi lại thông báo thất bại này.',
      confirmLabel: 'Gửi lại',
      onConfirm: () => submitRetry(notification),
    })
  }

  const submitRetry = async (notification) => {
    setRetryingId(notification.id)
    try {
      const updated = await retryAdminNotification(notification.id)
      setNotifications((prev) => prev.map((n) => (n.id === notification.id ? { ...n, ...updated } : n)))
      showToast(`Đã gửi lại thông báo #${notification.id} — trạng thái: ${formatNotificationStatus(updated.status)}`)
    } catch (err) {
      const friendly = getAdminErrorMessage(err, {
        fallbackTitle: 'Gửi lại thông báo thất bại',
        fallbackMessage: 'Hệ thống chưa thể gửi lại thông báo này. Vui lòng thử lại sau.',
      })
      setRowErrors((prev) => ({ ...prev, [notification.id]: friendly }))
    } finally {
      setRetryingId(null)
    }
  }

  const filtered = notifications.filter((n) => {
    if (!search) return true
    const haystack = `${n.title || ''} ${n.content || ''} ${n.recipientEmail || ''}`.toLowerCase()
    return haystack.includes(search.toLowerCase())
  })

  const formatNotificationTitle = (notification) => {
    const raw = notification?.title || notification?.type || ''
    return NOTIFICATION_TITLE_LABELS[raw] || raw
  }

  const formatNotificationStatus = (status) => STATUS_LABELS[String(status || '').toUpperCase()] || status

  return (
    <div className="space-y-6">
      {toast && (
        <div className="fixed top-4 right-4 z-50 bg-surface-container-highest text-on-surface px-4 py-2 rounded-lg shadow-lg text-sm font-medium border border-outline/20">
          {toast}
        </div>
      )}

      <div className="flex items-center justify-between">
        <h1 className="font-headline-md text-primary">Thông báo</h1>
        <button
          onClick={fetchNotifications}
          disabled={loading}
          className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-high transition-colors disabled:opacity-40"
        >
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} /> Làm mới
        </button>
      </div>

      {listError && (
        <div className="rounded-lg border border-error/20 bg-error-container/40 px-4 py-3 text-sm text-error">
          <p className="font-medium">{listError.title}</p>
          <p className="mt-0.5">{listError.message}</p>
        </div>
      )}

      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Tìm thông báo..."
            className="w-full pl-9 pr-4 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none focus:ring-1 focus:ring-tertiary-container"
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter size={16} className="text-on-surface-variant shrink-0" />
          <div className="flex gap-1 flex-wrap">
            {STATUS_FILTERS.map((f) => (
              <button
                key={f.value}
                onClick={() => setStatusFilter(f.value)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                  statusFilter === f.value
                    ? 'bg-primary text-on-primary'
                    : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-high'
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="bg-surface-container-lowest rounded-xl ambient-shadow divide-y divide-outline-variant/10">
        {loading && filtered.length === 0 ? (
          <div className="py-16 text-center text-sm text-on-surface-variant">Đang tải thông báo...</div>
        ) : filtered.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-on-surface-variant">
            <Bell size={40} className="mb-3 opacity-30" />
            <p className="text-sm">Không tìm thấy thông báo nào.</p>
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
                    <p className="text-sm font-medium text-on-surface">{formatNotificationTitle(n)}</p>
                    <Badge variant={STATUS_VARIANT[n.status] || 'default'}>{formatNotificationStatus(n.status)}</Badge>
                  </div>
                  <p className="text-xs text-on-surface-variant mt-0.5 line-clamp-1">
                    {n.recipientEmail} — {n.content}
                  </p>
                  {n.errorMessage && (
                    <p className="text-xs text-error mt-1">{n.errorMessage}</p>
                  )}
                  {rowErrors[n.id] && (
                    <div className="mt-2 rounded-lg border border-error/20 bg-error-container/40 px-3 py-2 text-xs text-error">
                      <p className="font-medium">{rowErrors[n.id].title}</p>
                      <p className="mt-0.5">{rowErrors[n.id].message}</p>
                    </div>
                  )}
                  <p className="text-xs text-on-surface-variant/60 mt-1">{formatDateTime(n.createdAt)}</p>
                </div>
                {n.status === 'FAILED' && (
                  <button
                    onClick={() => requestRetry(n)}
                    disabled={retryingId === n.id}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-primary hover:bg-primary/10 transition-colors disabled:opacity-40 shrink-0"
                  >
                    <RotateCcw size={12} className={retryingId === n.id ? 'animate-spin' : ''} /> Gửi lại
                  </button>
                )}
              </div>
            )
          })
        )}
      </div>

      <AdminConfirmDialog
        open={!!confirmDialog}
        title={confirmDialog?.title}
        message={confirmDialog?.message}
        confirmLabel={confirmDialog?.confirmLabel}
        loading={retryingId !== null}
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
