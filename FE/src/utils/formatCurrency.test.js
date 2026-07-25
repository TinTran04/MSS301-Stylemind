import test from 'node:test'
import assert from 'node:assert/strict'

import { formatCurrency } from './formatCurrency.js'

function normalizeCurrency(text) {
  return text.replace(/\u00a0/g, ' ')
}

test('formatCurrency formats VND without decimals', () => {
  assert.equal(normalizeCurrency(formatCurrency(220000)), '220.000 ₫')
  assert.equal(normalizeCurrency(formatCurrency(237600)), '237.600 ₫')
  assert.equal(normalizeCurrency(formatCurrency(0)), '0 ₫')
})
