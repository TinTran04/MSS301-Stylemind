import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, Mail, Sparkles, ShoppingBag } from 'lucide-react'
import ProductCard from '../../components/customer/ProductCard'
import { getProducts } from '../../features/products/product.api'

const categories = [
  { name: 'Women', image: 'https://images.unsplash.com/photo-1487222477894-8943e31ef7b2?w=600&h=800&fit=crop', cols: 'col-span-2 row-span-2' },
  { name: 'Men', image: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=600&h=400&fit=crop', cols: 'col-span-2 row-span-1' },
  { name: 'Accessories', image: 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?w=600&h=400&fit=crop', cols: 'col-span-1 row-span-1' },
  { name: 'Footwear', image: 'https://images.unsplash.com/photo-1543163521-1bf539c55dd2?w=600&h=400&fit=crop', cols: 'col-span-1 row-span-1' },
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
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Unable to load products.')
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
              <span className="font-label-sm uppercase text-on-surface-variant">AI-Powered Fashion</span>
            </div>
            <h1 className="font-headline-lg text-primary leading-tight mb-6">
              Your Personal<br />Style Algorithm
            </h1>
            <p className="text-on-surface-variant text-lg mb-8 max-w-md leading-relaxed">
              Discover fashion curated by artificial intelligence that learns your unique aesthetic and recommends pieces you'll love.
            </p>
            <div className="flex gap-4">
              <Link to="/shop" className="group bg-primary text-on-primary px-8 py-3 rounded-lg text-sm font-medium hover:opacity-90 active:scale-95 transition-all inline-flex items-center gap-2 no-underline">
                Explore Collection <ArrowRight size={16} className="transform group-hover:translate-x-1 transition-transform duration-300" />
              </Link>
              <Link to="/ai-stylist" className="group border border-primary text-primary px-8 py-3 rounded-lg text-sm font-medium hover:bg-primary hover:text-on-primary active:scale-95 transition-all inline-flex items-center gap-2 no-underline">
                <Sparkles size={16} className="transform group-hover:scale-110 group-hover:rotate-12 transition-transform duration-300" /> AI Stylist
              </Link>
            </div>
          </div>
          <div className="relative">
            <img
              src="https://images.unsplash.com/photo-1483985988355-763728e1935b?w=800&h=1000&fit=crop"
              alt="Fashion editorial"
              className="w-full rounded-[24px] editorial-shadow"
            />
            <div className="absolute -bottom-6 -left-6 bg-surface-container-lowest rounded-xl p-4 product-card-shadow">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-ai-lavender flex items-center justify-center">
                  <Sparkles size={14} className="text-ai-indigo animate-spin-slow" />
                </div>
                <div>
                  <p className="text-xs font-medium text-primary">AI Match</p>
                  <p className="text-xs text-on-surface-variant">98% compatible</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Category Bento Grid */}
      <section className="max-w-[1440px] mx-auto px-6 md:px-16 py-12">
        <div className="flex items-center justify-between mb-8">
          <h2 className="font-headline-md text-primary">Shop by Category</h2>
          <Link to="/shop" className="text-sm text-on-surface-variant hover:text-primary transition-colors no-underline flex items-center gap-1">
            View All <ArrowRight size={14} />
          </Link>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6 h-[500px] md:h-[600px]">
          {categories.map((cat) => (
            <Link
              key={cat.name}
              to={`/shop?category=${cat.name}`}
              className={`${cat.cols} relative rounded-[24px] overflow-hidden group no-underline`}
            >
              <img
                src={cat.image}
                alt={cat.name}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-primary/60 to-transparent" />
              <div className="absolute bottom-6 left-6">
                <h3 className="font-headline-md text-on-primary">{cat.name}</h3>
                <span className="text-on-primary/70 text-sm flex items-center gap-1 mt-1">
                  Shop Now <ArrowRight size={14} className="transform group-hover:translate-x-1 transition-transform duration-300" />
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
            <h2 className="font-headline-md text-primary">AI-Curated for You</h2>
            <p className="text-on-surface-variant mt-1">Personalized picks based on your style DNA</p>
          </div>
          <Link to="/shop" className="text-sm text-on-surface-variant hover:text-primary transition-colors no-underline flex items-center gap-1">
            View All <ArrowRight size={14} />
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
            No products are available yet.
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
          <h2 className="font-headline-md text-on-primary-container mb-4">Stay in the Algorithm</h2>
          <p className="text-on-primary-container/70 mb-8 max-w-md mx-auto">
            Get AI-curated style recommendations delivered to your inbox. No spam, just fashion intelligence.
          </p>
          <div className="flex max-w-md mx-auto">
            <div className="relative flex-1">
              <Mail size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-primary-container/50" />
              <input
                type="email"
                placeholder="Enter your email"
                className="w-full bg-surface-container-lowest/10 border border-on-primary-container/20 rounded-l-lg pl-10 pr-4 py-3 text-sm text-on-primary-container placeholder:text-on-primary-container/40 focus:outline-none focus:border-tertiary-container"
              />
            </div>
            <button className="bg-tertiary-container text-on-primary px-6 rounded-r-lg text-sm font-medium hover:opacity-90 transition-opacity">
              Subscribe
            </button>
          </div>
        </div>
      </section>
    </div>
  )
}
