import test from 'node:test'
import assert from 'node:assert/strict'

import { mapOrder } from './order.mapper.js'

test('mapOrder uses real product details and image from order item response', () => {
  const order = mapOrder({
    id: 'order-1',
    orderStatus: 'COMPLETED',
    totalAmount: 687000,
    createdAt: '2026-07-20T07:00:00Z',
    items: [
      {
        id: 'item-1',
        variantId: 'SM-ATC018-S-CRM',
        catalogVariantId: 'SM-PRD-018-V01',
        productId: 'SM-PRD-018',
        productName: 'Ao Thun Nu Co Tron Pastel',
        sku: 'SM-ATC018-S-CRM',
        size: 'S',
        color: 'Kem',
        material: 'Cotton 100%',
        primaryImageUrl: 'https://cdn.example/pastel-tee.jpg',
        priceAtPurchase: 219000,
        quantity: 2,
      },
    ],
  })

  assert.equal(order.items[0].name, 'Ao Thun Nu Co Tron Pastel')
  assert.equal(order.items[0].image, 'https://cdn.example/pastel-tee.jpg')
  assert.equal(order.items[0].imageUrl, 'https://cdn.example/pastel-tee.jpg')
  assert.equal(order.items[0].size, 'S')
  assert.equal(order.items[0].color, 'Kem')
  assert.equal(order.items[0].material, 'Cotton 100%')
  assert.equal(order.items[0].price, 219000)
})

test('mapOrder does not invent a fake shared product image when no image exists', () => {
  const order = mapOrder({
    id: 'order-1',
    orderStatus: 'PROCESSING',
    totalAmount: 100000,
    items: [{ id: 'item-1', variantId: 'var-1', priceAtPurchase: 100000, quantity: 1 }],
  })

  assert.equal(order.items[0].image, null)
  assert.equal(order.items[0].imageUrl, null)
})
