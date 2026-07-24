import test from 'node:test'
import assert from 'node:assert/strict'
import {
  canDirectCancel,
  canRequestCancellation,
  formatCancellationReason,
  formatCancellationStatus,
  formatCancellationType,
  formatRefundStatus,
  isCancellationRequested,
  validateCancellationDialogInput,
} from './order-cancellation.utils.js'
import { CUSTOMER_CANCELLATION_REASONS } from './order-cancellation.constants.js'

test('customer cancellation helpers follow order status', () => {
  assert.equal(canDirectCancel({ orderStatus: 'PENDING' }), true)
  assert.equal(canDirectCancel({ orderStatus: 'payment_pending' }), true)
  assert.equal(canDirectCancel({ orderStatus: 'PAID' }), false)
  assert.equal(canRequestCancellation({ orderStatus: 'PAID' }), true)
  assert.equal(canRequestCancellation({ orderStatus: 'PROCESSING' }), true)
  assert.equal(canRequestCancellation({ orderStatus: 'SHIPPED' }), false)
})

test('pending cancellation detection checks either flag or latest request', () => {
  assert.equal(isCancellationRequested({ hasPendingCancellation: true }), true)
  assert.equal(isCancellationRequested({ latestCancellation: { status: 'REQUESTED' } }), true)
  assert.equal(isCancellationRequested({ latestCancellation: { status: 'APPROVED' } }), false)
})

test('customer cannot request cancellation again after admin rejection', () => {
  assert.equal(canRequestCancellation({ orderStatus: 'PROCESSING', latestCancellation: { status: 'REJECTED' } }), false)
})

test('customer cancellation reasons exclude slow delivery', () => {
  assert.equal(CUSTOMER_CANCELLATION_REASONS.some((reason) => reason.value === 'DELIVERY_TOO_SLOW'), false)
})

test('formatters return Vietnamese labels for cancellation and refund', () => {
  assert.equal(formatCancellationStatus('REQUESTED'), 'Đang chờ duyệt')
  assert.equal(formatCancellationType('ADMIN_DIRECT'), 'Quản trị hủy trực tiếp')
  assert.equal(formatCancellationReason('CHANGE_DELIVERY_ADDRESS'), 'Đổi địa chỉ giao hàng')
  assert.equal(formatRefundStatus('REFUND_PENDING'), 'Chờ hoàn tiền')
})

test('cancellation dialog can require admin note for direct admin cancellation', () => {
  assert.equal(
    validateCancellationDialogInput({
      reasonCode: 'DELIVERY_NOT_SUPPORTED',
      note: '',
      noteRequired: true,
      noteRequiredMessage: 'Vui lòng nhập ghi chú admin trước khi hủy đơn.',
    }),
    'Vui lòng nhập ghi chú admin trước khi hủy đơn.'
  )

  assert.equal(
    validateCancellationDialogInput({
      reasonCode: 'DELIVERY_NOT_SUPPORTED',
      note: 'Không hỗ trợ giao đến địa chỉ này',
      noteRequired: true,
    }),
    ''
  )
})
