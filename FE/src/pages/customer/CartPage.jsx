import { Link } from 'react-router-dom'
import { ShoppingBag, ArrowRight, Sparkles } from 'lucide-react'
import CartItem from '../../components/customer/CartItem'
import { useCart } from '../../hooks/useCart'
import { formatCurrency } from '../../utils/formatCurrency'

export default function CartPage() {
  const { items, subtotal, loading, error } = useCart()

  const shipping = subtotal > 200 ? 0 : 15
  const tax = Math.round(subtotal * 0.08 * 100) / 100
  const total = subtotal + shipping + tax

  return (
    <div className="max-w-[1440px] mx-auto px-6 md:px-16 py-8">
      <h1 className="font-headline-md text-primary mb-8">Giỏ hàng</h1>

      {loading && items.length === 0 ? (
        <div className="py-20 text-center text-on-surface-variant">Đang tải giỏ hàng của bạn...</div>
      ) : error ? (
        <div role="alert" className="rounded-xl border border-error/20 bg-error-container/30 p-6 text-sm text-error">
          {error}
        </div>
      ) : items.length === 0 ? (
        <div className="text-center py-20">
          <ShoppingBag size={48} className="text-on-surface-variant/30 mx-auto mb-4" />
          <p className="text-on-surface-variant mb-6">Giỏ hàng của bạn đang trống</p>
          <Link to="/shop" className="bg-primary text-on-primary px-8 py-3 rounded-lg text-sm font-medium hover:opacity-90 transition-opacity inline-flex items-center gap-2 no-underline">
            Bắt đầu mua sắm <ArrowRight size={16} />
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Cart Items */}
          <div className="lg:col-span-8 space-y-6">
            {/* Cart Items */}
            <div>
              <h2 className="font-title-lg text-primary mb-4">Sản phẩm trong giỏ ({items.length})</h2>
              {items.map((item) => (
                <CartItem key={item.cartItemId} item={item} />
              ))}
            </div>
          </div>

          {/* Order Summary */}
          <div className="lg:col-span-4">
            <div className="sticky top-28 bg-surface-container-lowest rounded-xl p-6 tri-layer-shadow space-y-4">
              <h2 className="font-headline-md text-primary">Tóm tắt đơn hàng</h2>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-on-surface-variant">Tạm tính</span>
                  <span className="text-primary">{formatCurrency(subtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-on-surface-variant">Phí vận chuyển</span>
                  <span className="text-primary">{shipping === 0 ? 'Miễn phí' : formatCurrency(shipping)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-on-surface-variant">Thuế</span>
                  <span className="text-primary">{formatCurrency(tax)}</span>
                </div>
                <div className="border-t border-outline-variant/20 pt-3 flex justify-between">
                  <span className="font-semibold text-primary">Tổng cộng</span>
                  <span className="font-semibold text-primary text-lg">{formatCurrency(total)}</span>
                </div>
              </div>

              {subtotal > 0 && subtotal < 200 && (
                <div className="bg-tertiary-fixed/20 text-tertiary text-xs rounded-lg p-3 text-center">
                  Thêm {formatCurrency(200 - subtotal)} nữa để được miễn phí vận chuyển
                </div>
              )}

              <Link
                to="/checkout"
                className="block w-full bg-primary text-on-primary rounded-lg py-3 text-sm font-medium text-center hover:opacity-90 transition-opacity tracking-[0.1em] uppercase no-underline"
              >
                Thanh toán
              </Link>

              <div className="flex items-center justify-center gap-1.5 text-xs text-on-surface-variant">
                <span className="material-symbols-outlined text-sm">lock</span>
                Thanh toán an toàn
              </div>

              {/* AI Upsell */}
              <div className="bg-surface-container-low rounded-xl p-4 mt-4">
                <div className="flex items-center gap-2 mb-2">
                  <Sparkles size={14} className="text-tertiary" />
                  <span className="text-xs font-medium text-primary">Gợi ý phong cách</span>
                </div>
                <p className="text-xs text-on-surface-variant">
                  Hoàn thiện outfit của bạn với Structured Wool Blazer - mức độ phù hợp AI 92%
                </p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
