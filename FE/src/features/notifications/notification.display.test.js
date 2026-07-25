import test from 'node:test'
import assert from 'node:assert/strict'

import {
  formatNotificationContent,
  formatNotificationStatus,
  formatNotificationTitle,
  isOrderNotification,
  sortNotificationsNewestFirst,
} from './notification.display.js'

test('formatNotificationTitle maps legacy and typed order notifications', () => {
  assert.equal(formatNotificationTitle({ title: 'Order confirmed', type: 'ORDER_CONFIRMED' }), 'Đơn hàng đã được xác nhận')
  assert.equal(formatNotificationTitle({ type: 'ORDER_CONFIRMED' }), 'Đơn hàng đã được xác nhận')
})

test('formatNotificationContent maps legacy order notification content', () => {
  assert.equal(
    formatNotificationContent({
      title: 'Order confirmed',
      type: 'ORDER_CONFIRMED',
      content: 'Your order 42 has been confirmed and will be paid on delivery.',
    }),
    'Đơn hàng #42 của bạn đã được xác nhận. Bạn sẽ thanh toán khi nhận hàng.',
  )
})

test('formatNotificationStatus normalizes backend status values', () => {
  assert.equal(formatNotificationStatus('sent'), 'Email đã gửi')
  assert.equal(formatNotificationStatus('FAILED'), 'Gửi email lỗi')
})

test('sortNotificationsNewestFirst prefers sentAt over createdAt and breaks ties by id', () => {
  const sorted = sortNotificationsNewestFirst([
    { id: 1, createdAt: '2026-07-21T08:00:00Z', sentAt: '2026-07-21T08:05:00Z' },
    { id: 3, createdAt: '2026-07-21T08:00:00Z', sentAt: '2026-07-21T08:05:00Z' },
    { id: 2, createdAt: '2026-07-21T09:00:00Z', sentAt: null },
  ])

  assert.deepEqual(sorted.map((item) => item.id), [2, 3, 1])
})

test('isOrderNotification treats order and payment notifications as order-related', () => {
  assert.equal(isOrderNotification('ORDER_PAID'), true)
  assert.equal(isOrderNotification('PAYMENT_FAILED'), true)
  assert.equal(isOrderNotification('REGISTER_OTP'), false)
})
