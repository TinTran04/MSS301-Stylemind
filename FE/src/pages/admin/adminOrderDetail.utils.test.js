import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildAdminOrderDetailPath,
  displayValue,
  getOrderItemDisplay,
  getOrderItemLineTotal,
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
