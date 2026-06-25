import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Link, useNavigate } from 'react-router-dom'
import { MapPin, CreditCard, Lock, AlertTriangle, Check, Sparkles, Loader2, ArrowRight, RotateCcw, Banknote } from 'lucide-react'
import usePaymentStore from '../../features/payment/payment.store'
import { useCart } from '../../hooks/useCart'
import { formatCurrency } from '../../utils/formatCurrency'

const paymentMethods = [
  { id: 'cod', label: 'Cash on Delivery', icon: Banknote, description: 'Pay when your order arrives' },
  { id: 'online_simulated', label: 'Simulated Online Payment', icon: CreditCard, description: 'Test card ****4242' },
]

export default function CheckoutPage() {
  const { items, subtotal, clearCart } = useCart()
  const { status, steps, error, method, setMethod, processPayment, reset, lastOrder } = usePaymentStore()
  const navigate = useNavigate()

  const [shippingAddress, setShippingAddress] = useState('')
  const [addressError, setAddressError] = useState('')

  const displayItems = items
  const displaySubtotal = subtotal
  const shipping = displaySubtotal > 200 ? 0 : 15
  const tax = Math.round(displaySubtotal * 0.08 * 100) / 100
  const total = displaySubtotal + shipping + tax

  useEffect(() => {
    if (items.length === 0 && status === 'idle') {
      navigate('/shop')
    }
  }, [items.length, status, navigate])

  const handlePlaceOrder = async () => {
    if (!shippingAddress.trim()) {
      setAddressError('Please enter a shipping address.')
      return
    }
    setAddressError('')
    const result = await processPayment({
      shippingAddress: shippingAddress.trim(),
      items: displayItems,
      total,
    })

    if (result.success) {
      await clearCart()
    }
  }

  const handleTryAgain = () => {
    reset()
  }

  if (items.length === 0 && status === 'idle') {
    return null
  }

  return (
    <div className="max-w-[1440px] mx-auto px-6 md:px-16 py-8">
      <h1 className="font-headline-md text-primary mb-6">Checkout</h1>

      {/* Processing State */}
      {status === 'processing' && (
        <div className="max-w-2xl mx-auto py-12">
          <div className="bg-surface-container-lowest rounded-xl p-8 ambient-shadow text-center mb-8">
            <Loader2 size={40} className="text-primary animate-spin mx-auto mb-4" />
            <h2 className="font-title-lg text-primary mb-2">Processing Your Order</h2>
            <p className="text-sm text-on-surface-variant">Please wait while we complete your transaction...</p>
          </div>
          <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow">
            <div className="space-y-4">
              {steps.map((step, idx) => (
                <motion.div
                  key={idx}
                  initial={{ opacity: 0, x: -16 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ duration: 0.4, delay: idx * 0.1 }}
                  className="flex items-center gap-4"
                >
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 transition-all duration-300 ${
                    step.status === 'completed' ? 'bg-green-status text-white'
                    : step.status === 'processing' ? 'bg-primary text-on-primary animate-pulse'
                    : step.status === 'failed' ? 'bg-error text-white'
                    : 'bg-surface-container-high text-on-surface-variant'
                  }`}>
                    {step.status === 'completed' ? <Check size={14} /> : idx + 1}
                  </div>
                  <div className="flex-1">
                    <span className={`text-sm font-medium ${
                      step.status === 'completed' ? 'text-green-status'
                      : step.status === 'processing' ? 'text-primary'
                      : step.status === 'failed' ? 'text-error'
                      : 'text-on-surface-variant'
                    }`}>{step.label}</span>
                  </div>
                  {step.status === 'processing' && <Loader2 size={14} className="text-primary animate-spin" />}
                </motion.div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Success State */}
      {status === 'success' && (
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.5 }}
          className="max-w-2xl mx-auto py-12 text-center"
        >
          <div className="bg-surface-container-lowest rounded-xl p-8 ambient-shadow mb-6">
            <div className="w-16 h-16 bg-green-status/10 rounded-full flex items-center justify-center mx-auto mb-4">
              <Check size={32} className="text-green-status" />
            </div>
            <h2 className="font-headline-md text-primary mb-2">Order Confirmed!</h2>
            <p className="text-on-surface-variant mb-4">Your order has been placed successfully.</p>
            <p className="text-sm text-on-surface-variant">
              Order ID: <span className="font-mono font-medium text-primary">{lastOrder?.id}</span>
            </p>
          </div>
          <div className="flex gap-4 justify-center">
            <Link
              to="/orders"
              className="bg-primary text-on-primary px-6 py-3 rounded-lg text-sm font-medium hover:opacity-90 transition-opacity inline-flex items-center gap-2 no-underline"
            >
              Track Order <ArrowRight size={14} />
            </Link>
            <Link
              to="/shop"
              className="border border-outline-variant text-primary px-6 py-3 rounded-lg text-sm font-medium hover:bg-surface-container-high transition-colors inline-flex items-center gap-2 no-underline"
            >
              Continue Shopping
            </Link>
          </div>
        </motion.div>
      )}

      {/* Failure State */}
      {status === 'failed' && (
        <div className="max-w-2xl mx-auto py-12">
          <div className="bg-error-container/30 border border-error/20 rounded-xl p-8 text-center mb-6">
            <AlertTriangle size={40} className="text-error mx-auto mb-4" />
            <h2 className="font-headline-md text-error mb-2">Payment Failed</h2>
            <p className="text-sm text-on-surface-variant">{error}</p>
          </div>

          {/* Saga Rollback Timeline */}
          <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow mb-6">
            <h3 className="font-label-sm uppercase tracking-wider text-on-surface-variant mb-4">Transaction Rollback</h3>
            <div className="space-y-3">
              {[
                { label: 'Order Created', status: 'completed' },
                { label: 'Stock Reserved', status: 'completed' },
                { label: 'Payment Failed', status: 'failed' },
                { label: 'Stock Released', status: 'completed' },
                { label: 'Order Cancelled', status: 'completed' },
              ].map((step, idx) => (
                <div key={idx} className="flex items-center gap-3">
                  <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold shrink-0 ${
                    step.status === 'completed' ? 'bg-primary text-on-primary'
                    : step.status === 'failed' ? 'bg-error text-white'
                    : 'bg-surface-container-high text-on-surface-variant'
                  }`}>
                    {step.status === 'completed' ? '✓' : step.status === 'failed' ? '!' : idx + 1}
                  </div>
                  <span className={`text-sm ${
                    step.status === 'failed' ? 'text-error font-medium' : 'text-on-surface-variant'
                  }`}>{step.label}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="flex gap-4 justify-center">
            <button
              onClick={handleTryAgain}
              className="bg-primary text-on-primary px-6 py-3 rounded-lg text-sm font-medium hover:opacity-90 transition-opacity inline-flex items-center gap-2"
            >
              <RotateCcw size={14} /> Try Again
            </button>
            <Link
              to="/cart"
              className="border border-outline-variant text-primary px-6 py-3 rounded-lg text-sm font-medium hover:bg-surface-container-high transition-colors inline-flex items-center gap-2 no-underline"
            >
              Back to Cart
            </Link>
          </div>
        </div>
      )}

      {/* Checkout Form */}
      {status === 'idle' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          <div className="lg:col-span-8 space-y-6">
            {/* Delivery */}
            <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow">
              <div className="flex items-center gap-2 mb-4">
                <MapPin size={18} className="text-primary" />
                <h2 className="font-title-lg text-primary">Delivery Address</h2>
              </div>
              <textarea
                rows={3}
                value={shippingAddress}
                onChange={(e) => { setShippingAddress(e.target.value); setAddressError('') }}
                placeholder="Enter your full shipping address (street, city, postal code)"
                className="w-full bg-surface-container-low border border-outline-variant/20 rounded-xl px-4 py-3 text-sm text-on-surface placeholder:text-on-surface-variant/50 focus:outline-none focus:border-tertiary-container resize-none transition-colors"
              />
              {addressError && (
                <p className="text-xs text-error mt-2">{addressError}</p>
              )}
            </div>

            {/* Payment Method */}
            <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow">
              <div className="flex items-center gap-2 mb-4">
                <CreditCard size={18} className="text-primary" />
                <h2 className="font-title-lg text-primary">Payment Method</h2>
              </div>
              <div className="space-y-3">
                {paymentMethods.map((pm) => {
                  const Icon = pm.icon
                  return (
                    <button
                      key={pm.id}
                      onClick={() => setMethod(pm.id)}
                      className={`w-full flex items-center gap-4 p-4 rounded-xl text-left transition-all ${
                        method === pm.id
                          ? 'border-2 border-tertiary-container bg-surface-container-low'
                          : 'border border-outline-variant/20 hover:border-outline-variant'
                      }`}
                    >
                      <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                        method === pm.id ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant'
                      }`}>
                        <Icon size={18} />
                      </div>
                      <div>
                        <p className="text-sm font-medium text-primary">{pm.label}</p>
                        <p className="text-xs text-on-surface-variant">{pm.description}</p>
                      </div>
                    </button>
                  )
                })}
              </div>

              {method === 'online_simulated' && (
                <div className="mt-4 bg-surface-container-low rounded-xl p-4">
                  <p className="text-xs text-on-surface-variant">
                    A secure transaction ID will be generated and processed by our simulated payment gateway.
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Right: Order Summary */}
          <div className="lg:col-span-4">
            <div className="sticky top-28 bg-surface-container-lowest rounded-xl p-6 tri-layer-shadow space-y-4">
              <h2 className="font-headline-md text-primary">Order Summary</h2>
              <div className="space-y-3">
                {displayItems.map((item, idx) => (
                  <div key={idx} className="flex gap-3">
                    <img src={item.images?.[0] || item.image} alt={item.name} className="w-12 h-14 object-cover rounded-lg" />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-primary truncate">{item.name}</p>
                      <p className="text-xs text-on-surface-variant">{item.size} / {item.color}</p>
                      <p className="text-sm font-semibold text-primary">{formatCurrency(item.price * (item.quantity || 1))}</p>
                    </div>
                  </div>
                ))}
              </div>
              <div className="border-t border-outline-variant/20 pt-4 space-y-2 text-sm">
                <div className="flex justify-between"><span className="text-on-surface-variant">Subtotal</span><span>{formatCurrency(displaySubtotal)}</span></div>
                <div className="flex justify-between"><span className="text-on-surface-variant">Shipping</span><span className={shipping === 0 ? 'text-green-status' : ''}>{shipping === 0 ? 'Free' : formatCurrency(shipping)}</span></div>
                <div className="flex justify-between"><span className="text-on-surface-variant">Tax</span><span>{formatCurrency(tax)}</span></div>
                <div className="border-t border-outline-variant/20 pt-2 flex justify-between font-semibold text-primary text-lg">
                  <span>Total</span><span>{formatCurrency(total)}</span>
                </div>
              </div>
              <button
                onClick={handlePlaceOrder}
                disabled={displayItems.length === 0}
                className="w-full bg-primary text-on-primary rounded-lg py-3 text-sm font-medium hover:opacity-90 transition-opacity tracking-[0.1em] uppercase flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Lock size={14} /> Place Order
              </button>
              <div className="flex items-center justify-center gap-1.5 text-xs text-on-surface-variant">
                <Lock size={12} /> Secure Checkout
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
