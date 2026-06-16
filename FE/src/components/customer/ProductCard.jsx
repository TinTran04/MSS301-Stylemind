import { ShoppingBag, Sparkles } from 'lucide-react'
import { motion } from 'framer-motion'
import { Link } from 'react-router-dom'
import Badge from '../common/Badge'
import useCartStore from '../../features/cart/cart.store'

export default function ProductCard({ product }) {
  const addItem = useCartStore((s) => s.addItem)

  const handleAddToCart = (e) => {
    e.preventDefault()
    e.stopPropagation()
    addItem(product)
  }

  return (
    <motion.div
      whileHover={{ y: -8 }}
      transition={{ duration: 0.3, ease: 'easeOut' }}
    >
      <Link
        to={`/products/${product.id}`}
        className="block group bg-surface-container-lowest rounded-[24px] overflow-hidden product-card-shadow hover:soft-shadow-hover transition-all duration-300 no-underline"
      >
        <div className="relative aspect-[3/4] overflow-hidden">
          <img
            src={product.images[0]}
            alt={product.name}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
            loading="lazy"
          />
          {product.aiMatchScore >= 90 && (
            <div className="absolute top-4 left-4">
              <Badge variant="ai">
                <Sparkles size={12} />
                {product.aiMatchScore}% Match
              </Badge>
            </div>
          )}
          {product.isNew && (
            <div className="absolute top-4 right-4">
              <Badge variant="primary">New</Badge>
            </div>
          )}
        </div>
        <div className="p-5">
          <div className="flex justify-between items-start mb-1">
            <h3 className="font-title-lg text-primary text-sm">{product.name}</h3>
            <span className="text-sm font-semibold text-primary">${product.price}</span>
          </div>
          <p className="text-xs text-on-surface-variant mb-3">{product.material}</p>
          <button
            onClick={handleAddToCart}
            className="group/btn w-full bg-[#1A1A1A] text-on-primary rounded-lg py-2.5 text-xs font-medium hover:bg-primary active:scale-98 transition-all flex items-center justify-center gap-2"
          >
            <ShoppingBag size={14} className="transform group-hover/btn:-translate-y-0.5 transition-transform duration-300" />
            Add to Bag
          </button>
        </div>
      </Link>
    </motion.div>
  )
}
