import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Sparkles } from 'lucide-react'
import ProductCard from '../../components/customer/ProductCard'
import { getProducts } from '../../features/products/product.api'

const categories = [
  { label: 'Nam', targetDemographic: 'MALE', image: 'https://images.unsplash.com/photo-1487222477894-8943e31ef7b2?w=600&h=800&fit=crop' },
  { label: 'Nữ', targetDemographic: 'FEMALE', image: 'https://images.unsplash.com/photo-1509631179647-0177331693ae?w=700&h=900&fit=crop' },
  { label: 'Unisex', targetDemographic: 'UNISEX', image: 'https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=700&h=900&fit=crop' },
]

export default function HomePage() {
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false

    getProducts({ size: 8, sort: 'createdAt,desc' })
      .then((result) => {
        if (!cancelled) setProducts(result)
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
  }, [])

  return (
    <div>
      {/* Hero Section */}
      <section className="max-w-[1440px] mx-auto px-6 md:px-16 py-12 md:py-20">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          <div>
            <div className="flex items-center gap-2 mb-6">
              <Sparkles size={16} className="text-tertiary" />
              <span className="font-label-sm uppercase text-on-surface-variant">STYLIST AI CÁ NHÂN</span>
            </div>
            <h1 className="font-headline-lg text-primary leading-tight mb-6">
              Mặc đẹp hơn,<br />chọn nhanh hơn.
            </h1>
            <p className="text-on-surface-variant text-lg mb-8 max-w-md leading-relaxed">
              StyleMind phân tích phong cách, nhu cầu và ngân sách để đề xuất những trang phục phù hợp riêng với bạn.
            </p>
            <div className="flex gap-4">
              <Link to="/shop" className="group bg-primary text-on-primary px-8 py-3 rounded-lg text-sm font-medium hover:opacity-90 active:scale-95 transition-all inline-flex items-center gap-2 no-underline">
                Khám phá cửa hàng <ArrowRight size={16} className="transform group-hover:translate-x-1 transition-transform duration-300" />
              </Link>
              <Link to="/ai-stylist" className="group border border-primary text-primary px-8 py-3 rounded-lg text-sm font-medium hover:bg-primary hover:text-on-primary active:scale-95 transition-all inline-flex items-center gap-2 no-underline">
                <Sparkles size={16} className="transform group-hover:scale-110 group-hover:rotate-12 transition-transform duration-300" /> Thử Stylist AI
              </Link>
            </div>
          </div>
          <div className="relative">
            <img
              src="https://images.unsplash.com/photo-1483985988355-763728e1935b?w=800&h=1000&fit=crop"
              alt="Ảnh biên tập thời trang"
              className="w-full rounded-[24px] editorial-shadow"
            />
            <div className="absolute -bottom-6 -left-6 bg-surface-container-lowest rounded-xl p-4 product-card-shadow">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-ai-lavender flex items-center justify-center">
                  <Sparkles size={14} className="text-ai-indigo animate-spin-slow" />
                </div>
                <div>
                  <p className="text-xs font-medium text-primary">Phù hợp AI</p>
                  <p className="text-xs text-on-surface-variant">Phù hợp 98%</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Category Bento Grid */}
      <section className="max-w-[1440px] mx-auto px-6 md:px-16 py-12">
        <div className="flex items-center justify-between mb-8">
          <h2 className="font-headline-md text-primary">Mua sắm theo danh mục</h2>
          <Link to="/shop" className="text-sm text-on-surface-variant hover:text-primary transition-colors no-underline flex items-center gap-1">
            Xem tất cả <ArrowRight size={14} />
          </Link>
        </div>
        <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
          {categories.map((cat) => (
            <Link
              key={cat.targetDemographic}
              to={`/shop?targetDemographic=${cat.targetDemographic}`}
              onClick={() => window.scrollTo({ top: 0, left: 0, behavior: 'auto' })}
              className="group relative aspect-[4/5] overflow-hidden rounded-[24px] no-underline"
            >
              <img
                src={cat.image}
                alt={cat.label}
                className="h-full w-full object-cover grayscale-[12%] saturate-[0.86] contrast-[0.96] transition-transform duration-700 group-hover:scale-105"
              />
              <div className="absolute inset-0 bg-primary/25 transition-colors duration-300 group-hover:bg-primary/35" />
              <div className="absolute inset-0 bg-gradient-to-t from-primary/70 via-primary/10 to-transparent" />
              <div className="absolute bottom-6 left-6">
                <h3 className="font-headline-md text-on-primary">{cat.label}</h3>
                <span className="text-on-primary/70 text-sm flex items-center gap-1 mt-1">
                  Mua ngay <ArrowRight size={14} className="transform group-hover:translate-x-1 transition-transform duration-300" />
                </span>
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* Product Grid */}
      <section className="max-w-[1440px] mx-auto px-6 md:px-16 py-12">
        <div className="flex items-center justify-between mb-8">
          <div>
          <h2 className="font-headline-md text-primary">Sản phẩm nổi bật</h2>
        </div>
        <Link to="/shop" className="text-sm text-on-surface-variant hover:text-primary transition-colors no-underline flex items-center gap-1">
          Xem tất cả <ArrowRight size={14} />
        </Link>
      </div>
        {loading ? (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6" aria-busy="true">
            {Array.from({ length: 4 }).map((_, idx) => (
              <div key={idx} className="aspect-[3/4] rounded-[24px] bg-surface-container animate-pulse" />
            ))}
          </div>
        ) : error ? (
          <div role="alert" className="rounded-xl border border-error/20 bg-error-container/30 p-6 text-sm text-error">
            {error}
          </div>
        ) : products.length === 0 ? (
          <div className="rounded-xl border border-outline-variant/20 p-8 text-center text-on-surface-variant">
            Chưa có sản phẩm nào.
          </div>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            {products.slice(0, 8).map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}
      </section>

      {/* Footer CTA */}
      <section className="bg-primary-container mx-6 md:mx-16 rounded-[24px] my-12">
        <div className="max-w-[1440px] mx-auto px-12 py-16 text-center">
          <h2 className="font-headline-md text-on-primary-container mb-4">Tìm phong cách hợp với bạn</h2>
          <p className="text-on-primary-container/70 mb-8 max-w-xl mx-auto">
            Bắt đầu với Stylist AI để chọn outfit theo dáng người, dịp mặc và ngân sách của riêng bạn.
          </p>
          <Link to="/ai-stylist" className="inline-flex items-center gap-2 rounded-lg bg-tertiary-container px-6 py-3 text-sm font-medium text-on-primary no-underline transition-opacity hover:opacity-90 active:scale-95">
            Thử Stylist AI <ArrowRight size={16} />
          </Link>
        </div>
      </section>
    </div>
  )
}
