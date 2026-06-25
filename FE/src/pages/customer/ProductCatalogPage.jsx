import { useState, useEffect, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import ProductCard from '../../components/customer/ProductCard'
import ProductFilter from '../../components/customer/ProductFilter'
import { getCategories, getProducts } from '../../features/products/product.api'

export default function ProductCatalogPage() {
  const [searchParams] = useSearchParams()
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filters, setFilters] = useState({
    category: searchParams.get('category') || null,
    minPrice: null,
    maxPrice: null,
    colors: [],
    sort: 'ai_match',
  })
  const [sortBy, setSortBy] = useState('ai_match')
  const [currentPage, setCurrentPage] = useState(1)
  const perPage = 8

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')

    getProducts({ ...filters, size: 100 })
      .then((result) => {
        if (!cancelled) {
          setProducts(result)
          setCurrentPage(1)
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Unable to load products.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [filters])

  useEffect(() => {
    getCategories().then(setCategories).catch(() => setCategories([]))
  }, [])

  const sortedProducts = useMemo(() => {
    const sorted = [...products]
    if (sortBy === 'price_asc') sorted.sort((a, b) => a.price - b.price)
    if (sortBy === 'price_desc') sorted.sort((a, b) => b.price - a.price)
    if (sortBy === 'rating') sorted.sort((a, b) => b.rating - a.rating)
    if (sortBy === 'ai_match') sorted.sort((a, b) => b.aiMatchScore - a.aiMatchScore)
    return sorted
  }, [products, sortBy])

  const totalPages = Math.ceil(sortedProducts.length / perPage)
  const paginatedProducts = sortedProducts.slice((currentPage - 1) * perPage, currentPage * perPage)

  return (
    <div className="max-w-[1440px] mx-auto px-6 md:px-16 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="font-headline-md text-primary">Shop</h1>
        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value)}
          className="bg-surface-container-low border border-outline-variant/20 rounded-lg px-4 py-2 text-sm text-on-surface focus:outline-none focus:border-tertiary-container"
        >
          <option value="ai_match">AI Match</option>
          <option value="price_asc">Price: Low to High</option>
          <option value="price_desc">Price: High to Low</option>
          <option value="rating">Top Rated</option>
        </select>
      </div>

      <div className="flex gap-8">
        <ProductFilter filters={filters} onFilterChange={setFilters} categories={categories} />

        <div className="flex-1">
          {loading ? (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6" aria-busy="true">
              {Array.from({ length: 8 }).map((_, idx) => (
                <div key={idx} className="aspect-[3/4] rounded-[24px] bg-surface-container animate-pulse" />
              ))}
            </div>
          ) : error ? (
            <div role="alert" className="rounded-xl border border-error/20 bg-error-container/30 p-6 text-sm text-error">
              {error}
            </div>
          ) : sortedProducts.length === 0 ? (
            <div className="text-center py-20">
              <p className="text-on-surface-variant">No products found matching your filters.</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
              {paginatedProducts.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          )}

          {!loading && !error && totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-12">
              {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => (
                <button
                  key={page}
                  onClick={() => setCurrentPage(page)}
                  className={`w-10 h-10 rounded-full text-sm font-medium transition-all ${
                    currentPage === page
                      ? 'bg-primary text-on-primary'
                      : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-high'
                  }`}
                >
                  {page}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
