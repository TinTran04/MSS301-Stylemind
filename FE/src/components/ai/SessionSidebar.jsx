import { useState } from 'react'
import clsx from 'clsx'
import { MessageSquare, Plus, Trash2, Loader2 } from 'lucide-react'
import { formatRelativeTime } from '../../features/ai-stylist/aiStylist.utils'

// Sidebar listing the user's chat sessions, matching the page's glass/surface style.
export default function SessionSidebar({
  sessions,
  activeSessionId,
  loading,
  onSelect,
  onNewChat,
  onDelete,
}) {
  const [deletingId, setDeletingId] = useState(null)

  const handleDelete = async (e, sessionId) => {
    e.stopPropagation()
    setDeletingId(sessionId)
    try {
      await onDelete(sessionId)
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <aside className="hidden md:flex w-72 shrink-0 flex-col border-r border-outline-variant/20 bg-surface-container-lowest/60">
      <div className="p-4 shrink-0">
        <button
          onClick={onNewChat}
          className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-primary text-on-primary rounded-xl text-sm font-medium hover:opacity-90 transition-opacity"
        >
          <Plus size={16} /> Cuộc trò chuyện mới
        </button>
      </div>

      <div className="px-4 pb-2 shrink-0">
        <h4 className="text-xs font-semibold uppercase tracking-wide text-on-surface-variant">
          Lịch sử trò chuyện
        </h4>
      </div>

      <div className="flex-1 overflow-y-auto custom-scrollbar px-2 pb-4 space-y-1">
        {loading && (
          <div className="flex items-center justify-center py-8 text-on-surface-variant">
            <Loader2 size={16} className="animate-spin" />
          </div>
        )}

        {!loading && sessions.length === 0 && (
          <p className="px-3 py-6 text-xs text-on-surface-variant text-center">
            Chưa có cuộc trò chuyện nào. Hãy bắt đầu hỏi Stylist AI!
          </p>
        )}

        {!loading && sessions.map((session) => {
          const isActive = session.id === activeSessionId
          return (
            <button
              key={session.id}
              onClick={() => onSelect(session.id)}
              className={clsx(
                'group w-full flex items-start gap-2.5 px-3 py-2.5 rounded-xl text-left transition-colors',
                isActive
                  ? 'bg-primary text-on-primary'
                  : 'hover:bg-surface-container text-on-surface'
              )}
            >
              <MessageSquare
                size={14}
                className={clsx('shrink-0 mt-0.5', isActive ? 'text-on-primary' : 'text-on-surface-variant')}
              />
              <span className="flex-1 min-w-0">
                <span className={clsx('block text-sm truncate', isActive && 'font-medium')}>
                  {session.title || 'Cuộc trò chuyện mới'}
                </span>
                <span className={clsx('block text-[10px] mt-0.5', isActive ? 'text-on-primary/70' : 'text-on-surface-variant')}>
                  {formatRelativeTime(session.updated_at)}
                </span>
              </span>
              <span
                role="button"
                tabIndex={-1}
                aria-label="Xóa cuộc trò chuyện"
                title="Xóa cuộc trò chuyện"
                onClick={(e) => handleDelete(e, session.id)}
                className={clsx(
                  'opacity-0 group-hover:opacity-100 p-1 rounded-md transition-all shrink-0',
                  isActive
                    ? 'text-on-primary/80 hover:text-on-primary hover:bg-white/20'
                    : 'text-on-surface-variant hover:text-error hover:bg-error-container/30'
                )}
              >
                {deletingId === session.id
                  ? <Loader2 size={12} className="animate-spin" />
                  : <Trash2 size={12} />}
              </span>
            </button>
          )
        })}
      </div>
    </aside>
  )
}
