import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getSizeOptions,
  getColorOptions,
  isOptionOutOfStock,
  isVariantAvailable,
  resolveVariant,
  getDisplayedPrice,
  getAddToCartState,
  getVisibleAddToCartMessage,
} from './product.variant-selection.js'

// Matches the manual verification walkthrough: S/Trắng/10, S/Đen/5, M/Trắng/8,
// M/Đen/0, M/Xanh/3 — plus a duplicate-looking "trắng" to reproduce the
// original bug report ("Colors: trắng, Trắng").
const variants = [
  { id: 'v1', size: 'S', color: 'Trắng', stockQuantity: 10, active: true },
  { id: 'v2', size: 'S', color: 'Đen', stockQuantity: 5, active: true },
  { id: 'v3', size: 'M', color: 'trắng', stockQuantity: 8, active: true },
  { id: 'v4', size: 'M', color: 'Đen', stockQuantity: 0, active: true },
  { id: 'v5', size: 'M', color: 'Xanh', stockQuantity: 3, active: true },
]

test('getSizeOptions(variants, null) derives all unique sizes regardless of any color', () => {
  assert.deepEqual(getSizeOptions(variants, null), ['S', 'M'])
})

test('isVariantAvailable is false when active=false or stockQuantity<=0, true otherwise', () => {
  assert.equal(isVariantAvailable({ active: true, stockQuantity: 5 }), true)
  assert.equal(isVariantAvailable({ active: false, stockQuantity: 5 }), false)
  assert.equal(isVariantAvailable({ active: true, stockQuantity: 0 }), false)
  assert.equal(isVariantAvailable(null), false)
})

test('getColorOptions collapses case/whitespace duplicates like "trắng"/"Trắng" into one option', () => {
  const colors = getColorOptions(variants, null)
  const normalized = colors.map((c) => c.toLowerCase())
  assert.equal(new Set(normalized).size, colors.length, 'no duplicate normalized colors')
  assert.ok(colors.includes('Trắng') || colors.includes('trắng'))
})

test('getColorOptions cross-filters by selected size', () => {
  const colorsForS = getColorOptions(variants, 'S').map((c) => c.toLowerCase()).sort()
  assert.deepEqual(colorsForS, ['trắng', 'đen'].sort())

  const colorsForM = getColorOptions(variants, 'M').map((c) => c.toLowerCase()).sort()
  assert.deepEqual(colorsForM, ['trắng', 'đen', 'xanh'].sort())
})

test('getSizeOptions cross-filters by selected color', () => {
  const sizesForXanh = getSizeOptions(variants, 'Xanh')
  assert.deepEqual(sizesForXanh, ['M'])
})

test('isOptionOutOfStock flags M/Đen (stock 0) but not S/Đen (stock 5)', () => {
  assert.equal(isOptionOutOfStock(variants, 'color', 'Đen', 'size', 'M'), true)
  assert.equal(isOptionOutOfStock(variants, 'color', 'Đen', 'size', 'S'), false)
})

test('resolveVariant finds the exact variant for a valid combination', () => {
  const variant = resolveVariant(variants, 'M', 'Xanh')
  assert.equal(variant.id, 'v5')
})

test('resolveVariant returns null for a combination that does not exist, never a fallback', () => {
  assert.equal(resolveVariant(variants, 'S', 'Xanh'), null)
})

test('getAddToCartState asks for a size first when none is selected', () => {
  const state = getAddToCartState(variants, null, null)
  assert.equal(state.disabled, true)
  assert.equal(state.message, 'Vui lòng chọn kích cỡ.')
  assert.equal(state.variantId, null)
})

test('getAddToCartState asks for a color once a size is selected but no color yet', () => {
  const state = getAddToCartState(variants, 'S', null)
  assert.equal(state.disabled, true)
  assert.equal(state.message, 'Vui lòng chọn màu sắc.')
  assert.equal(state.variantId, null)
})

test('getAddToCartState still asks for a size even if a color-only value is passed', () => {
  const state = getAddToCartState(variants, null, 'Xanh')
  assert.equal(state.disabled, true)
  assert.equal(state.message, 'Vui lòng chọn kích cỡ.')
  assert.equal(state.variantId, null)
})

test('getAddToCartState rejects a combination with no matching variant', () => {
  const state = getAddToCartState(variants, 'S', 'Xanh')
  assert.equal(state.disabled, true)
  assert.equal(state.message, 'Vui lòng chọn phân loại sản phẩm.')
})

test('getAddToCartState rejects an out-of-stock variant (M/Đen)', () => {
  const state = getAddToCartState(variants, 'M', 'Đen')
  assert.equal(state.disabled, true)
  assert.equal(state.message, 'Biến thể này đã hết hàng.')
})

test('getAddToCartState returns the real variantId for a valid, in-stock combination', () => {
  const state = getAddToCartState(variants, 'M', 'Xanh')
  assert.equal(state.disabled, false)
  assert.equal(state.variantId, 'v5')
  assert.equal(state.message, null)
})

test('getDisplayedPrice uses base price when no variant is selected', () => {
  assert.equal(getDisplayedPrice(200000, null), 200000)
})

test('getDisplayedPrice uses selected variant priceOverride when present', () => {
  assert.equal(getDisplayedPrice(200000, { priceOverride: 220000 }), 220000)
})

test('getDisplayedPrice falls back to base price when variant has no override', () => {
  assert.equal(getDisplayedPrice(200000, { priceOverride: null }), 200000)
})

test('getVisibleAddToCartMessage stays silent until the user attempts add to cart', () => {
  assert.equal(getVisibleAddToCartMessage(variants, null, null, false), null)
  assert.equal(getVisibleAddToCartMessage(variants, null, null, true), 'Vui lòng chọn kích cỡ.')
})

// Switching size is a page-level concern (ProductDetailPage always clears
// selectedColor on any size click, same or different), but the helpers it
// relies on must reflect the new size's own colors/variant immediately.
test('switching size changes the available colors to the new size only', () => {
  const colorsForS = getColorOptions(variants, 'S').map((c) => c.toLowerCase()).sort()
  const colorsForM = getColorOptions(variants, 'M').map((c) => c.toLowerCase()).sort()
  assert.notDeepEqual(colorsForS, colorsForM)
  assert.deepEqual(colorsForM, ['trắng', 'đen', 'xanh'].sort())
})

test('a color valid for the previous size does not resolve a variant for the new size', () => {
  // S has no "Xanh" color at all, so switching from S to M and re-selecting
  // the stale "Xanh" color (if the UI failed to clear it) must not resolve.
  assert.equal(resolveVariant(variants, 'S', 'Xanh'), null)
  assert.equal(resolveVariant(variants, 'M', 'Xanh').id, 'v5')
})
