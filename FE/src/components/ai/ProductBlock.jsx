import { ShoppingBag, Sparkles, AlertTriangle } from 'lucide-react'
import useCartStore from '../../features/cart/cart.store'
import { mockProducts } from '../../data/mockProducts'
import { mockInventory } from '../../data/mockInventory'

function getStockInfo(productId) {
  const inv = mockInventory.find((i) => i.productId === productId)
  if (!inv) return { available: 0, status: 'out_of_stock' }
  const available = inv.currentStock - inv.reservedStock
  if (available <= 0) return { available: 0, status: 'out_of_stock' }
  if (available <= 5) return { available, status: 'low_stock' }
  return { available, status: 'in_stock' }
}

export default function ProductBlock({ productId, matchScore }) {
  const product = mockProducts.find((p) => p.id === String(productId))
  const addItem = useCartStore((s) => s.addItem)
  const stockInfo = getStockInfo(productId)
  const isOutOfStock = stockInfo.status === 'out_of_stock'

  if (!product) return null

  return (
    <div className={`bg-surface-container-lowest rounded-[24px] overflow-hidden product-card-shadow soft-shadow transition-all duration-300 hover:soft-shadow-hover ${isOutOfStock ? 'opacity-60' : ''}`}>
      <div className="relative">
        <img
          src={product.images[0]}
          alt={product.name}
          className="w-full aspect-[3/4] object-cover"
        />
        {matchScore && (
          <div className="absolute top-3 left-3 bg-ai-lavender/90 backdrop-blur-sm text-ai-indigo text-xs font-semibold px-2.5 py-1 rounded-full flex items-center gap-1 animate-pulse-glow">
            <Sparkles size={10} />
            {matchScore}% Match
          </div>
        )}
        <div className="absolute top-3 right-3">
          {isOutOfStock ? (
            <span className="bg-error-container text-error text-[10px] font-semibold px-2 py-0.5 rounded-full">Out of Stock</span>
          ) : stockInfo.status === 'low_stock' ? (
            <span className="bg-tertiary-fixed/30 text-tertiary text-[10px] font-semibold px-2 py-0.5 rounded-full">Low Stock</span>
          ) : (
            <span className="bg-green-status/10 text-green-status text-[10px] font-semibold px-2 py-0.5 rounded-full">In Stock</span>
          )}
        </div>
      </div>
      <div className="p-4">
        <h4 className="text-sm font-medium text-primary">{product.name}</h4>
        <p className="text-xs text-on-surface-variant mt-0.5">{product.material}</p>
        <div className="flex items-center justify-between mt-3">
          <span className="text-sm font-semibold text-primary">${product.price}</span>
          <button
            onClick={() => !isOutOfStock && addItem(product)}
            disabled={isOutOfStock}
            className="p-2 rounded-full bg-primary text-on-primary hover:opacity-90 transition-opacity disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <ShoppingBag size={14} />
          </button>
        </div>
      </div>
    </div>
  )
}
