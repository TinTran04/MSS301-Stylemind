import { useState } from 'react'
import { ShoppingBag, Sparkles, Loader2 } from 'lucide-react'
import useCartStore from '../../features/cart/cart.store'
import { getProductById } from '../../features/products/product.api'
import { formatCurrency } from '../../utils/formatCurrency'

// Renders a recommendation straight from the AI stylist's stable RecommendedProduct
// shape (productId/name/basePrice/imageUrl/matchScore) - no local mock catalog lookup,
// since these ids come from the real product catalog.
export default function ProductBlock({ product, bundleId }) {
  const addItem = useCartStore((s) => s.addItem)
  const [adding, setAdding] = useState(false)
  const [added, setAdded] = useState(false)

  if (!product) return null

  const matchPercent = product.matchScore != null ? Math.round(product.matchScore * 100) : null

  const handleAddToCart = async () => {
    setAdding(true)
    try {
      const fullProduct = await getProductById(product.productId)
      if (fullProduct) {
        await addItem(fullProduct, 1, undefined, undefined, {
          isAiRecommended: true,
          sourceBundleId: bundleId,
        })
        setAdded(true)
      }
    } finally {
      setAdding(false)
    }
  }

  return (
    <div className="bg-surface-container-lowest rounded-[24px] overflow-hidden product-card-shadow soft-shadow transition-all duration-300 hover:soft-shadow-hover">
      <div className="relative">
        <img
          src={product.imageUrl || 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=600&h=800&fit=crop'}
          alt={product.name}
          className="w-full aspect-[3/4] object-cover"
        />
        {matchPercent != null && (
          <div className="absolute top-3 left-3 bg-ai-lavender/90 backdrop-blur-sm text-ai-indigo text-xs font-semibold px-2.5 py-1 rounded-full flex items-center gap-1 animate-pulse-glow">
            <Sparkles size={10} />
            {matchPercent}% Match
          </div>
        )}
      </div>
      <div className="p-4">
        <h4 className="text-sm font-medium text-primary">{product.name}</h4>
        {product.reason && <p className="text-xs text-on-surface-variant mt-0.5">{product.reason}</p>}
        <div className="flex items-center justify-between mt-3">
          <span className="text-sm font-semibold text-primary">{formatCurrency(product.basePrice)}</span>
          <button
            onClick={handleAddToCart}
            disabled={adding || added}
            className="p-2 rounded-full bg-primary text-on-primary hover:opacity-90 transition-opacity disabled:opacity-40 disabled:cursor-not-allowed"
          >
            {adding ? <Loader2 size={14} className="animate-spin" /> : <ShoppingBag size={14} />}
          </button>
        </div>
      </div>
    </div>
  )
}
