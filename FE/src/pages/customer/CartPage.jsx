import { Link } from 'react-router-dom'
import { ShoppingBag, Sparkles, ArrowRight } from 'lucide-react'
import Badge from '../../components/common/Badge'
import CartItem from '../../components/customer/CartItem'
import useCartStore from '../../features/cart/cart.store'
import { useCart } from '../../hooks/useCart'
import { formatCurrency } from '../../utils/formatCurrency'
import { mockProducts } from '../../data/mockProducts'

export default function CartPage() {
  const { items, subtotal } = useCart()
  const addItem = useCartStore((s) => s.addItem)

  const shipping = subtotal > 200 ? 0 : 15
  const tax = Math.round(subtotal * 0.08 * 100) / 100
  const total = subtotal + shipping + tax

  const curatedOutfit = [mockProducts[0], mockProducts[4]]

  const handleAddOutfit = () => {
    curatedOutfit.forEach((p) => addItem(p))
  }

  return (
    <div className="max-w-[1440px] mx-auto px-6 md:px-16 py-8">
      <h1 className="font-headline-md text-primary mb-8">Shopping Bag</h1>

      {items.length === 0 ? (
        <div className="text-center py-20">
          <ShoppingBag size={48} className="text-on-surface-variant/30 mx-auto mb-4" />
          <p className="text-on-surface-variant mb-6">Your bag is empty</p>
          <Link to="/shop" className="bg-primary text-on-primary px-8 py-3 rounded-lg text-sm font-medium hover:opacity-90 transition-opacity inline-flex items-center gap-2 no-underline">
            Start Shopping <ArrowRight size={16} />
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Cart Items */}
          <div className="lg:col-span-8 space-y-6">
            {/* AI Curated Outfit */}
            <div className="bg-ai-lavender/20 rounded-2xl p-5 border border-ai-lavender/30">
              <div className="flex items-center gap-2 mb-4">
                <Sparkles size={16} className="text-tertiary" />
                <span className="font-label-sm uppercase text-on-surface-variant">AI Curated Outfit</span>
                <Badge variant="ai">Recommended</Badge>
              </div>
              <div className="grid grid-cols-2 gap-4 mb-4">
                {curatedOutfit.map((product) => (
                  <div key={product.id} className="flex gap-3 bg-surface-container-lowest rounded-xl p-3">
                    <img src={product.images[0]} alt={product.name} className="w-16 h-20 object-cover rounded-lg" />
                    <div>
                      <p className="text-sm font-medium text-primary">{product.name}</p>
                      <p className="text-xs text-on-surface-variant">{product.material}</p>
                      <p className="text-sm font-semibold text-primary mt-1">${product.price}</p>
                    </div>
                  </div>
                ))}
              </div>
              <button onClick={handleAddOutfit} className="w-full bg-tertiary-container text-on-primary rounded-lg py-2 text-sm font-medium hover:opacity-90 transition-opacity flex items-center justify-center gap-2">
                <Sparkles size={14} /> Add Outfit to Bag
              </button>
            </div>

            {/* Cart Items */}
            <div>
              <h2 className="font-title-lg text-primary mb-4">Bag Items ({items.length})</h2>
              {items.map((item) => (
                <CartItem key={item.cartItemId} item={item} />
              ))}
            </div>
          </div>

          {/* Order Summary */}
          <div className="lg:col-span-4">
            <div className="sticky top-28 bg-surface-container-lowest rounded-xl p-6 tri-layer-shadow space-y-4">
              <h2 className="font-headline-md text-primary">Order Summary</h2>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-on-surface-variant">Subtotal</span>
                  <span className="text-primary">{formatCurrency(subtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-on-surface-variant">Shipping</span>
                  <span className="text-primary">{shipping === 0 ? 'Free' : formatCurrency(shipping)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-on-surface-variant">Tax</span>
                  <span className="text-primary">{formatCurrency(tax)}</span>
                </div>
                <div className="border-t border-outline-variant/20 pt-3 flex justify-between">
                  <span className="font-semibold text-primary">Total</span>
                  <span className="font-semibold text-primary text-lg">{formatCurrency(total)}</span>
                </div>
              </div>

              {subtotal > 0 && subtotal < 200 && (
                <div className="bg-tertiary-fixed/20 text-tertiary text-xs rounded-lg p-3 text-center">
                  Add {formatCurrency(200 - subtotal)} more for free shipping
                </div>
              )}

              <Link
                to="/checkout"
                className="block w-full bg-primary text-on-primary rounded-lg py-3 text-sm font-medium text-center hover:opacity-90 transition-opacity tracking-[0.1em] uppercase no-underline"
              >
                Proceed to Checkout
              </Link>

              <div className="flex items-center justify-center gap-1.5 text-xs text-on-surface-variant">
                <span className="material-symbols-outlined text-sm">lock</span>
                Secure Checkout
              </div>

              {/* AI Upsell */}
              <div className="bg-surface-container-low rounded-xl p-4 mt-4">
                <div className="flex items-center gap-2 mb-2">
                  <Sparkles size={14} className="text-tertiary" />
                  <span className="text-xs font-medium text-primary">Style Tip</span>
                </div>
                <p className="text-xs text-on-surface-variant">
                  Complete your look with the Structured Wool Blazer - 92% AI Match
                </p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
