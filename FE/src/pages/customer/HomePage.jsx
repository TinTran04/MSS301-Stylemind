import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Mail, Sparkles, ShoppingBag } from 'lucide-react'
import ProductCard from '../../components/customer/ProductCard'
import { getProducts } from '../../features/products/product.api'

const categories = [
  { label: 'Nữ', to: '/shop?targetDemographic=FEMALE', image: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=600&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8ZmVtYWxlJTIwbW9kZWxzfGVufDB8fDB8fHww', cols: 'col-span-2 row-span-2' },
  { label: 'Nam', to: '/shop?targetDemographic=MALE', image: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600&h=400&fit=crop', cols: 'col-span-2 row-span-1' },
  { label: 'Áo', to: '/shop?categorySlug=ao', image: 'https://images.unsplash.com/photo-1523381210434-271e8be1f52b?w=600&h=400&fit=crop', cols: 'col-span-1 row-span-1' },
  { label: 'Quần', to: '/shop?categorySlug=quan', image: 'https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=600&h=400&fit=crop', cols: 'col-span-1 row-span-1' },
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
              <span className="font-label-sm uppercase text-on-surface-variant">THỜI TRANG CÁ NHÂN HÓA BỞI AI</span>
            </div>
            <h1 className="font-headline-lg text-primary leading-tight mb-6">
              Phong cách riêng,<br />gợi ý riêng cho bạn
            </h1>
            <p className="text-on-surface-variant text-lg mb-8 max-w-md leading-relaxed">
              Khám phá những gợi ý thời trang được cá nhân hóa bởi AI, dựa trên gu thẩm mỹ và phong cách riêng của bạn.
            </p>
            <div className="flex gap-4">
              <Link to="/shop" className="group bg-primary text-on-primary px-8 py-3 rounded-lg text-sm font-medium hover:opacity-90 active:scale-95 transition-all inline-flex items-center gap-2 no-underline">
                Khám phá bộ sưu tập <ArrowRight size={16} className="transform group-hover:translate-x-1 transition-transform duration-300" />
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
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6 h-[500px] md:h-[600px]">
          {categories.map((cat) => (
            <Link
              key={cat.to}
              to={cat.to}
              className={`${cat.cols} relative rounded-[24px] overflow-hidden group no-underline`}
            >
              <img
                src={cat.image}
                alt={cat.label}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-primary/60 to-transparent" />
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
          <h2 className="font-headline-md text-primary">Gợi ý dành riêng cho bạn</h2>
          <p className="text-on-surface-variant mt-1">Những lựa chọn được tinh chỉnh theo phong cách riêng của bạn</p>
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

      {/* Newsletter CTA */}
      <section className="bg-primary-container mx-6 md:mx-16 rounded-[24px] my-12">
        <div className="max-w-[1440px] mx-auto px-12 py-16 text-center">
          <h2 className="font-headline-md text-on-primary-container mb-4">Luôn nhận gợi ý phù hợp</h2>
          <p className="text-on-primary-container/70 mb-8 max-w-md mx-auto">
            Nhận gợi ý thời trang cá nhân hóa từ AI ngay trong hộp thư của bạn. Không spam, chỉ có những gợi ý thực sự hữu ích.
          </p>
          <div className="flex max-w-md mx-auto">
            <div className="relative flex-1">
              <Mail size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-primary-container/50" />
              <input
                type="email"
                placeholder="Nhập email của bạn"
                className="w-full bg-surface-container-lowest/10 border border-on-primary-container/20 rounded-l-lg pl-10 pr-4 py-3 text-sm text-on-primary-container placeholder:text-on-primary-container/40 focus:outline-none focus:border-tertiary-container"
              />
            </div>
            <button className="bg-tertiary-container text-on-primary px-6 rounded-r-lg text-sm font-medium hover:opacity-90 transition-opacity">
              Đăng ký
            </button>
          </div>
        </div>
      </section>
    </div>
  )
}
