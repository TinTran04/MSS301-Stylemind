import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ShoppingBag, Heart, Share2, Sparkles, Star } from 'lucide-react'
import Badge from '../../components/common/Badge'
import ProductCard from '../../components/customer/ProductCard'
import useCartStore from '../../features/cart/cart.store'
import { getProductById, getProducts } from '../../features/products/product.api'

export default function ProductDetailPage() {
  const { id } = useParams()
  const [product, setProduct] = useState(null)
  const [recommendations, setRecommendations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedSize, setSelectedSize] = useState(null)
  const [selectedColor, setSelectedColor] = useState(null)
  const addItem = useCartStore((s) => s.addItem)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')

    Promise.all([
      getProductById(id),
      getProducts({ size: 4 }),
    ])
      .then(([detail, all]) => {
        if (cancelled) return
        setProduct(detail)
        setSelectedSize(detail?.sizes?.[0] || null)
        setSelectedColor(detail?.colors?.[0] || null)
        setRecommendations(all.filter((p) => p.id !== id).slice(0, 3))
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Unable to load product.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [id])

  if (loading) {
    return <div className="max-w-[1440px] mx-auto px-16 py-20 text-center">Loading...</div>
  }

  if (error || !product) {
    return (
      <div className="max-w-[1440px] mx-auto px-6 md:px-16 py-20 text-center">
        <p className="text-error">{error || 'Product not found.'}</p>
        <Link to="/shop" className="text-primary hover:underline">Back to shop</Link>
      </div>
    )
  }

  const hasVariant = Boolean(product.availableVariantId)

  const handleAddToCart = () => {
    addItem(product, 1, selectedSize || product.sizes[0], selectedColor || product.colors[0])
  }

  return (
    <div className="max-w-[1440px] mx-auto px-6 md:px-16 py-8">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
        {/* Image Gallery */}
        <div className="space-y-4">
          <div className="relative rounded-[24px] overflow-hidden aspect-[3/4]">
            <img src={product.images[0]} alt={product.name} className="w-full h-full object-cover" />
            {product.aiMatchScore >= 90 && (
              <div className="absolute top-4 left-4">
                <Badge variant="ai"><Sparkles size={12} /> {product.aiMatchScore}% Match</Badge>
              </div>
            )}
          </div>
        </div>

        {/* Product Info */}
        <div className="space-y-6">
          <div>
            <h1 className="font-headline-md text-primary">{product.name}</h1>
            <div className="flex items-center gap-4 mt-2">
              <span className="text-2xl font-semibold text-primary">${product.price}</span>
              {product.originalPrice && (
                <span className="text-on-surface-variant line-through">${product.originalPrice}</span>
              )}
              <div className="flex items-center gap-1">
                <Star size={14} className="text-tertiary fill-tertiary" />
                <span className="text-sm text-on-surface-variant">{product.rating} ({product.reviews})</span>
              </div>
            </div>
          </div>

          <p className="text-on-surface-variant leading-relaxed">{product.description}</p>

          {/* Colors */}
          <div>
            <label className="font-label-sm uppercase tracking-wider text-on-surface-variant mb-3 block">Color</label>
            <div className="flex gap-3">
              {product.colors.map((color) => (
                <button
                  key={color}
                  onClick={() => setSelectedColor(color)}
                  className={`px-4 py-2 rounded-lg border text-sm active:scale-95 transition-all ${
                    selectedColor === color
                      ? 'border-tertiary-container bg-surface-container-low'
                      : 'border-outline-variant/20 hover:border-outline-variant'
                  }`}
                >
                  {color}
                </button>
              ))}
            </div>
          </div>

          {/* Sizes */}
          <div>
            <label className="font-label-sm uppercase tracking-wider text-on-surface-variant mb-3 block">Size</label>
            <div className="flex gap-3">
              {product.sizes.map((size) => (
                <button
                  key={size}
                  onClick={() => setSelectedSize(size)}
                  className={`w-12 h-12 rounded-lg border text-sm font-medium active:scale-95 transition-all ${
                    selectedSize === size
                      ? 'border-primary bg-primary text-on-primary'
                      : 'border-outline-variant/20 hover:border-outline-variant'
                  }`}
                >
                  {size}
                </button>
              ))}
            </div>
          </div>

          {/* Stock Status */}
          {!hasVariant ? (
            <div className="bg-error-container/50 text-error px-4 py-3 rounded-lg text-sm font-medium">
              No variant available
            </div>
          ) : null}

          {/* Add to Cart */}
          <div className="flex gap-3">
            <button
              onClick={handleAddToCart}
              disabled={!hasVariant}
              className="group/bag flex-1 bg-primary text-on-primary rounded-lg py-3 text-sm font-medium hover:opacity-90 active:scale-98 transition-all flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <ShoppingBag size={16} className="transform group-hover/bag:-translate-y-0.5 transition-transform duration-300" />
              {hasVariant ? 'Add to Bag' : 'No Variant Available'}
            </button>
            <button className="group/btn p-3 rounded-lg border border-outline-variant/20 hover:bg-surface-container-high active:scale-90 transition-all">
              <Heart size={18} className="text-on-surface-variant transform group-hover/btn:scale-110 transition-transform duration-300" />
            </button>
            <button className="group/btn p-3 rounded-lg border border-outline-variant/20 hover:bg-surface-container-high active:scale-90 transition-all">
              <Share2 size={18} className="text-on-surface-variant transform group-hover/btn:scale-110 transition-transform duration-300" />
            </button>
          </div>

          {/* AI Match Analysis */}
          {product.aiMatchScore && (
            <div className="bg-ai-lavender/20 rounded-2xl p-5 border border-ai-lavender/30">
              <div className="flex items-center gap-2 mb-3">
                <Sparkles size={16} className="text-tertiary" />
                <span className="font-label-sm uppercase text-on-surface-variant">AI Match Analysis</span>
              </div>
              <div className="space-y-2">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-on-surface-variant">Style Compatibility</span>
                  <span className="font-medium text-primary">{product.aiMatchScore}%</span>
                </div>
                <div className="w-full h-1.5 bg-surface-container-high rounded-full overflow-hidden">
                  <div className="h-full bg-tertiary-container rounded-full" style={{ width: `${product.aiMatchScore}%` }} />
                </div>
                <p className="text-xs text-on-surface-variant mt-2">
                  This piece aligns with your minimalist aesthetic and neutral color preference.
                </p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Complete the Look */}
      <section className="mt-16">
        <h2 className="font-headline-md text-primary mb-6">Complete the Look</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 gap-6">
          {recommendations.map((rec) => (
            <ProductCard key={rec.id} product={rec} />
          ))}
        </div>
      </section>
    </div>
  )
}
