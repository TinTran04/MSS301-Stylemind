import test from 'node:test'
import assert from 'node:assert/strict'

import { formatStatusLabel, normalizeOrderStatus, ORDER_REVENUE_STATUSES } from './orderStatus.js'

test('normalizeOrderStatus maps delivered aliases to completed', () => {
  assert.equal(normalizeOrderStatus('delivered'), 'COMPLETED')
  assert.equal(normalizeOrderStatus('fulfilled'), 'COMPLETED')
  assert.equal(normalizeOrderStatus('completed'), 'COMPLETED')
})

test('formatStatusLabel returns Vietnamese labels and unknown fallback', () => {
  assert.equal(formatStatusLabel('payment_pending'), 'Chờ thanh toán')
  assert.equal(formatStatusLabel('completed'), 'Đã giao thành công')
  assert.equal(formatStatusLabel('does-not-exist'), 'Không xác định')
})

test('revenue statuses only include completed orders', () => {
  assert.equal(ORDER_REVENUE_STATUSES.has('PAYMENT_PENDING'), false)
  assert.equal(ORDER_REVENUE_STATUSES.has('PAID'), false)
  assert.equal(ORDER_REVENUE_STATUSES.has('CONFIRMED'), false)
  assert.equal(ORDER_REVENUE_STATUSES.has('SHIPPED'), false)
  assert.equal(ORDER_REVENUE_STATUSES.has('COMPLETED'), true)
})
