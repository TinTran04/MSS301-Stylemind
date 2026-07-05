import test from 'node:test'
import assert from 'node:assert/strict'

import { mapProduct, mapProductPage } from './product.mapper.js'

test('mapProduct exposes only real image and variant values', () => {
  const product = mapProduct({
    id: 'p1',
    categoryId: 3,
    categoryName: 'Áo sơ mi',
    name: 'Oxford Shirt',
    basePrice: 1290000,
    images: [
      { id: 1, imageUrl: 'https://cdn.example/secondary.jpg', isPrimary: false },
      { id: 2, imageUrl: 'https://cdn.example/primary.jpg', isPrimary: true },
    ],
    variants: [
      { id: 'v1', sku: 'OX-S-W', size: 'S', color: 'White', material: 'Cotton' },
      { id: 'v2', sku: 'OX-M-W', size: 'M', color: 'White', material: 'Cotton' },
    ],
  })

  assert.equal(product.primaryImageUrl, 'https://cdn.example/primary.jpg')
  assert.deepEqual(product.colors, ['White'])
  assert.deepEqual(product.sizes, ['S', 'M'])
  assert.equal(product.category, 'Áo sơ mi')
  assert.equal(product.availableVariantId, 'v1')
  assert.equal('rating' in product, false)
  assert.equal('aiMatchScore' in product, false)
})

test('mapProduct keeps missing catalogue data empty', () => {
  const product = mapProduct({
    id: 'p2',
    name: 'Unfinished Product',
    basePrice: 500000,
    images: [],
    variants: [],
  })

  assert.equal(product.primaryImageUrl, null)
  assert.deepEqual(product.colors, [])
  assert.deepEqual(product.sizes, [])
  assert.equal(product.category, '')
  assert.equal(product.material, '')
  assert.equal(product.availableVariantId, null)
})

test('mapProductPage preserves backend pagination metadata', () => {
  const result = mapProductPage({
    content: [{ id: 'p1', name: 'Shirt', basePrice: 100000, images: [], variants: [] }],
    page: 2,
    size: 12,
    totalElements: 37,
    totalPages: 4,
    first: false,
    last: false,
    empty: false,
  })

  assert.equal(result.content[0].id, 'p1')
  assert.equal(result.page, 2)
  assert.equal(result.totalElements, 37)
  assert.equal(result.totalPages, 4)
})
