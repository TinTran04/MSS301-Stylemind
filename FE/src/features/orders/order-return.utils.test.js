import test from 'node:test'
import assert from 'node:assert/strict'
import {
  canRequestCodReturn,
  formatReturnStatus,
  getOrderStatusDisplay,
  getOrderReturnDisplay,
  getManualBankInfoValidationMessage,
  groupReturnAttachments,
  needsReturnBankInfo,
  toManualBankInfoPayload,
} from './order-return.utils.js'

test('COD return request is only available for completed COD orders without an existing ticket', () => {
  assert.equal(canRequestCodReturn({ status: 'completed', paymentMethod: 'cod' }), true)
  assert.equal(canRequestCodReturn({ status: 'completed', paymentMethod: 'sepay' }), false)
  assert.equal(canRequestCodReturn({ status: 'processing', paymentMethod: 'cod' }), false)
  assert.equal(canRequestCodReturn({ status: 'completed', paymentMethod: 'cod', latestReturnRequest: { id: 'ret-1' } }), false)
})

test('return status labels match the approved Vietnamese flow', () => {
  assert.equal(formatReturnStatus('REQUESTED'), 'Yêu cầu hoàn hàng')
  assert.equal(formatReturnStatus('AWAITING_BANK_INFO'), 'Chờ thông tin ngân hàng')
  assert.equal(formatReturnStatus('BANK_INFO_SUBMITTED'), 'Chờ xử lí hoàn tiền')
  assert.equal(formatReturnStatus('REFUNDED'), 'Đã hoàn hàng')
})

test('return display overrides completed status only while return flow is visible', () => {
  assert.deepEqual(
    getOrderReturnDisplay({ orderStatus: 'COMPLETED', latestReturnRequest: { status: 'REQUESTED' } }),
    { label: 'Yêu cầu hoàn hàng', variant: 'warning', visible: true }
  )
  assert.deepEqual(
    getOrderReturnDisplay({ orderStatus: 'COMPLETED', latestReturnRequest: { status: 'REFUNDED' } }),
    { label: 'Đã hoàn hàng', variant: 'success', visible: true }
  )
  assert.deepEqual(
    getOrderReturnDisplay({ orderStatus: 'COMPLETED', latestReturnRequest: { status: 'REJECTED' } }),
    { label: '', variant: 'default', visible: false }
  )
})

test('order status display uses return status as the primary badge while return flow is visible', () => {
  assert.deepEqual(
    getOrderStatusDisplay({ orderStatus: 'COMPLETED', latestReturnRequest: { status: 'AWAITING_BANK_INFO' } }),
    {
      label: 'Chờ thông tin ngân hàng',
      variant: 'warning',
      visible: true,
      source: 'return',
    }
  )
  assert.deepEqual(
    getOrderStatusDisplay({ orderStatus: 'COMPLETED', latestReturnRequest: { status: 'REFUNDED' } }),
    {
      label: 'Đã hoàn hàng',
      variant: 'success',
      visible: true,
      source: 'return',
    }
  )
})

test('bank info step is shown only after admin approval', () => {
  assert.equal(needsReturnBankInfo({ status: 'AWAITING_BANK_INFO' }), true)
  assert.equal(needsReturnBankInfo({ status: 'REQUESTED' }), false)
})

test('return attachments are grouped by ticket purpose', () => {
  const grouped = groupReturnAttachments({
    attachments: [
      { id: '1', kind: 'CUSTOMER_PROOF' },
      { id: '2', kind: 'ADMIN_REJECTION' },
      { id: '3', kind: 'ADMIN_BILL' },
    ],
  })
  assert.equal(grouped.customerProofs.length, 1)
  assert.equal(grouped.adminRejections.length, 1)
  assert.equal(grouped.adminBills.length, 1)
})

test('manual bank info accepts typed bank name and trims values', () => {
  const form = {
    bankName: '  Vietcombank  ',
    bankAccountNumber: '  0123456789  ',
    bankAccountHolder: '  NGUYEN VAN A  ',
  }

  assert.equal(getManualBankInfoValidationMessage(form), '')
  assert.deepEqual(toManualBankInfoPayload(form), {
    bankName: 'Vietcombank',
    bankAccountNumber: '0123456789',
    bankAccountHolder: 'NGUYEN VAN A',
  })
})

test('manual bank info requires all bank fields before confirmation', () => {
  assert.equal(
    getManualBankInfoValidationMessage({
      bankName: '',
      bankAccountNumber: '0123456789',
      bankAccountHolder: 'NGUYEN VAN A',
    }),
    'Vui lòng nhập đầy đủ tên ngân hàng, số tài khoản và chủ tài khoản.'
  )
})
