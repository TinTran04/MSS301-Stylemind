import test from 'node:test'
import assert from 'node:assert/strict'

import {
  formatVariantPrice,
  formatVariantStock,
  formatVariantStatus,
  groupVariantsBySize,
  summarizeVariants,
} from './admin-product-variants.js'

test('summarizeVariants reports no-variant state in Vietnamese', () => {
  assert.deepEqual(summarizeVariants([]), {
    countLabel: 'Chưa có biến thể',
    hintLabel: '',
  })
})

test('summarizeVariants shows count and size hint for multiple sizes', () => {
  const result = summarizeVariants([
    { size: 'S', color: 'Trắng' },
    { size: 'M', color: 'Xanh' },
    { size: 'L', color: 'Đen' },
  ])

  assert.equal(result.countLabel, '3 biến thể')
  assert.equal(result.hintLabel, 'S, M')
})

test('summarizeVariants falls back to size/color combos when size alone is not helpful', () => {
  const result = summarizeVariants([
    { size: 'S', color: 'Trắng' },
    { size: 'S', color: 'Đen' },
  ])

  assert.equal(result.countLabel, '2 biến thể')
  assert.equal(result.hintLabel, 'S/Trắng, S/Đen')
})

test('groupVariantsBySize keeps the original order inside each size bucket', () => {
  const groups = groupVariantsBySize([
    { size: 'M', color: 'Xanh' },
    { size: 'S', color: 'Trắng' },
    { size: 'M', color: 'Đen' },
  ])

  assert.equal(groups.length, 2)
  assert.equal(groups[0].size, 'M')
  assert.equal(groups[0].variants.length, 2)
  assert.equal(groups[1].size, 'S')
})

test('formatVariantStock makes zero stock visibly out of stock', () => {
  assert.equal(formatVariantStock(0), 'Hết hàng')
  assert.equal(formatVariantStock(10), 'Còn 10')
})

test('formatVariantPrice and status label variant rows consistently', () => {
  assert.equal(formatVariantPrice({ priceOverride: 199000 }, 379000), '199.000 đ')
  assert.equal(formatVariantPrice({}, 379000), '379.000 đ')
  assert.equal(formatVariantStatus({ active: false }), 'Ngừng bán')
  assert.equal(formatVariantStatus({ active: true }), 'Đang bán')
})
