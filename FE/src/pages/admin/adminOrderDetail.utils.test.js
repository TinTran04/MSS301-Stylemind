import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildAdminOrderDetailPath,
  displayValue,
  getOrderItemDisplay,
  getOrderItemLineTotal,
  getOrderPricingSummary,
  getOrderSubtotal,
  resolveAdminOrderBackUrl,
} from './adminOrderDetail.utils.js'

test('buildAdminOrderDetailPath keeps the current admin order-list query', () => {
  assert.equal(
    buildAdminOrderDetailPath('order-123', '?status=PAID&page=2'),
    '/admin/orders/order-123?from=%2Fadmin%2Forders%3Fstatus%3DPAID%26page%3D2',
  )
})

test('resolveAdminOrderBackUrl accepts only same-area relative list URLs', () => {
  assert.equal(resolveAdminOrderBackUrl('?from=%2Fadmin%2Forders%3Fstatus%3DPAID'), '/admin/orders?status=PAID')
  assert.equal(resolveAdminOrderBackUrl('?from=https%3A%2F%2Fevil.example'), '/admin/orders')
})

test('order item totals use the purchased snapshot price', () => {
  const items = [
    { quantity: 2, priceAtPurchase: 299000 },
    { quantity: 1, priceAtPurchase: 150000 },
  ]
  assert.equal(getOrderItemLineTotal(items[0]), 598000)
  assert.equal(getOrderSubtotal(items), 748000)
})

test('getOrderPricingSummary uses backend subtotal tax and shipping without COD rounding', () => {
  const summary = getOrderPricingSummary({
    totalAmount: 1224300,
    subtotalAmount: 1113000,
    shippingFee: 0,
    taxAmount: 111300,
    roundingAdjustment: 0,
  }, [
    { quantity: 1, priceAtPurchase: 1113000 },
  ])

  assert.deepEqual(summary, {
    subtotal: 1113000,
    shippingFee: 0,
    taxAmount: 111300,
    roundingAdjustment: 0,
    exactTotal: 1224300,
    totalAmount: 1224300,
    hasBreakdown: true,
    source: 'backend',
  })
})

test('getOrderPricingSummary falls back to checkout pricing for legacy orders', () => {
  const summary = getOrderPricingSummary({
    paymentMethod: 'cod',
    totalAmount: 379000,
  }, [
    { quantity: 1, priceAtPurchase: 379000 },
  ])

  assert.deepEqual(summary, {
    subtotal: 379000,
    shippingFee: 0,
    taxAmount: 37900,
    roundingAdjustment: 0,
    exactTotal: 416900,
    totalAmount: 416900,
    hasBreakdown: true,
    source: 'fallback',
  })
})

test('displayValue never exposes null-like UI values', () => {
  assert.equal(displayValue(null), 'Chưa có thông tin')
  assert.equal(displayValue(''), 'Chưa có thông tin')
  assert.equal(displayValue('customer@example.com'), 'customer@example.com')
})

test('getOrderItemDisplay maps catalog metadata without confusing SKU and variant reference', () => {
  const display = getOrderItemDisplay({
    variantId: 'SM-ATC018-S-CRM',
    catalogVariantId: 'SM-PRD-018-V01',
    productId: 'SM-PRD-018',
    productName: 'Áo Thun Nữ Cổ Tròn Pastel',
    sku: 'SM-ATC018-S-CRM',
    color: 'Kem',
    size: 'S',
    primaryImageUrl: 'https://example.test/product.jpg',
  })

  assert.deepEqual(display, {
    name: 'Áo Thun Nữ Cổ Tròn Pastel',
    productCode: 'SM-PRD-018',
    variantCode: 'SM-PRD-018-V01',
    sku: 'SM-ATC018-S-CRM',
    color: 'Kem',
    size: 'S',
    material: 'Chưa có thông tin',
    imageUrl: 'https://example.test/product.jpg',
  })
})
