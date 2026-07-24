import { useState } from 'react'
import { ShoppingCart, Loader2, PackageSearch } from 'lucide-react'
import useCartStore from '../../features/cart/cart.store'
import { getProductById } from '../../features/products/product.api'

// Renders a flat list of product search results returned by search_products tool.
// Each card shows image, name, price and an individual Add-to-Cart button.
export default function ProductListBlock({ products, messageId }) {
  const addItem = useCartStore((s) => s.addItem)
  const [addingId, setAddingId] = useState(null)
  const [addedIds, setAddedIds] = useState([])

  if (!products || products.length === 0) {
    return (
      <div className="flex items-center gap-2 text-xs text-on-surface-variant py-2">
        <PackageSearch size={14} />
        <span>Không tìm thấy sản phẩm phù hợp trong catalog.</span>
      </div>
    )
  }

  const handleAdd = async (product) => {
    const pid = product.product_id
    if (!pid || addingId === pid || addedIds.includes(pid)) return
    setAddingId(pid)
    try {
      const fullProduct = await getProductById(pid)
      if (fullProduct) {
        await addItem(fullProduct, 1, undefined, undefined, {
          isAiRecommended: true,
          sourceBundleId: `${messageId}-search`,
        })
        setAddedIds((prev) => [...prev, pid])
      }
    } catch {
      // silently fail — user can retry
    } finally {
      setAddingId(null)
    }
  }

  const formatPrice = (price) => {
    if (!price) return null
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price)
  }

  return (
    <div className="flex flex-wrap gap-3 max-w-[70%]">
      {products.map((product, idx) => {
        const pid = product.product_id || `p-${idx}`
        const isAdding = addingId === pid
        const isAdded = addedIds.includes(pid)
        const price = formatPrice(product.base_price || product.price)
        const colors = Array.isArray(product.colors)
          ? product.colors
          : product.color
          ? [product.color]
          : []

        return (
          <div
            key={pid}
            className="w-[160px] glass-panel rounded-2xl overflow-hidden flex flex-col"
            style={{ minHeight: 200 }}
          >
            {/* Product Image */}
            <div className="relative bg-surface-container-low" style={{ height: 160 }}>
              {product.image_url || product.primary_image_url ? (
                <img
                  src={product.image_url || product.primary_image_url}
                  alt={product.name}
                  className="w-full h-full object-contain p-2"
                  onError={(e) => {
                    e.currentTarget.style.display = 'none'
                    e.currentTarget.nextSibling.style.display = 'flex'
                  }}
                />
              ) : null}
              {/* Fallback placeholder */}
              <div
                className="w-full h-full flex items-center justify-center text-on-surface-variant/30"
                style={{ display: product.image_url || product.primary_image_url ? 'none' : 'flex' }}
              >
                <PackageSearch size={32} />
              </div>
            </div>

            {/* Info */}
            <div className="flex flex-col gap-1 p-3 flex-1">
              <p className="text-xs font-medium text-on-surface leading-snug line-clamp-2">
                {product.name}
              </p>
              {price && (
                <p className="text-xs font-semibold text-primary">{price}</p>
              )}
              {colors.length > 0 && (
                <p className="text-[11px] text-on-surface-variant">
                  Màu: {colors.slice(0, 3).join(', ')}
                </p>
              )}

              {/* Add to Cart */}
              {pid && (
                <button
                  onClick={() => handleAdd(product)}
                  disabled={isAdding || isAdded}
                  className="mt-auto flex items-center justify-center gap-1.5 w-8 h-8 rounded-full bg-primary text-on-primary hover:opacity-90 transition-opacity disabled:opacity-50 self-end"
                  title={isAdded ? 'Đã thêm vào giỏ' : 'Thêm vào giỏ hàng'}
                >
                  {isAdding
                    ? <Loader2 size={13} className="animate-spin" />
                    : <ShoppingCart size={13} />}
                </button>
              )}
            </div>
          </div>
        )
      })}
    </div>
  )
}
