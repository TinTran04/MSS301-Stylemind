import { useCallback, useEffect, useRef, useState } from 'react'
import { Bell, ShoppingCart, AlertTriangle, Check } from 'lucide-react'
import Badge from '../../components/common/Badge'
import Pagination from '../../components/common/Pagination'
import {
  getMyNotifications,
  getUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '../../features/notifications/notification.api'
import {
  formatNotificationContent,
  formatNotificationStatus,
  formatNotificationTitle,
  isOrderNotification,
} from '../../features/notifications/notification.display'
import { formatDateTime } from '../../utils/formatDate'

const STATUS_VARIANT = {
  SENT: 'success',
  PENDING: 'warning',
  FAILED: 'error',
  SKIPPED: 'default',
}

const READ_FILTERS = [
  { value: 'all', label: 'Tất cả' },
  { value: 'unread', label: 'Chưa đọc' },
  { value: 'read', label: 'Đã đọc' },
]

export default function NotificationsPage() {
  const [pageData, setPageData] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0 })
  const [currentPage, setCurrentPage] = useState(0)
  const [readFilter, setReadFilter] = useState('all')
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const listRequestIdRef = useRef(0)

  const refreshUnreadCount = useCallback(async () => {
    try {
      const response = await getUnreadNotificationCount()
      setUnreadCount(Number(response?.unreadCount ?? response?.count ?? response ?? 0))
    } catch {
      // The list remains usable when the lightweight badge request fails.
    }
  }, [])

  useEffect(() => {
    const requestId = listRequestIdRef.current + 1
    listRequestIdRef.current = requestId
    setLoading(true)
    setError('')
    getMyNotifications({
      page: currentPage,
      size: 10,
      sort: 'createdAt,desc',
      read: readFilter === 'all' ? undefined : readFilter === 'read',
    })
      .then((nextPage) => {
        if (listRequestIdRef.current !== requestId) return
        setPageData(nextPage)
        setCurrentPage(nextPage.page ?? currentPage)
      })
      .catch(() => {
        if (listRequestIdRef.current === requestId) setError('Không thể tải thông báo.')
      })
      .finally(() => {
        if (listRequestIdRef.current === requestId) setLoading(false)
      })
  }, [currentPage, readFilter, refreshKey])

  useEffect(() => {
    refreshUnreadCount()
  }, [refreshUnreadCount, refreshKey])

  useEffect(() => {
    if (pageData.totalPages === 0 && currentPage !== 0) {
      setCurrentPage(0)
    } else if (pageData.totalPages > 0 && currentPage >= pageData.totalPages) {
      setCurrentPage(pageData.totalPages - 1)
    }
  }, [currentPage, pageData.totalPages])

  const handleReadOne = async (id) => {
    setActionLoading(true)
    setError('')
    try {
      await markNotificationRead(id)
      setRefreshKey((value) => value + 1)
    } catch {
      setError('Không thể đánh dấu thông báo đã đọc.')
    } finally {
      setActionLoading(false)
    }
  }

  const handleReadAll = async () => {
    setActionLoading(true)
    setError('')
    try {
      await markAllNotificationsRead()
      setRefreshKey((value) => value + 1)
    } catch {
      setError('Không thể đánh dấu tất cả thông báo đã đọc.')
    } finally {
      setActionLoading(false)
    }
  }

  const hasNotifications = pageData.totalElements > 0
  const emptyMessage = readFilter === 'all' ? 'Bạn chưa có thông báo nào.' : 'Không có thông báo phù hợp với bộ lọc.'

  return (
    <div className="mx-auto max-w-5xl px-5 py-10 sm:px-8 lg:py-14">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-headline-md text-primary">Thông báo</h1>
          <p className="mt-2 text-sm text-on-surface-variant">{unreadCount} thông báo chưa đọc</p>
        </div>
        {unreadCount > 0 && <button type="button" onClick={handleReadAll} disabled={actionLoading} className="inline-flex min-h-10 items-center gap-2 rounded-full border border-primary px-4 py-2 text-sm font-medium text-primary transition-colors hover:bg-primary hover:text-on-primary disabled:opacity-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"><Check size={15} aria-hidden="true" /> Đánh dấu tất cả đã đọc</button>}
      </div>

      <div className="mt-8 flex flex-wrap gap-2 border-b border-outline-variant/20 pb-5" aria-label="Bộ lọc thông báo">
        {READ_FILTERS.map((filter) => <button key={filter.value} type="button" aria-pressed={readFilter === filter.value} onClick={() => { setReadFilter(filter.value); setCurrentPage(0) }} className={`rounded-full px-4 py-2 text-xs font-medium transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${readFilter === filter.value ? 'bg-primary text-on-primary' : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-high'}`}>{filter.label}</button>)}
      </div>

      {error && <div role="alert" className="mt-6 rounded-xl border border-error/20 bg-error-container/30 p-5 text-sm text-error"><p>{error}</p><button type="button" onClick={() => setRefreshKey((value) => value + 1)} className="mt-3 underline">Thử lại</button></div>}
      {loading && pageData.content.length === 0 && <div className="py-20 text-center text-on-surface-variant">Đang tải thông báo...</div>}
      {!loading && !error && !hasNotifications && <div className="py-20 text-center"><Bell size={48} className="mx-auto mb-4 text-on-surface-variant/30" /><p className="text-on-surface-variant">{emptyMessage}</p></div>}

      {!error && (loading || hasNotifications) && <>
        <div className="mt-6 divide-y divide-outline-variant/10 overflow-hidden rounded-2xl border border-outline-variant/15 bg-surface-container-lowest">
          {pageData.content.map((notification) => {
            const Icon = isOrderNotification(notification.type) ? ShoppingCart : AlertTriangle
            const displayContent = formatNotificationContent(notification)
            return (
              <article key={notification.id} className={`flex items-start gap-4 px-5 py-4 ${notification.read ? '' : 'bg-primary/[0.03]'}`}>
                <div className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary/10"><Icon size={16} className="text-primary" aria-hidden="true" /></div>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2"><p className="text-sm font-medium text-on-surface">{formatNotificationTitle(notification)}</p><Badge variant={STATUS_VARIANT[String(notification.status || '').toUpperCase()] || 'default'}>{formatNotificationStatus(notification.status)}</Badge></div>
                  {displayContent && <p className="mt-1 text-xs text-on-surface-variant">{displayContent}</p>}
                  <p className="mt-1 text-xs text-on-surface-variant/60">{formatDateTime(notification.sentAt || notification.createdAt)}</p>
                </div>
                {!notification.read && <button type="button" aria-label="Đánh dấu thông báo đã đọc" onClick={() => handleReadOne(notification.id)} disabled={actionLoading} className="shrink-0 rounded-full p-2 text-on-surface-variant transition-colors hover:bg-surface-container-high disabled:opacity-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"><Check size={15} aria-hidden="true" /></button>}
              </article>
            )
          })}
        </div>
        <Pagination page={currentPage} totalPages={pageData.totalPages} onPageChange={setCurrentPage} label="Phân trang thông báo" />
        {hasNotifications && <p className="pb-3 text-center text-xs text-on-surface-variant">Trang {currentPage + 1} / {pageData.totalPages} · {pageData.totalElements} thông báo</p>}
      </>}
    </div>
  )
}
