import { useEffect, useState } from 'react'
import { ChevronDown, ChevronLeft, ChevronRight, SlidersHorizontal } from 'lucide-react'
import { useSearchParams } from 'react-router-dom'
import ProductCard from '../../components/customer/ProductCard'
import ProductFilter from '../../components/customer/ProductFilter'
import { getCategories, getProductPage } from '../../features/products/product.api'
import { getTargetDemographicOptions, normalizeTargetDemographic } from '../../features/products/product.demographic'

const PAGE_SIZE = 12
const FILTER_QUERY_KEYS = ['category', 'categorySlug', 'targetDemographic', 'search', 'minPrice', 'maxPrice', 'sort', 'collection']

function numericParam(value) {
  if (!value) return null
  const number = Number(value)
  return Number.isNaN(number) ? null : number
}

function normalizeSlug(value) {
  return String(value || '').trim().toLowerCase()
}

function categoryValueFromQuery(searchParams, categories) {
  const category = searchParams.get('category')
  if (category && !Number.isNaN(Number(category))) {
    return {
      category: String(Number(category)),
      categorySlug: '',
    }
  }

  const slug = normalizeSlug(searchParams.get('categorySlug') || category)
  if (!slug) {
    return {
      category: null,
      categorySlug: '',
    }
  }

  const matchedCategory = categories.find((item) => normalizeSlug(item.slug) === slug)
  return {
    category: matchedCategory ? String(matchedCategory.id) : null,
    categorySlug: matchedCategory ? '' : slug,
  }
}

function filtersFromSearchParams(searchParams, categories = []) {
  const rawCategory = searchParams.get('category')
  const demographicFromCategoryAlias = rawCategory && Number.isNaN(Number(rawCategory))
    ? normalizeTargetDemographic(rawCategory)
    : ''
  const category = demographicFromCategoryAlias
    ? { category: null, categorySlug: '' }
    : categoryValueFromQuery(searchParams, categories)

  return {
    ...category,
    search: searchParams.get('search') || '',
    minPrice: numericParam(searchParams.get('minPrice')),
    maxPrice: numericParam(searchParams.get('maxPrice')),
    sort: searchParams.get('sort') || 'newest',
    targetDemographic: normalizeTargetDemographic(searchParams.get('targetDemographic') || demographicFromCategoryAlias),
    page: 0,
  }
}

function hasFilterQuery(searchParams) {
  return FILTER_QUERY_KEYS.some((key) => searchParams.has(key))
}

function sameFilters(left, right) {
  return FILTER_QUERY_KEYS.every((key) => (left[key] || '') === (right[key] || '')) && left.page === right.page
}

