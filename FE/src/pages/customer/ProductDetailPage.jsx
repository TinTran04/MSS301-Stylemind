import { useEffect, useState } from 'react'
import { Heart, Share2, ShoppingBag } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import ProductCard from '../../components/customer/ProductCard'
import ProductImage from '../../components/customer/ProductImage'
import useCartStore from '../../features/cart/cart.store'
import { getProductById, getProducts } from '../../features/products/product.api'

const priceFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

export default function ProductDetailPage() {
  const { id } = useParams()
  const [product, setProduct] = useState(null)
  const [recommendations, setRecommendations] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedSize, setSelectedSize] = useState(null)
  const [selectedColor, setSelectedColor] = useState(null)
  const addItem = useCartStore((state) => state.addItem)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')

    Promise.all([
      getProductById(id),
      getProducts({ size: 4, sort: 'newest' }),
    ])
      .then(([detail, products]) => {
        if (cancelled) return
        setProduct(detail)
        setSelectedSize(detail?.sizes?.[0] || null)
        setSelectedColor(detail?.colors?.[0] || null)
        setRecommendations(products.filter((item) => item.id !== id).slice(0, 3))
      })
      .catch((requestError) => {
        if (!cancelled) setError(requestError.message || 'Không thể tải sản phẩm.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [id])

  if (loading) {
    return <div className="mx-auto max-w-[1440px] px-6 py-20 text-center">Đang tải...</div>
  }

  if (error || !product) {
    return (
      <div className="mx-auto max-w-[1440px] px-6 py-20 text-center md:px-16">
        <p className="text-error">{error || 'Không tìm thấy sản phẩm.'}</p>
        <Link to="/shop" className="mt-3 inline-block text-primary hover:underline">
          Quay lại cửa hàng
        </Link>
      </div>
    )
  }

  const hasVariant = Boolean(product.availableVariantId)

  return (
    <main className="mx-auto max-w-[1440px] px-6 py-8 md:px-16">
      <div className="grid grid-cols-1 gap-10 lg:grid-cols-2 lg:gap-14">
        <div className="aspect-[3/4] overflow-hidden rounded-lg bg-surface-container">
          <ProductImage
            src={product.primaryImageUrl}
            alt={product.name}
            className="h-full w-full object-cover"
          />
        </div>

        <div className="space-y-7 lg:pt-4">
          <div className="border-b border-outline-variant/20 pb-6">
            {product.category ? (
              <p className="mb-2 text-sm text-on-surface-variant">{product.category}</p>
            ) : null}
            <h1 className="font-headline-md text-primary">{product.name}</h1>
            <p className="mt-3 text-xl font-semibold text-primary">
              {priceFormatter.format(product.price)}
            </p>
          </div>

          {product.description ? (
            <p className="leading-relaxed text-on-surface-variant">{product.description}</p>
          ) : null}

          {product.colors.length > 0 ? (
            <fieldset>
              <legend className="mb-3 text-xs font-medium uppercase text-on-surface-variant">
                Màu sắc
              </legend>
              <div className="flex flex-wrap gap-2">
                {product.colors.map((color) => (
                  <button
                    key={color}
                    type="button"
                    onClick={() => setSelectedColor(color)}
                    className={`rounded-lg border px-4 py-2 text-sm transition-colors ${
                      selectedColor === color
                        ? 'border-primary bg-primary text-on-primary'
                        : 'border-outline-variant/30 hover:border-primary'
                    }`}
                  >
                    {color}
                  </button>
                ))}
              </div>
            </fieldset>
          ) : null}

          {product.sizes.length > 0 ? (
            <fieldset>
              <legend className="mb-3 text-xs font-medium uppercase text-on-surface-variant">
                Kích cỡ
              </legend>
              <div className="flex flex-wrap gap-2">
                {product.sizes.map((size) => (
                  <button
                    key={size}
                    type="button"
                    onClick={() => setSelectedSize(size)}
                    className={`flex h-11 min-w-11 items-center justify-center rounded-lg border px-3 text-sm transition-colors ${
                      selectedSize === size
                        ? 'border-primary bg-primary text-on-primary'
                        : 'border-outline-variant/30 hover:border-primary'
                    }`}
                  >
                    {size}
                  </button>
                ))}
              </div>
            </fieldset>
          ) : null}

          {!hasVariant ? (
            <div className="bg-error-container/40 px-4 py-3 text-sm font-medium text-error">
              Sản phẩm chưa có biến thể khả dụng.
            </div>
          ) : null}

          <div className="flex gap-3">
            <button
              type="button"
              onClick={() => addItem(product, 1, selectedSize, selectedColor)}
              disabled={!hasVariant}
              className="flex flex-1 items-center justify-center gap-2 rounded-lg bg-primary py-3 text-sm font-medium text-on-primary transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
            >
              <ShoppingBag size={16} aria-hidden="true" />
              {hasVariant ? 'Thêm vào giỏ hàng' : 'Chưa có biến thể'}
            </button>
            <button
              type="button"
              className="flex h-11 w-11 items-center justify-center rounded-lg border border-outline-variant/30 hover:bg-surface-container"
              title="Yêu thích"
              aria-label="Yêu thích"
            >
              <Heart size={18} aria-hidden="true" />
            </button>
            <button
              type="button"
              className="flex h-11 w-11 items-center justify-center rounded-lg border border-outline-variant/30 hover:bg-surface-container"
              title="Chia sẻ"
              aria-label="Chia sẻ"
            >
              <Share2 size={18} aria-hidden="true" />
            </button>
          </div>
        </div>
      </div>

      {recommendations.length > 0 ? (
        <section className="mt-16 border-t border-outline-variant/20 pt-10">
          <h2 className="font-headline-md text-primary">Có thể bạn cũng thích</h2>
          <div className="mt-7 grid grid-cols-2 gap-x-3 gap-y-8 md:grid-cols-3 md:gap-x-5">
            {recommendations.map((recommendation) => (
              <ProductCard key={recommendation.id} product={recommendation} />
            ))}
          </div>
        </section>
      ) : null}
    </main>
  )
}
