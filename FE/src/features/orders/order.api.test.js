import test from 'node:test'
import assert from 'node:assert/strict'

import { mapOrder } from './order.mapper.js'
import { ORDER_TIMELINE_STEPS } from './orderStatus.js'

test('mapOrder builds timeline from real status history without marking skipped milestones', () => {
  const order = mapOrder({
    id: 'order-1',
    orderStatus: 'CONFIRMED',
    totalAmount: 150000,
    createdAt: '2026-07-21T09:00:00Z',
    updatedAt: '2026-07-21T09:05:00Z',
    statusHistory: [
      {
        previousStatus: 'PENDING',
        newStatus: 'CONFIRMED',
        timestamp: '2026-07-21T09:05:00Z',
      },
    ],
    items: [],
  })

  const paidStep = order.timeline.find((step) => step.status === 'PAID')
  const confirmedStep = order.timeline.find((step) => step.status === 'CONFIRMED')

  assert.equal(paidStep.completed, false)
  assert.equal(paidStep.date, null)
  assert.equal(confirmedStep.completed, true)
  assert.equal(confirmedStep.date, '2026-07-21T09:05:00Z')
})

test('mapOrder hides payment milestones for COD orders', () => {
  const order = mapOrder({
    id: 'order-cod',
    paymentMethod: 'cod',
    orderStatus: 'CONFIRMED',
    totalAmount: 399000,
    createdAt: '2026-07-21T10:22:00Z',
    updatedAt: '2026-07-21T10:25:00Z',
    statusHistory: [
      {
        previousStatus: 'PENDING',
        newStatus: 'CONFIRMED',
        timestamp: '2026-07-21T10:25:00Z',
      },
    ],
    items: [],
  })

  assert.deepEqual(
    order.timeline.map((step) => step.status),
    ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'COMPLETED'],
  )
  assert.equal(order.timeline.some((step) => step.status === 'PAYMENT_PENDING'), false)
  assert.equal(order.timeline.some((step) => step.status === 'PAID'), false)
  assert.equal(order.timeline.find((step) => step.status === 'PROCESSING').label, 'Đang xử lý / Đóng gói')
  assert.equal(order.timeline.find((step) => step.status === 'COMPLETED').label, 'Đã giao thành công / Đã thanh toán')
})

test('mapOrder keeps payment milestones for Sepay orders', () => {
  const order = mapOrder({
    id: 'order-sepay',
    paymentMethod: 'sepay',
    orderStatus: 'PAYMENT_PENDING',
    totalAmount: 399000,
    createdAt: '2026-07-21T10:22:00Z',
    updatedAt: '2026-07-21T10:25:00Z',
    statusHistory: [],
    items: [],
  })

  assert.deepEqual(order.timeline.map((step) => step.status), ORDER_TIMELINE_STEPS)
})

test('mapOrder preserves backend pricing breakdown for order details', () => {
  const order = mapOrder({
    id: 'order-cod-priced',
    paymentMethod: 'cod',
    orderStatus: 'CONFIRMED',
    subtotalAmount: 1113000,
    shippingFee: 0,
    taxAmount: 111300,
    roundingAdjustment: 0,
    totalAmount: 1224300,
    createdAt: '2026-07-21T10:22:00Z',
    updatedAt: '2026-07-21T10:25:00Z',
    statusHistory: [],
    items: [
      { id: 'item-1', productName: 'Set demo', priceAtPurchase: 1113000, quantity: 1 },
    ],
  })

  assert.equal(order.subtotal, 1113000)
  assert.equal(order.taxAmount, 111300)
  assert.equal(order.roundingAdjustment, 0)
  assert.equal(order.exactTotal, 1224300)
  assert.equal(order.total, 1224300)
  assert.equal(order.hasPricingBreakdown, true)
  assert.equal(order.pricingSource, 'backend')
})

test('mapOrder calculates checkout-style pricing when legacy order lacks breakdown', () => {
  const order = mapOrder({
    id: 'order-legacy-cod',
    paymentMethod: 'cod',
    orderStatus: 'CONFIRMED',
    totalAmount: 379000,
    createdAt: '2026-07-21T11:21:00Z',
    updatedAt: '2026-07-21T11:21:00Z',
    statusHistory: [],
    items: [
      { id: 'item-1', productName: 'Áo Polo', priceAtPurchase: 379000, quantity: 1 },
    ],
  })

  assert.equal(order.subtotal, 379000)
  assert.equal(order.shippingFee, 0)
  assert.equal(order.taxAmount, 37900)
  assert.equal(order.roundingAdjustment, 0)
  assert.equal(order.exactTotal, 416900)
  assert.equal(order.total, 416900)
  assert.equal(order.pricingSource, 'fallback')
})
