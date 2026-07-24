import test from 'node:test'
import assert from 'node:assert/strict'
import { getAdminNetRevenue, getAdminRevenueMetrics } from './adminRevenue.utils.js'

test('admin net revenue prefers the authoritative netRevenue field', () => {
  assert.equal(getAdminNetRevenue({ netRevenue: 125000, totalRevenue: 999999 }), 125000)
})

test('admin revenue keeps compatibility with older totalRevenue responses', () => {
  assert.equal(getAdminNetRevenue({ totalRevenue: 125000 }), 125000)
})

test('admin revenue metrics use neutral values when optional fields are absent', () => {
  assert.deepEqual(getAdminRevenueMetrics({}), {
    vatCollected: 0,
    shippingFeesCollected: 0,
    grossCustomerPayments: 0,
    refundAmount: 0,
    recognizedOrderCount: 0,
  })
})
