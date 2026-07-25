import test from 'node:test'
import assert from 'node:assert/strict'

import {
  calculateShipping,
  calculateTax,
  calculateTotal,
  FREE_SHIPPING_THRESHOLD,
  STANDARD_SHIPPING_FEE,
} from './cart.utils.js'

test('calculateTax applies 10 percent VAT rounded to whole VND', () => {
  assert.equal(calculateTax(1113000), 111300)
})

test('calculateShipping is free from the configured threshold', () => {
  assert.equal(calculateShipping(FREE_SHIPPING_THRESHOLD - 1), STANDARD_SHIPPING_FEE)
  assert.equal(calculateShipping(FREE_SHIPPING_THRESHOLD), 0)
})

test('calculateTotal keeps exact COD cash collection without rounding adjustment', () => {
  const summary = calculateTotal([
    { price: 1113000, quantity: 1 },
  ], 'cod')

  assert.equal(summary.subtotal, 1113000)
  assert.equal(summary.shipping, 0)
  assert.equal(summary.tax, 111300)
  assert.equal(summary.exactTotal, 1224300)
  assert.equal(summary.roundingAdjustment, 0)
  assert.equal(summary.total, 1224300)
})

test('calculateTotal keeps exact payable amount for Sepay', () => {
  const summary = calculateTotal([
    { price: 1113000, quantity: 1 },
  ], 'sepay')

  assert.equal(summary.roundingAdjustment, 0)
  assert.equal(summary.total, 1224300)
})
