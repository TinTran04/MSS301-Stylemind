import { useEffect, useRef, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { ArrowRight, ChevronRight, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import { buildShopCategoryPath, groupCategories } from './headerNavigation'

export default function CategoryMegaMenu({ isOpen, onClose, categories, categoryStatus, onRetry }) {
  const panelRef = useRef(null)
  const groups = groupCategories(categories)
  const [selectedGroupId, setSelectedGroupId] = useState(null)
  const selectedGroup = groups.find((group) => group.id === selectedGroupId) || groups[0]

  useEffect(() => {
    if (isOpen && groups.length && !groups.some((group) => group.id === selectedGroupId)) {
      setSelectedGroupId(groups[0].id)
    }
  }, [groups, isOpen, selectedGroupId])

  useEffect(() => {
    if (!isOpen) return undefined
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') onClose()
      if (event.key !== 'Tab') return
      const focusable = panelRef.current?.querySelectorAll(
        'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])',
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
    document.addEventListener('keydown', handleKeyDown)
    requestAnimationFrame(() => panelRef.current?.focus())
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, onClose])

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-x-0 top-20 bottom-0 z-40">
          <motion.button
            type="button"
            aria-label="Đóng danh mục"
            className="absolute inset-0 w-full cursor-default bg-primary/30"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />
          <motion.section
            ref={panelRef}
            role="dialog"
            aria-modal="true"
            aria-label="Danh mục Cửa hàng"
            tabIndex="-1"
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
            transition={{ duration: 0.18 }}
            className="relative mx-auto max-w-[1440px] border-x border-b border-outline-variant/30 bg-surface px-6 py-8 shadow-xl md:px-12 motion-reduce:transition-none"
          >
            <div className="flex items-start justify-between gap-6">
              <div>
                <p className="font-label-sm uppercase tracking-[0.16em] text-on-surface-variant">Cửa hàng</p>
                <h2 className="mt-2 font-display text-3xl text-primary">Danh mục tuyển chọn</h2>
              </div>
              <div className="flex items-center gap-2">
                <Link
                  to="/shop"
                  onClick={onClose}
                  className="hidden items-center gap-2 px-2 py-2 text-sm font-semibold text-primary no-underline hover:underline sm:inline-flex"
                >
                  Xem tất cả <ArrowRight size={16} aria-hidden="true" />
                </Link>
                <button
                  type="button"
                  onClick={onClose}
                  aria-label="Đóng danh mục"
                  title="Đóng danh mục"
                  className="flex h-10 w-10 items-center justify-center rounded-lg text-on-surface-variant transition-colors hover:bg-surface-container-high focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                >
                  <X size={20} aria-hidden="true" />
                </button>
              </div>
            </div>

            {categoryStatus === 'loading' && <p className="mt-8 text-sm text-on-surface-variant">Đang tải danh mục...</p>}
            {categoryStatus === 'error' && (
              <div className="mt-8 flex items-center gap-3 text-sm text-on-surface-variant">
                <span>Không thể tải danh mục.</span>
                <button type="button" onClick={onRetry} className="font-semibold text-primary underline underline-offset-4">Thử lại</button>
              </div>
            )}
            {categoryStatus === 'ready' && (
              groups.length ? (
                <div className="mt-8 grid gap-8 lg:grid-cols-[220px_minmax(0,1fr)]">
                  <div className="border-b border-outline-variant/30 pb-4 lg:border-b-0 lg:border-r lg:pb-0 lg:pr-6">
                    <div className="flex gap-2 overflow-x-auto lg:block lg:space-y-1">
                      {groups.map((group) => (
                        <button
                          key={group.id}
                          type="button"
                          onClick={() => setSelectedGroupId(group.id)}
                          className={`inline-flex min-h-10 shrink-0 items-center gap-2 rounded-lg px-3 py-2 text-left text-sm font-medium transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary lg:flex lg:w-full lg:justify-between ${
                            selectedGroup?.id === group.id ? 'bg-surface-container-high text-primary' : 'text-on-surface-variant hover:bg-surface-container'
                          }`}
                        >
                          {group.label}<ChevronRight size={16} aria-hidden="true" />
                        </button>
                      ))}
                    </div>
                  </div>
                  {selectedGroup && (
                    <div>
                      <div className="flex items-baseline justify-between gap-4">
                        <h3 className="font-title-lg text-primary">{selectedGroup.label}</h3>
                        {selectedGroup.parent && (
                          <Link to={buildShopCategoryPath(selectedGroup.parent)} onClick={onClose} className="text-sm font-semibold text-primary no-underline hover:underline">
                            Xem tất cả {selectedGroup.parent.name}
                          </Link>
                        )}
                      </div>
                      <div className="mt-5 grid grid-cols-1 gap-x-8 gap-y-1 sm:grid-cols-2 xl:grid-cols-3">
                        {selectedGroup.categories.map((category) => (
                          <Link
                            key={category.id}
                            to={buildShopCategoryPath(category)}
                            onClick={onClose}
                            className="group flex min-h-11 items-center justify-between border-b border-outline-variant/20 py-2 text-sm text-on-surface-variant no-underline transition-colors hover:text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                          >
                            {category.name}<ArrowRight className="opacity-0 transition-opacity group-hover:opacity-100" size={15} aria-hidden="true" />
                          </Link>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              ) : <p className="mt-8 text-sm text-on-surface-variant">Chưa có danh mục để hiển thị.</p>
            )}
          </motion.section>
        </div>
      )}
    </AnimatePresence>
  )
}
