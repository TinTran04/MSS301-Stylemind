import test from 'node:test'
import assert from 'node:assert/strict'

import { formatCartVariantSummary, resolveCartItemImage } from './cart.display.js'

test('resolveCartItemImage prefers direct imageUrl fields', () => {
  assert.equal(
    resolveCartItemImage({
      imageUrl: 'https://cdn.example/direct.jpg',
      images: [{ imageUrl: 'https://cdn.example/fallback.jpg', isPrimary: true }],
    }),
    'https://cdn.example/direct.jpg',
  )
})

test('resolveCartItemImage falls back to nested product images', () => {
  assert.equal(
    resolveCartItemImage({
      variant: {
        product: {
          images: [{ imageUrl: 'https://cdn.example/product.jpg', isPrimary: true }],
        },
      },
    }),
    'https://cdn.example/product.jpg',
  )
})

test('resolveCartItemImage returns null when no real image exists', () => {
  assert.equal(resolveCartItemImage({}), null)
})

test('formatCartVariantSummary shows only size and color', () => {
  assert.equal(
    formatCartVariantSummary({
      size: 'S',
      color: 'Trắng',
      material: 'vải',
    }),
    'Kích cỡ: S · Màu sắc: Trắng',
  )
})

test('formatCartVariantSummary falls back when no variant labels exist', () => {
  assert.equal(formatCartVariantSummary({}), 'Phân loại sản phẩm')
})
