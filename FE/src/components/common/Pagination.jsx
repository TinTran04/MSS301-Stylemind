import { ChevronLeft, ChevronRight } from 'lucide-react'
import { getPageItems } from './pagination.utils.js'

export default function Pagination({ page = 0, totalPages = 0, onPageChange, label = 'Phân trang' }) {
  if (totalPages <= 1) return null

  const items = getPageItems(page, totalPages)
  return (
    <nav aria-label={label} className="flex flex-wrap items-center justify-center gap-1.5 py-5">
      <button
        type="button"
        aria-label="Trang trước"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
        className="inline-flex min-h-10 items-center gap-1 rounded-full border border-outline-variant/30 px-3 text-sm text-on-surface-variant transition-colors hover:bg-surface-container-high disabled:cursor-not-allowed disabled:opacity-35 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
      >
        <ChevronLeft size={15} aria-hidden="true" /> <span className="hidden sm:inline">Trang trước</span>
      </button>
      {items.map((item) => item.toString().startsWith('ellipsis-') ? (
        <span key={item} aria-hidden="true" className="px-1 text-on-surface-variant">…</span>
      ) : (
        <button
          key={item}
          type="button"
          aria-label={`Trang ${item + 1}`}
          aria-current={item === page ? 'page' : undefined}
          onClick={() => onPageChange(item)}
          className={`min-h-10 min-w-10 rounded-full px-3 text-sm font-medium transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary ${item === page ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:bg-surface-container-high'}`}
        >
          {item + 1}
        </button>
      ))}
      <button
        type="button"
        aria-label="Trang sau"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
        className="inline-flex min-h-10 items-center gap-1 rounded-full border border-outline-variant/30 px-3 text-sm text-on-surface-variant transition-colors hover:bg-surface-container-high disabled:cursor-not-allowed disabled:opacity-35 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
      >
        <span className="hidden sm:inline">Trang sau</span> <ChevronRight size={15} aria-hidden="true" />
      </button>
    </nav>
  )
}
