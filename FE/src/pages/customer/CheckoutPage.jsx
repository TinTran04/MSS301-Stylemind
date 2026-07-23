import { useState, useEffect, useLayoutEffect } from 'react'
import { motion } from 'framer-motion'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { MapPin, CreditCard, Lock, AlertTriangle, Check, Sparkles, Loader2, ArrowRight, RotateCcw, Banknote } from 'lucide-react'
import usePaymentStore from '../../features/payment/payment.store'
import { useCart } from '../../hooks/useCart'
import { formatCurrency } from '../../utils/formatCurrency'
import OrderCancellationDialog from '../../components/customer/OrderCancellationDialog'
import { requestOrderCancellation } from '../../features/orders/order.api'
import AddressForm, { createEmptyAddressForm } from '../../features/profile/AddressForm'
import { createAddress, getAddresses, getProvinces, getWards } from '../../features/profile/profile.api'
import { buildAddressPayload, formatSavedAddress, isCheckoutEligibleAddress } from '../../features/profile/address.utils'
import { getVietnamesePhoneValidationMessage } from '../../features/profile/phone.utils'
import { calculateTotal, isCodPaymentMethod, TAX_LABEL } from '../../features/cart/cart.utils'
import Modal from '../../components/common/Modal'

const paymentMethods = [
  { id: 'cod', label: 'Thanh toán khi nhận hàng', icon: Banknote, description: 'Thanh toán khi đơn hàng được giao đến bạn' },
  { id: 'sepay', label: 'Thanh toán qua SePay (VietQR)', icon: CreditCard, description: 'Quét mã QR bằng ứng dụng ngân hàng của bạn' },
]

