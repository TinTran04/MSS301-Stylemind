import test from 'node:test'
import assert from 'node:assert/strict'

import { hydrateOrderSummariesWithDetails, mapOrder, mapOrderSummary, mergeOrderSummaryUpdate } from './order.mapper.js'
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

test('mapOrderSummary keeps list responses bounded to summary fields', () => {
  const order = mapOrderSummary({
    id: 'order-summary',
    orderStatus: 'PROCESSING',
    totalAmount: 123000,
    itemCount: 2,
    createdAt: '2026-07-22T10:00:00Z',
  })

  assert.equal(order.id, 'order-summary')
  assert.equal(order.itemCount, 2)
  assert.equal(order.total, 123000)
  assert.deepEqual(order.items, [])
})

test('mergeOrderSummaryUpdate refreshes list return status from order detail', () => {
  const currentList = [
    mapOrderSummary({
      id: 'order-returning',
      orderStatus: 'COMPLETED',
      totalAmount: 350900,
      itemCount: 1,
      createdAt: '2026-07-24T12:32:00Z',
    }),
  ]
  const detail = mapOrder({
    id: 'order-returning',
    orderStatus: 'COMPLETED',
    totalAmount: 350900,
    createdAt: '2026-07-24T12:32:00Z',
    latestReturnRequest: { id: 'return-1', status: 'BANK_INFO_SUBMITTED' },
    items: [{ id: 'item-1', productName: 'Áo Polo', priceAtPurchase: 350900, quantity: 1 }],
  })

  const nextList = mergeOrderSummaryUpdate(currentList, detail)

  assert.equal(nextList[0].latestReturnRequest.status, 'BANK_INFO_SUBMITTED')
  assert.equal(nextList[0].itemCount, 1)
  assert.equal(nextList[0].items.length, 0)
})

test('hydrateOrderSummariesWithDetails refreshes completed cards missing return info', async () => {
  const summaries = [
    mapOrderSummary({
      id: 'order-returning',
      orderStatus: 'COMPLETED',
      totalAmount: 350900,
      itemCount: 1,
      createdAt: '2026-07-24T12:32:00Z',
    }),
    mapOrderSummary({
      id: 'order-processing',
      orderStatus: 'PROCESSING',
      totalAmount: 350900,
      itemCount: 1,
      createdAt: '2026-07-24T12:33:00Z',
    }),
  ]
  const fetchedIds = []
  const hydrated = await hydrateOrderSummariesWithDetails(summaries, async (id) => {
    fetchedIds.push(id)
    return mapOrder({
      id,
      orderStatus: 'COMPLETED',
      totalAmount: 350900,
      createdAt: '2026-07-24T12:32:00Z',
      latestReturnRequest: { id: 'return-1', status: 'BANK_INFO_SUBMITTED' },
      items: [],
    })
  })

  assert.deepEqual(fetchedIds, ['order-returning'])
  assert.equal(hydrated[0].latestReturnRequest.status, 'BANK_INFO_SUBMITTED')
  assert.equal(hydrated[1].latestReturnRequest, null)
})

test('hydrateOrderSummariesWithDetails skips cards that already include return info', async () => {
  const summaries = [
    mapOrderSummary({
      id: 'order-returning',
      orderStatus: 'COMPLETED',
      totalAmount: 350900,
      itemCount: 1,
      createdAt: '2026-07-24T12:32:00Z',
      latestReturnRequest: { id: 'return-1', status: 'REQUESTED' },
    }),
  ]
  let fetchCount = 0
  const hydrated = await hydrateOrderSummariesWithDetails(summaries, async () => {
    fetchCount += 1
    return null
  })

  assert.equal(fetchCount, 0)
  assert.equal(hydrated[0].latestReturnRequest.status, 'REQUESTED')
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

test('mapOrder uses the current monetary field names and discounts before VAT', () => {
  const order = mapOrder({
    id: 'order-current-pricing',
    paymentMethod: 'sepay',
    orderStatus: 'PAYMENT_PENDING',
    productSubtotal: 100000,
    discountAmount: 20000,
    shippingFee: 0,
    taxAmount: 8000,
    totalAmount: 88000,
    createdAt: '2026-07-22T10:00:00Z',
    items: [{ id: 'item-1', productName: 'Áo thử nghiệm', priceAtPurchase: 100000, quantity: 1 }],
  })

  assert.equal(order.subtotal, 100000)
  assert.equal(order.discountAmount, 20000)
  assert.equal(order.taxAmount, 8000)
  assert.equal(order.exactTotal, 88000)
  assert.equal(order.total, 88000)
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