export default function ProductCatalogPage() {
  const [searchParams] = useSearchParams()
  const queryString = searchParams.toString()
  const [productPage, setProductPage] = useState({
    content: [],
    page: 0,
    totalElements: 0,
    totalPages: 0,
  })
  const [categories, setCategories] = useState([])
  const [categoryError, setCategoryError] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [filters, setFilters] = useState(() => filtersFromSearchParams(searchParams))

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')

    getProductPage({ ...filters, size: PAGE_SIZE })
      .then((result) => {
        if (!cancelled) setProductPage(result)
      })
      .catch(() => {
        if (!cancelled) setError('Không thể tải sản phẩm.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [filters])

  useEffect(() => {
    getCategories()
      .then((data) => {
        setCategories(data)
        setCategoryError(false)
      })
      .catch(() => {
        // Don't crash the Shop page: fall back to "Tất cả" only and tell the user.
        setCategories([])
        setCategoryError(true)
      })
  }, [])

  useEffect(() => {
    if (!hasFilterQuery(searchParams)) return

    const nextFilters = filtersFromSearchParams(searchParams, categories)
    setFilters((current) => (sameFilters(current, nextFilters) ? current : nextFilters))
  }, [queryString, categories]) // eslint-disable-line react-hooks/exhaustive-deps

  const updateFilters = (nextFilters) => {
    setFilters({ ...nextFilters, page: 0 })
  }

  const selectCategory = (category) => {
    setFilters((current) => ({
      ...current,
      category: category ? String(category.id) : null,
      categorySlug: '',
      page: 0,
    }))
  }

  const selectDemographic = (targetDemographic) => {
    setFilters((current) => ({
      ...current,
      targetDemographic,
      page: 0,
    }))
  }

  const activeCategory = categories.find(
    (category) => String(category.id) === String(filters.category)
      || (filters.categorySlug && normalizeSlug(category.slug) === normalizeSlug(filters.categorySlug)),
  )

  return (
    <main className="mx-auto max-w-[1440px] px-4 py-8 sm:px-6 md:px-10 lg:px-16 lg:py-12">
      <header className="border-b border-outline-variant/20 pb-6">
        <p className="mb-2 text-xs font-medium uppercase text-tertiary">Bộ sưu tập StyleMind</p>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h1 className="font-headline-md text-primary">
              {activeCategory?.name || 'Bộ sưu tập'}
            </h1>
            <p className="mt-2 text-sm text-on-surface-variant">
              {productPage.totalElements} sản phẩm đang hoạt động
            </p>
          </div>
        </div>
      </header>

      <nav
        className="-mx-4 flex gap-2 overflow-x-auto px-4 py-5 sm:mx-0 sm:px-0"
        aria-label="Danh mục sản phẩm"
      >
        <button
          type="button"
          onClick={() => selectCategory(null)}
          className={`shrink-0 rounded-lg border px-4 py-2 text-sm transition-colors ${
            !filters.category
              ? 'border-primary bg-primary text-on-primary'
              : 'border-outline-variant/30 bg-surface-container-lowest text-primary hover:border-primary'
          }`}
        >
          Tất cả
        </button>
        {categories.map((category) => {
          const active = String(filters.category) === String(category.id)
          return (
            <button
              key={category.id}
              type="button"
              onClick={() => selectCategory(category)}
              className={`shrink-0 rounded-lg border px-4 py-2 text-sm transition-colors ${
                active
                  ? 'border-primary bg-primary text-on-primary'
                  : 'border-outline-variant/30 bg-surface-container-lowest text-primary hover:border-primary'
              }`}
            >
              {category.name}
            </button>
          )
        })}
      </nav>

      <div className="flex flex-wrap items-center gap-2 pb-4 pt-1">
        <span className="text-xs font-medium uppercase text-on-surface-variant">Đối tượng</span>
        {getTargetDemographicOptions().map((option) => {
          const active = normalizeTargetDemographic(filters.targetDemographic) === normalizeTargetDemographic(option.value)
          return (
            <button
              key={option.value || 'all'}
              type="button"
              onClick={() => selectDemographic(option.value)}
              className={`shrink-0 rounded-lg border px-4 py-2 text-sm transition-colors ${
                active
                  ? 'border-primary bg-primary text-on-primary'
                  : 'border-outline-variant/30 bg-surface-container-lowest text-primary hover:border-primary'
              }`}
            >
              {option.label}
            </button>
          )
        })}
      </div>

      {categoryError ? (
        <p className="-mt-2 pb-2 text-xs text-on-surface-variant">
          Không thể tải danh mục. Bạn vẫn có thể xem tất cả sản phẩm.
        </p>
      ) : null}

      <div className="flex min-h-12 items-center justify-between gap-4 border-y border-outline-variant/20 py-3">
        <button
          type="button"
          onClick={() => setFiltersOpen((open) => !open)}
          className="flex items-center gap-2 text-sm font-medium text-primary"
          aria-expanded={filtersOpen}
        >
          <SlidersHorizontal size={16} aria-hidden="true" />
          Bộ lọc
        </button>

        <div className="flex items-center gap-4">
          <span className="hidden text-sm text-on-surface-variant sm:inline">
            {productPage.totalElements} kết quả
          </span>
          <label className="relative">
            <span className="sr-only">Sắp xếp sản phẩm</span>
            <select
              value={filters.sort}
              onChange={(event) => setFilters((current) => ({
                ...current,
                sort: event.target.value,
                page: 0,
              }))}
              className="appearance-none bg-transparent py-2 pl-2 pr-7 text-sm font-medium text-primary outline-none"
            >
              <option value="newest">Mới nhất</option>
              <option value="price_asc">Giá tăng dần</option>
              <option value="price_desc">Giá giảm dần</option>
            </select>
            <ChevronDown
              size={14}
              className="pointer-events-none absolute right-1 top-1/2 -translate-y-1/2"
              aria-hidden="true"
            />
          </label>
        </div>
      </div>

      {filtersOpen ? (
        <ProductFilter
          filters={filters}
          onFilterChange={updateFilters}
          onClose={() => setFiltersOpen(false)}
        />
      ) : null}

      <section className="pt-8" aria-live="polite">
        {loading ? (
          <div className="grid grid-cols-2 gap-x-3 gap-y-8 md:grid-cols-3 md:gap-x-5 lg:grid-cols-4" aria-busy="true">
            {Array.from({ length: PAGE_SIZE }).map((_, index) => (
              <div key={index}>
                <div className="aspect-[3/4] animate-pulse rounded-lg bg-surface-container" />
                <div className="mt-3 h-4 w-3/4 animate-pulse bg-surface-container" />
                <div className="mt-2 h-3 w-1/2 animate-pulse bg-surface-container" />
              </div>
            ))}
          </div>
        ) : error ? (
          <div role="alert" className="border border-error/20 bg-error-container/30 p-6 text-sm text-error">
            {error}
          </div>
        ) : productPage.content.length === 0 ? (
          <div className="py-20 text-center">
            <p className="font-headline-sm text-primary">Không tìm thấy sản phẩm</p>
            <p className="mt-2 text-sm text-on-surface-variant">
              Hãy thử danh mục hoặc khoảng giá khác.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-x-3 gap-y-8 md:grid-cols-3 md:gap-x-5 lg:grid-cols-4 lg:gap-y-10">
            {productPage.content.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}
      </section>

      {!loading && !error && productPage.totalPages > 1 ? (
        <nav className="mt-12 flex items-center justify-center gap-4" aria-label="Phân trang sản phẩm">
          <button
            type="button"
            onClick={() => setFilters((current) => ({ ...current, page: current.page - 1 }))}
            disabled={productPage.first}
            className="flex h-10 w-10 items-center justify-center rounded-lg border border-outline-variant/30 disabled:opacity-35"
            title="Trang trước"
            aria-label="Trang trước"
          >
            <ChevronLeft size={18} aria-hidden="true" />
          </button>
          <span className="min-w-24 text-center text-sm text-on-surface-variant">
            Trang {productPage.page + 1} / {productPage.totalPages}
          </span>
          <button
            type="button"
            onClick={() => setFilters((current) => ({ ...current, page: current.page + 1 }))}
            disabled={productPage.last}
            className="flex h-10 w-10 items-center justify-center rounded-lg border border-outline-variant/30 disabled:opacity-35"
            title="Trang sau"
            aria-label="Trang sau"
          >
            <ChevronRight size={18} aria-hidden="true" />
          </button>
        </nav>
      ) : null}
    </main>
  )
}
