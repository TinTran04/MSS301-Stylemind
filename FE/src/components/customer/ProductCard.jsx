import { ShoppingBag } from 'lucide-react'
import { Link } from 'react-router-dom'
import useCartStore from '../../features/cart/cart.store'
import ProductImage from './ProductImage'

const priceFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

export default function ProductCard({ product }) {
  const addItem = useCartStore((state) => state.addItem)
  const hasVariant = Boolean(product.availableVariantId)
  const variantSummary = [
    product.colors.length > 0 ? `${product.colors.length} màu` : null,
    product.sizes.length > 0 ? product.sizes.join(', ') : null,
  ].filter(Boolean).join(' · ')

  const handleAddToCart = (event) => {
    event.preventDefault()
    event.stopPropagation()
    if (hasVariant) addItem(product)
  }

  return (
    <article className="group min-w-0">
      <Link to={`/products/${product.id}`} className="block no-underline">
        <div className="aspect-[3/4] overflow-hidden rounded-lg bg-surface-container">
          <ProductImage
            src={product.primaryImageUrl}
            alt={product.name}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-[1.025]"
          />
        </div>

        <div className="relative min-h-[116px] pt-3 pr-10">
          {product.category ? (
            <p className="mb-1 text-xs text-on-surface-variant">{product.category}</p>
          ) : null}
          <h2 className="text-sm font-medium leading-5 text-primary">{product.name}</h2>
          {variantSummary ? (
            <p className="mt-1 truncate text-xs text-on-surface-variant">{variantSummary}</p>
          ) : null}
          <p className="mt-2 text-sm font-semibold text-primary">
            {priceFormatter.format(product.price)}
          </p>

          <button
            type="button"
            onClick={handleAddToCart}
            disabled={!hasVariant}
            className="absolute right-0 top-3 flex h-8 w-8 items-center justify-center rounded-lg border border-outline-variant/30 text-primary transition-colors hover:bg-surface-container disabled:cursor-not-allowed disabled:opacity-35"
            title={hasVariant ? 'Thêm vào giỏ hàng' : 'Sản phẩm chưa có biến thể'}
            aria-label={hasVariant ? `Thêm ${product.name} vào giỏ hàng` : `${product.name} chưa có biến thể`}
          >
            <ShoppingBag size={15} aria-hidden="true" />
          </button>
        </div>
      </Link>
    </article>
  )
}
