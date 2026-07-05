import { Search, X } from 'lucide-react'

export default function ProductFilter({ filters, onFilterChange, onClose }) {
  const clearFilters = () => {
    onFilterChange({
      ...filters,
      search: '',
      minPrice: null,
      maxPrice: null,
    })
  }

  return (
    <div className="border-y border-outline-variant/20 bg-surface-container-lowest py-5">
      <div className="grid gap-5 md:grid-cols-[minmax(220px,1fr)_180px_180px_auto] md:items-end">
        <label className="block">
          <span className="mb-2 block text-xs font-medium uppercase text-on-surface-variant">
            Tìm sản phẩm
          </span>
          <span className="relative block">
            <Search
              size={16}
              className="absolute left-0 top-1/2 -translate-y-1/2 text-on-surface-variant"
              aria-hidden="true"
            />
            <input
              type="search"
              value={filters.search || ''}
              onChange={(event) => onFilterChange({ ...filters, search: event.target.value })}
              placeholder="Tên hoặc mô tả"
              className="w-full border-b border-outline-variant bg-transparent py-2 pl-6 text-sm outline-none transition-colors focus:border-tertiary-container"
            />
          </span>
        </label>

        <label className="block">
          <span className="mb-2 block text-xs font-medium uppercase text-on-surface-variant">
            Giá tối thiểu
          </span>
          <input
            type="number"
            min="0"
            value={filters.minPrice ?? ''}
            onChange={(event) => onFilterChange({
              ...filters,
              minPrice: event.target.value ? Number(event.target.value) : null,
            })}
            placeholder="0 ₫"
            className="w-full border-b border-outline-variant bg-transparent py-2 text-sm outline-none transition-colors focus:border-tertiary-container"
          />
        </label>

        <label className="block">
          <span className="mb-2 block text-xs font-medium uppercase text-on-surface-variant">
            Giá tối đa
          </span>
          <input
            type="number"
            min="0"
            value={filters.maxPrice ?? ''}
            onChange={(event) => onFilterChange({
              ...filters,
              maxPrice: event.target.value ? Number(event.target.value) : null,
            })}
            placeholder="Không giới hạn"
            className="w-full border-b border-outline-variant bg-transparent py-2 text-sm outline-none transition-colors focus:border-tertiary-container"
          />
        </label>

        <div className="flex items-center gap-2 md:justify-end">
          <button
            type="button"
            onClick={clearFilters}
            className="text-sm text-on-surface-variant transition-colors hover:text-primary"
          >
            Xóa lọc
          </button>
          <button
            type="button"
            onClick={onClose}
            className="flex h-9 w-9 items-center justify-center rounded-lg border border-outline-variant/30 text-primary hover:bg-surface-container"
            title="Đóng bộ lọc"
            aria-label="Đóng bộ lọc"
          >
            <X size={16} aria-hidden="true" />
          </button>
        </div>
      </div>
    </div>
  )
}
