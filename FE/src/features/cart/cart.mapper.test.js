import test from 'node:test'
import assert from 'node:assert/strict'

import { mapCartItem } from './cart.mapper.js'

test('mapCartItem renders real product name, variant, price from an available item', () => {
  const item = mapCartItem({
    id: 'ci-1',
    variantId: 'v1',
    quantity: 2,
    available: true,
    variant: {
      id: 'v1',
      sku: 'SK-M-BLK',
      size: 'M',
      color: 'Black',
      material: 'Silk',
      priceOverride: null,
      product: {
        id: 'p1',
        name: 'Silk Shirt',
        basePrice: 379000,
        images: [{ imageUrl: 'https://cdn.example/shirt.jpg', isPrimary: true }],
      },
    },
  })

  assert.equal(item.name, 'Silk Shirt')
  assert.equal(item.size, 'M')
  assert.equal(item.color, 'Black')
  assert.equal(item.material, 'Silk')
  assert.equal(item.price, 379000)
  assert.equal(item.imageUrl, 'https://cdn.example/shirt.jpg')
  assert.equal(item.images[0], 'https://cdn.example/shirt.jpg')
  assert.equal(item.available, true)
})

test('mapCartItem does not show "$0" for a valid priced item using priceOverride', () => {
  const item = mapCartItem({
    id: 'ci-1',
    variantId: 'v1',
    quantity: 1,
    available: true,
    variant: {
      priceOverride: 199000,
      product: { name: 'Discounted Tee', basePrice: 250000, images: [] },
    },
  })

  assert.equal(item.price, 199000)
})

test('mapCartItem does not fall back to "Product" when productName is provided', () => {
  const item = mapCartItem({
    id: 'ci-1',
    variantId: 'v1',
    quantity: 1,
    available: true,
    variant: { product: { name: 'Real Product Name', basePrice: 100000, images: [] } },
  })

  assert.equal(item.name, 'Real Product Name')
})

test('mapCartItem falls back to "Product" only when variant/product data is truly missing', () => {
  const item = mapCartItem({ id: 'ci-1', variantId: 'v1', quantity: 1, available: true })

  assert.equal(item.name, 'Sản phẩm')
  assert.equal(item.size, 'Một cỡ')
  assert.equal(item.color, 'Mặc định')
  assert.equal(item.price, 0)
  assert.equal(item.imageUrl, null)
  assert.deepEqual(item.images, [])
})

test('mapCartItem marks unavailable items instead of showing a fake price', () => {
  const item = mapCartItem({
    id: 'ci-1',
    variantId: 'v1',
    quantity: 1,
    available: false,
    unavailableMessage: 'This item is no longer available.',
  })

  assert.equal(item.available, false)
  assert.equal(item.unavailableMessage, 'This item is no longer available.')
  assert.equal(item.price, 0)
})