export default function CheckoutPage() {
  const { items, clearCart, loadCart } = useCart()
  const { status, steps, error, method, setMethod, processPayment, resumeSepayPayment, stopPolling, reset, lastOrder } = usePaymentStore()
  const location = useLocation()
  const navigate = useNavigate()
  const resumePaymentOrder = location.state?.resumePaymentOrder || null

  const [addresses, setAddresses] = useState([])
  const [addressLoading, setAddressLoading] = useState(true)
  const [selectedAddressId, setSelectedAddressId] = useState('')
  const [addressError, setAddressError] = useState('')
  const [addressFormOpen, setAddressFormOpen] = useState(false)
  const [addressForm, setAddressForm] = useState(createEmptyAddressForm())
  const [provinces, setProvinces] = useState([])
  const [wards, setWards] = useState([])
  const [addressFormSaving, setAddressFormSaving] = useState(false)
  const [administrativeLoading, setAdministrativeLoading] = useState(false)
  const [cancelModalOpen, setCancelModalOpen] = useState(false)
  const [cancelLoading, setCancelLoading] = useState(false)
  const [cancelError, setCancelError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const displayItems = items
  const {
    subtotal: displaySubtotal,
    shipping,
    tax,
    total,
  } = calculateTotal(displayItems, method)
  const isCod = isCodPaymentMethod(method)
  const awaitingPaymentTotal = status === 'awaiting_confirmation' && lastOrder
    ? Number(lastOrder.total || lastOrder.exactTotal || total)
    : total

  useEffect(() => {
    loadCart()
  }, [loadCart])

  useEffect(() => {
    if (items.length === 0 && status === 'idle' && !resumePaymentOrder) {
      navigate('/shop')
    }
  }, [items.length, status, navigate, resumePaymentOrder])

  useEffect(() => {
    let active = true
    setAddressLoading(true)
    getAddresses()
      .then((list) => {
        if (!active) return
        const nextAddresses = Array.isArray(list) ? list : []
        setAddresses(nextAddresses)
        const preferred = nextAddresses.find((address) => isCheckoutEligibleAddress(address) && address.isDefault)
          || nextAddresses.find(isCheckoutEligibleAddress)
        setSelectedAddressId(preferred?.id || '')
      })
      .catch((err) => {
        if (active) setAddressError(err?.message || 'Không thể tải địa chỉ giao hàng.')
      })
      .finally(() => {
        if (active) setAddressLoading(false)
      })
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    getProvinces()
      .then((list) => { if (active) setProvinces(Array.isArray(list) ? list : []) })
      .catch(() => { if (active) setAddressError('Không thể tải dữ liệu tỉnh/thành phố.') })
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    if (!addressFormOpen || !addressForm.provinceCode) {
      setWards([])
      return undefined
    }
    setAdministrativeLoading(true)
    getWards(addressForm.provinceCode)
      .then((list) => { if (active) setWards(Array.isArray(list) ? list : []) })
      .catch(() => { if (active) setAddressError('Không thể tải dữ liệu phường/xã.') })
      .finally(() => { if (active) setAdministrativeLoading(false) })
    return () => { active = false }
  }, [addressForm.provinceCode, addressFormOpen])

  useLayoutEffect(() => {
    if (location.state?.freshCheckout) {
      reset()
    } else if (resumePaymentOrder) {
      resumeSepayPayment(resumePaymentOrder)
    }
  }, [location.state, reset, resumePaymentOrder, resumeSepayPayment])

  useEffect(() => {
    return () => {
      const currentStatus = usePaymentStore.getState().status
      if (currentStatus === 'success' || currentStatus === 'failed') {
        usePaymentStore.getState().reset()
      }
    }
  }, [])

  // Cart is cleared here (rather than only in the sandbox-confirm handler) so
  // it also happens if a PAID transition is ever observed purely via polling.
  useEffect(() => {
    if (status === 'success') {
      clearCart()
    }
  }, [status, clearCart])

  useEffect(() => stopPolling, [stopPolling])

  const handlePlaceOrder = async () => {
    if (isSubmitting || status === 'processing' || status === 'awaiting_confirmation') return
    if (!selectedAddressId || !addresses.some((address) => address.id === selectedAddressId && isCheckoutEligibleAddress(address))) {
      setAddressError('Vui lòng chọn một địa chỉ giao hàng đã được xác thực.')
      return
    }
    setAddressError('')
    setIsSubmitting(true)
    try {
      const result = await processPayment({
        addressId: selectedAddressId,
        items: displayItems,
        total,
      })
      if (result?.requiresConfirmation) {
        await clearCart()
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleCreateAddress = async (event) => {
    event.preventDefault()
    const phoneError = getVietnamesePhoneValidationMessage(addressForm.phoneNumber)
    if (phoneError) {
      setAddressError(phoneError)
      return
    }
    const payload = buildAddressPayload(addressForm)
    if (!payload.recipientName || !payload.addressLine || !payload.provinceCode || !payload.wardCode) {
      setAddressError('Vui lòng điền đầy đủ thông tin địa chỉ giao hàng.')
      return
    }
    setAddressFormSaving(true)
    setAddressError('')
    try {
      const savedAddress = await createAddress(payload)
      setAddresses((previous) => [...previous.filter((address) => address.id !== savedAddress.id), savedAddress])
      setSelectedAddressId(savedAddress.id)
      setAddressFormOpen(false)
      setAddressForm(createEmptyAddressForm())
    } catch (requestError) {
      setAddressError(requestError?.message || 'Không thể thêm địa chỉ. Vui lòng thử lại.')
    } finally {
      setAddressFormSaving(false)
    }
  }

  const handleTryAgain = () => {
    reset()
  }

  const handleCancelPayment = async (payload) => {
    const cancelledOrderId = lastOrder?.id
    if (!cancelledOrderId) return
    setCancelLoading(true)
    setCancelError('')
    try {
      await requestOrderCancellation(cancelledOrderId, payload, { idempotencyKey: `checkout-cancel-${cancelledOrderId}` })
      stopPolling()
      reset()
      navigate('/orders', {
        replace: true,
        state: { flashMessage: 'Đã hủy thanh toán.', cancelledOrderId },
      })
    } catch (err) {
      if (err?.status === 409) {
        setCancelError('Không thể hủy thanh toán cho đơn hàng này.')
      } else {
        setCancelError('Không thể hủy thanh toán. Vui lòng thử lại.')
      }
    } finally {
      setCancelLoading(false)
    }
  }

  if (items.length === 0 && status === 'idle') {
    return null
  }

  return (
    <div className="max-w-[1440px] mx-auto px-6 md:px-16 py-8">
      <h1 className="font-headline-md text-primary mb-6">Thanh toán</h1>

      {/* Processing State */}
      {status === 'processing' && (
        <div className="max-w-2xl mx-auto py-12">
          <div className="bg-surface-container-lowest rounded-xl p-8 ambient-shadow text-center mb-8">
            <Loader2 size={40} className="text-primary animate-spin mx-auto mb-4" />
            <h2 className="font-title-lg text-primary mb-2">Đang xử lý đơn hàng</h2>
            <p className="text-sm text-on-surface-variant">Vui lòng chờ trong giây lát để chúng tôi hoàn tất giao dịch của bạn...</p>
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

      {/* VietQR / Sandbox Confirmation State */}
      {status === 'awaiting_confirmation' && (
        <motion.div
          initial={{ opacity: 0, scale: 0.96 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.35 }}
          className="max-w-2xl mx-auto py-12"
        >
          <div className="bg-surface-container-lowest rounded-xl p-8 ambient-shadow mb-6">
            <h2 className="font-headline-md text-primary text-center mb-2">Quét mã &amp; chuyển khoản</h2>
            <p className="text-sm text-on-surface-variant text-center mb-4">
              Quét mã VietQR này bằng ứng dụng ngân hàng và chuyển đúng {formatCurrency(awaitingPaymentTotal)}.
            </p>

            {lastOrder?.qrImageUrl ? (
              <img
                src={lastOrder.qrImageUrl}
                alt="Mã thanh toán VietQR"
                className="w-48 h-48 mx-auto rounded-xl border border-outline-variant/20 object-contain mb-4"
              />
            ) : (
              <div className="w-14 h-14 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
                <CreditCard size={28} className="text-primary" />
              </div>
            )}

            {lastOrder?.transferContent && (
              <div className="bg-surface-container-low rounded-xl p-4 mb-4 text-center">
                <p className="text-xs text-on-surface-variant mb-1">Nội dung chuyển khoản (bắt buộc để đối soát)</p>
                <p className="font-mono text-sm text-primary tracking-wide">{lastOrder.transferContent}</p>
              </div>
            )}

                <div className="bg-error-container/20 border border-error/20 rounded-xl p-4 mb-4">
                  <p className="text-xs text-error text-center font-medium">
                    Không chỉnh sửa nội dung chuyển khoản. Nếu thay đổi hoặc bỏ trống, hệ thống sẽ không thể đối soát thanh toán.
                  </p>
                </div>

            {lastOrder?.paymentExpiresAt && (
              <p className="text-xs text-on-surface-variant text-center mb-4">
                Mã QR này hết hạn lúc {new Date(lastOrder.paymentExpiresAt).toLocaleTimeString()}. Đơn hàng sẽ tự động hết hạn nếu chưa thanh toán kịp thời.
              </p>
            )}

            <div className="flex items-center justify-center gap-2 text-sm text-on-surface-variant">
              <Loader2 size={16} className="animate-spin text-primary" />
              Đang chờ ngân hàng ghi nhận chuyển khoản... trang này sẽ tự cập nhật.
            </div>

            <div className="mt-6 flex justify-center">
              <button
                type="button"
                onClick={() => setCancelModalOpen(true)}
                className="inline-flex items-center justify-center rounded-lg border border-outline-variant px-4 py-2 text-sm font-medium text-on-surface-variant hover:bg-surface-container-high transition-colors"
              >
                Hủy thanh toán
              </button>
            </div>
          </div>

          <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow">
            <div className="space-y-4">
              {steps.map((step, idx) => (
                <div key={idx} className="flex items-center gap-4">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 transition-all duration-300 ${
                    step.status === 'completed' ? 'bg-green-status text-white'
                    : step.status === 'processing' ? 'bg-primary text-on-primary animate-pulse'
                    : step.status === 'failed' ? 'bg-error text-white'
                    : 'bg-surface-container-high text-on-surface-variant'
                  }`}>
                    {step.status === 'completed' ? <Check size={14} /> : idx + 1}
                  </div>
                  <span className={`text-sm font-medium ${
                    step.status === 'completed' ? 'text-green-status'
                    : step.status === 'processing' ? 'text-primary'
                    : step.status === 'failed' ? 'text-error'
                    : 'text-on-surface-variant'
                  }`}>{step.label}</span>
                </div>
              ))}
            </div>
          </div>
        </motion.div>
      )}

      <OrderCancellationDialog
        isOpen={cancelModalOpen}
        loading={cancelLoading}
        title="Hủy thanh toán"
        confirmLabel="Hủy đơn"
        cancelLabel="Tiếp tục thanh toán"
        onClose={() => {
          if (!cancelLoading) setCancelModalOpen(false)
        }}
        onConfirm={handleCancelPayment}
      />

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
            <h2 className="font-headline-md text-primary mb-2">Đơn hàng đã được xác nhận!</h2>
            <p className="text-on-surface-variant mb-4">Đơn hàng của bạn đã được đặt thành công.</p>
            <p className="text-sm text-on-surface-variant">
              Mã đơn hàng: <span className="font-mono font-medium text-primary">{lastOrder?.id}</span>
            </p>
          </div>
          <div className="flex gap-4 justify-center">
            <Link
              to="/orders"
              className="bg-primary text-on-primary px-6 py-3 rounded-lg text-sm font-medium hover:opacity-90 transition-opacity inline-flex items-center gap-2 no-underline"
            >
              Theo dõi đơn hàng <ArrowRight size={14} />
            </Link>
            <Link
              to="/shop"
              className="border border-outline-variant text-primary px-6 py-3 rounded-lg text-sm font-medium hover:bg-surface-container-high transition-colors inline-flex items-center gap-2 no-underline"
            >
              Tiếp tục mua sắm
            </Link>
          </div>
        </motion.div>
      )}

      {/* Failure State */}
      {status === 'failed' && (
        <div className="max-w-2xl mx-auto py-12">
          <div className="bg-error-container/30 border border-error/20 rounded-xl p-8 text-center mb-6">
            <AlertTriangle size={40} className="text-error mx-auto mb-4" />
            <h2 className="font-headline-md text-error mb-2">Thanh toán thất bại</h2>
            <p className="text-sm text-on-surface-variant">{error}</p>
          </div>

          {/* Saga Rollback Timeline */}
          <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow mb-6">
            <h3 className="font-label-sm uppercase tracking-wider text-on-surface-variant mb-4">Hoàn tác giao dịch</h3>
            <div className="space-y-3">
              {[
                { label: 'Đơn hàng đã tạo', status: 'completed' },
                { label: 'Giữ tồn kho', status: 'completed' },
                { label: 'Thanh toán thất bại', status: 'failed' },
                { label: 'Hoàn lại tồn kho', status: 'completed' },
                { label: 'Đơn hàng đã hủy', status: 'completed' },
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
              <RotateCcw size={14} /> Thử lại
            </button>
            <Link
              to="/cart"
              className="border border-outline-variant text-primary px-6 py-3 rounded-lg text-sm font-medium hover:bg-surface-container-high transition-colors inline-flex items-center gap-2 no-underline"
            >
              Quay lại giỏ hàng
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
                <h2 className="font-title-lg text-primary">Địa chỉ giao hàng</h2>
              </div>
              {addressLoading ? (
                <div className="rounded-xl bg-surface-container-low px-4 py-5 text-sm text-on-surface-variant">
                  Đang tải địa chỉ giao hàng...
                </div>
              ) : addresses.length === 0 ? (
                <div className="rounded-xl border border-dashed border-outline-variant/30 bg-surface-container-low px-4 py-5 text-sm text-on-surface-variant">
                  <p>Bạn chưa có địa chỉ giao hàng.</p>
                  <button type="button" onClick={() => setAddressFormOpen(true)} className="mt-2 inline-flex font-medium text-primary underline underline-offset-4">Thêm địa chỉ giao hàng</button>
                </div>
              ) : (
                <div className="space-y-3">
                  {addresses.map((address) => {
                    const eligible = isCheckoutEligibleAddress(address)
                    return (
                      <label
                        key={address.id}
                        className={`block rounded-xl border p-4 transition-colors ${
                          !eligible
                            ? 'border-outline-variant/10 bg-surface-container-low opacity-70'
                            : selectedAddressId === address.id
                              ? 'border-primary bg-primary/5'
                              : 'border-outline-variant/20 hover:border-primary/50'
                        }`}
                      >
                        <span className="flex items-start gap-3">
                          <input
                            type="radio"
                            name="shipping-address"
                            value={address.id}
                            checked={selectedAddressId === address.id}
                            disabled={!eligible}
                            onChange={() => { setSelectedAddressId(address.id); setAddressError('') }}
                            className="mt-1 h-4 w-4 text-primary focus:ring-primary"
                          />
                          <span className="min-w-0 flex-1">
                            <span className="flex flex-wrap items-center gap-2 text-sm font-medium text-primary">
                              {address.recipientName}
                              {address.isDefault && <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[11px]">Mặc định</span>}
                              {!eligible && <span className="rounded-full bg-error/10 px-2 py-0.5 text-[11px] text-error">Cần cập nhật</span>}
                            </span>
                            <span className="mt-1 block text-xs text-on-surface-variant">{address.phoneNumber || 'Chưa có số điện thoại'}</span>
                            <span className="mt-1 block text-sm leading-relaxed text-on-surface-variant">{formatSavedAddress(address)}</span>
                          </span>
                        </span>
                      </label>
                    )
                  })}
                  {!addresses.some(isCheckoutEligibleAddress) && (
                    <div className="rounded-xl border border-error/20 bg-error-container/20 px-4 py-3 text-sm text-error">
                      Các địa chỉ hiện có cần được cập nhật trước khi thanh toán.
                      <Link to="/profile" className="ml-1 font-medium underline underline-offset-4">Cập nhật địa chỉ</Link>
                    </div>
                  )}
                  <div className="flex flex-wrap gap-4"><button type="button" onClick={() => setAddressFormOpen(true)} className="inline-flex text-sm font-medium text-primary underline underline-offset-4">Thêm địa chỉ mới</button><Link to="/profile" className="inline-flex text-sm font-medium text-primary underline underline-offset-4">Quản lý địa chỉ</Link></div>
                </div>
              )}
              {addressError && (
                <p role="alert" className="text-xs text-error mt-2">{addressError}</p>
              )}
            </div>

            <Modal isOpen={addressFormOpen} onClose={() => { if (!addressFormSaving) setAddressFormOpen(false) }} title="Thêm địa chỉ giao hàng" className="max-w-2xl max-h-[calc(100vh-2rem)] overflow-y-auto">
              <AddressForm form={addressForm} provinces={provinces} wards={wards} loadingAdministrativeData={administrativeLoading} submitting={addressFormSaving} onChange={setAddressForm} onSubmit={handleCreateAddress} onCancel={() => setAddressFormOpen(false)} />
            </Modal>

            {/* Payment Method */}
            <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow">
              <div className="flex items-center gap-2 mb-4">
                <CreditCard size={18} className="text-primary" />
                <h2 className="font-title-lg text-primary">Phương thức thanh toán</h2>
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

              {method === 'sepay' && (
                <div className="mt-4 bg-surface-container-low rounded-xl p-4">
                  <p className="text-xs text-on-surface-variant">
                    Sau khi đặt hàng, hãy quét mã VietQR bằng ứng dụng ngân hàng và chuyển đúng số tiền theo
                    nội dung hiển thị - đừng chỉnh sửa nội dung này. Đơn hàng sẽ tự xác nhận khi SePay ghi nhận
                    giao dịch.
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Right: Order Summary */}
          <div className="lg:col-span-4">
            <div className="sticky top-28 bg-surface-container-lowest rounded-xl p-6 tri-layer-shadow space-y-4">
              <h2 className="font-headline-md text-primary">Tóm tắt đơn hàng</h2>
              <div className="space-y-3">
                {displayItems.map((item, idx) => (
                  <div key={idx} className="flex gap-3">
                    <img src={item.images?.[0] || item.image} alt={item.name} className="w-12 h-14 object-cover rounded-lg" />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-primary truncate">{item.name}</p>
                      <p className="text-xs text-on-surface-variant">Kích cỡ: {item.size} / Màu sắc: {item.color}</p>
                      <p className="text-sm font-semibold text-primary">{formatCurrency(item.price * (item.quantity || 1))}</p>
                    </div>
                  </div>
                ))}
              </div>
              <div className="border-t border-outline-variant/20 pt-4 space-y-2 text-sm">
                <div className="flex justify-between"><span className="text-on-surface-variant">Tạm tính</span><span>{formatCurrency(displaySubtotal)}</span></div>
                <div className="flex justify-between"><span className="text-on-surface-variant">Phí vận chuyển</span><span className={shipping === 0 ? 'text-green-status' : ''}>{shipping === 0 ? 'Miễn phí' : formatCurrency(shipping)}</span></div>
                <div className="flex justify-between"><span className="text-on-surface-variant">{TAX_LABEL}</span><span>{formatCurrency(tax)}</span></div>
                <div className="border-t border-outline-variant/20 pt-2 flex justify-between font-semibold text-primary text-lg">
                  <span>{isCod ? 'Số tiền thu hộ COD' : 'Tổng cộng'}</span><span>{formatCurrency(total)}</span>
                </div>
              </div>
              <button
                onClick={handlePlaceOrder}
                disabled={isSubmitting || status === 'processing' || displayItems.length === 0 || addressLoading || !selectedAddressId || !addresses.some(isCheckoutEligibleAddress)}
                className="w-full bg-primary text-on-primary rounded-lg py-3 text-sm font-medium hover:opacity-90 transition-opacity tracking-[0.1em] uppercase flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isSubmitting || status === 'processing' ? (
                  <>
                    <Loader2 size={16} className="animate-spin" /> Đang xử lý...
                  </>
                ) : (
                  <>
                    <Lock size={14} /> Đặt hàng
                  </>
                )}
              </button>
              <div className="flex items-center justify-center gap-1.5 text-xs text-on-surface-variant">
                <Lock size={12} /> Thanh toán an toàn
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
