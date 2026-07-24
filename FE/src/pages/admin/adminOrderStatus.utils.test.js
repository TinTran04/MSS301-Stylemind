import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getAdminOrderStatusOptions,
  getStatusUpdateErrorMessage,
} from './adminOrderStatus.utils.js'

test('status options use backend transitions and exclude the current status', () => {
  assert.deepEqual(
    getAdminOrderStatusOptions({
      orderStatus: 'PAID',
      availableTransitions: ['PROCESSING', 'CANCELLED', 'PAID', 'UNKNOWN'],
    }),
    [
      { value: 'PROCESSING', label: 'Đang xử lý' },
    ],
  )
})

test('terminal orders have no status options', () => {
  assert.deepEqual(getAdminOrderStatusOptions({ orderStatus: 'COMPLETED' }), [])
})

test('COD status options skip payment milestones', () => {
  assert.deepEqual(
    getAdminOrderStatusOptions({
      orderStatus: 'PENDING',
      paymentMethod: 'cod',
      availableTransitions: ['PAYMENT_PENDING', 'CONFIRMED', 'CANCELLED'],
    }),
    [
      { value: 'CONFIRMED', label: 'Đã xác nhận' },
    ],
  )
})

test('conflict errors request a refetch and preserve a Vietnamese message', () => {
  assert.deepEqual(
    getStatusUpdateErrorMessage({ status: 409, errorCode: 'INVALID_ORDER_STATUS_TRANSITION' }),
    {
      title: 'Trạng thái đã thay đổi',
      message: 'Trạng thái đơn hàng đã thay đổi. Dữ liệu sẽ được tải lại.',
      shouldRefetch: true,
    },
  )
})

test('business validation errors preserve the backend-safe message', () => {
  assert.deepEqual(
    getStatusUpdateErrorMessage({ status: 400, message: 'Dữ liệu trạng thái không hợp lệ.' }),
    {
      title: 'Không thể cập nhật trạng thái',
      message: 'Dữ liệu trạng thái không hợp lệ.',
      shouldRefetch: false,
    },
  )
})
