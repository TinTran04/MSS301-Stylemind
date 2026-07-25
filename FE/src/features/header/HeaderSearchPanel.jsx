import { useEffect, useRef } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { ArrowRight, Search, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import { buildShopCategoryPath, groupCategories } from './headerNavigation'

function trapFocus(event, panel) {
  if (event.key !== 'Tab') return
  const focusable = panel?.querySelectorAll(
    'a[href], button:not([disabled]), input:not([disabled]), [tabindex]:not([tabindex="-1"])',
  )
  if (!focusable?.length) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

export default function HeaderSearchPanel({ isOpen, onClose, onSubmit, query, onQueryChange, categories, categoryStatus, onRetry }) {
  const inputRef = useRef(null)
  const panelRef = useRef(null)
  const categoryShortcuts = groupCategories(categories).flatMap((group) => group.categories).slice(0, 8)

  useEffect(() => {
    if (!isOpen) return undefined
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onClose()
      trapFocus(event, panelRef.current)
    }
    document.addEventListener('keydown', handleKeyDown)
    requestAnimationFrame(() => inputRef.current?.focus())
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-x-0 top-20 bottom-0 z-40">
          <motion.button
            type="button"
            aria-label="Đóng tìm kiếm"
            className="absolute inset-0 w-full cursor-default bg-primary/30"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />
          <motion.section
            ref={panelRef}
            id="stylemind-search-panel"
            role="dialog"
            aria-modal="true"
            aria-label="Tìm kiếm sản phẩm"
            tabIndex="-1"
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
            transition={{ duration: 0.18 }}
            className="relative mx-auto max-w-4xl border-x border-b border-outline-variant/30 bg-surface px-6 py-8 shadow-xl sm:px-10 motion-reduce:transition-none"
          >
            <div className="flex items-start justify-between gap-6">
              <div>
                <p className="font-label-sm uppercase tracking-[0.16em] text-on-surface-variant">Khám phá StyleMind</p>
                <h2 className="mt-2 font-display text-3xl text-primary">Tìm thiết kế dành cho bạn</h2>
              </div>
              <button
                type="button"
                onClick={onClose}
                aria-label="Đóng tìm kiếm"
                title="Đóng tìm kiếm"
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg text-on-surface-variant transition-colors hover:bg-surface-container-high focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
              >
                <X size={20} aria-hidden="true" />
              </button>
            </div>

            <form
              className="mt-8 flex border-b-2 border-primary"
              onSubmit={(event) => {
                event.preventDefault()
                onSubmit()
              }}
            >
              <Search className="mt-3 shrink-0 text-on-surface-variant" size={20} aria-hidden="true" />
              <input
                ref={inputRef}
                value={query}
                onChange={(event) => onQueryChange(event.target.value)}
                className="min-w-0 flex-1 bg-transparent px-4 py-3 text-lg text-primary outline-none placeholder:text-on-surface-variant"
                placeholder="Tìm theo kiểu dáng, chất liệu hoặc tên sản phẩm"
                aria-label="Từ khóa tìm kiếm sản phẩm"
              />
              <button
                type="submit"
                className="inline-flex items-center gap-2 px-3 text-sm font-semibold text-primary transition-opacity hover:opacity-70 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary disabled:cursor-not-allowed disabled:opacity-40"
                disabled={!query.trim()}
              >
                Tìm <ArrowRight size={17} aria-hidden="true" />
              </button>
            </form>

            <div className="mt-8">
              <p className="font-label-sm uppercase tracking-[0.14em] text-on-surface-variant">Duyệt nhanh danh mục</p>
              {categoryStatus === 'loading' && <p className="mt-3 text-sm text-on-surface-variant">Đang tải danh mục...</p>}
              {categoryStatus === 'error' && (
                <div className="mt-3 flex items-center gap-3 text-sm text-on-surface-variant">
                  <span>Không thể tải danh mục.</span>
                  <button type="button" onClick={onRetry} className="font-semibold text-primary underline underline-offset-4">Thử lại</button>
                </div>
              )}
              {categoryStatus === 'ready' && (
                <div className="mt-4 flex flex-wrap gap-2">
                  {categoryShortcuts.length ? categoryShortcuts.map((category) => (
                    <Link
                      key={category.id}
                      to={buildShopCategoryPath(category)}
                      onClick={onClose}
                      className="rounded-lg border border-outline-variant/40 px-3 py-2 text-sm text-on-surface-variant no-underline transition-colors hover:border-primary hover:text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                    >
                      {category.name}
                    </Link>
                  )) : <p className="mt-3 text-sm text-on-surface-variant">Chưa có danh mục để hiển thị.</p>}
                </div>
              )}
            </div>
          </motion.section>
        </div>
      )}
    </AnimatePresence>
  )
}
