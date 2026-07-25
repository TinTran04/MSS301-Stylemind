const VIETNAM_TIME_ZONE = 'Asia/Ho_Chi_Minh'
const BACKEND_LOCAL_DATE_TIME_RE = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?$/

export function parseBackendDate(dateStr) {
  if (!dateStr) return null
  if (dateStr instanceof Date) return dateStr

  const raw = String(dateStr).trim()
  const normalized = BACKEND_LOCAL_DATE_TIME_RE.test(raw) ? `${raw}Z` : raw
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatDate(dateStr) {
  const date = parseBackendDate(dateStr)
  if (!date) return String(dateStr || '')
  return date.toLocaleDateString('en-US', {
    timeZone: VIETNAM_TIME_ZONE,
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

export function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const date = parseBackendDate(dateStr)
  if (!date) return String(dateStr)
  return date.toLocaleString('vi-VN', {
    timeZone: VIETNAM_TIME_ZONE,
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour12: false,
  })
}

export function formatRelativeTime(dateStr) {
  const date = parseBackendDate(dateStr)
  if (!date) return ''
  const now = new Date()
  const diffMs = now - date
  const diffMins = Math.floor(diffMs / 60000)
  const diffHrs = Math.floor(diffMins / 60)
  const diffDays = Math.floor(diffHrs / 24)

  if (diffMins < 1) return 'Just now'
  if (diffMins < 60) return `${diffMins}m ago`
  if (diffHrs < 24) return `${diffHrs}h ago`
  if (diffDays < 7) return `${diffDays}d ago`
  return formatDate(dateStr)
}
