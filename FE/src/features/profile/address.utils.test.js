import test from 'node:test'
import assert from 'node:assert/strict'

import { buildAddressPayload, formatSavedAddress, isCheckoutEligibleAddress } from './address.utils.js'

test('buildAddressPayload submits codes and never submits trusted administrative names', () => {
  assert.deepEqual(buildAddressPayload({
    recipientName: '  Người nhận ',
    phoneNumber: ' 0912345678 ',
    provinceCode: '01',
    wardCode: '00004',
    provinceName: 'Không dùng',
    wardName: 'Không dùng',
    addressLine: '  Số nhà 1 ',
    shippingNote: '  Gọi trước ',
    isDefault: true,
  }), {
    recipientName: 'Người nhận',
    phoneNumber: '0912345678',
    provinceCode: '01',
    wardCode: '00004',
    addressLine: 'Số nhà 1',
    shippingNote: 'Gọi trước',
    isDefault: true,
  })
})

test('legacy addresses are not checkout eligible', () => {
  assert.equal(isCheckoutEligibleAddress({ id: 'legacy', validationStatus: 'LEGACY_UNVERIFIED' }), false)
  assert.equal(isCheckoutEligibleAddress({ id: 'valid', validationStatus: 'VALID' }), true)
})

test('formatSavedAddress uses canonical administrative names', () => {
  assert.equal(formatSavedAddress({
    addressLine: 'Số nhà 1',
    wardName: 'Phường Ba Đình',
    provinceName: 'Thành phố Hà Nội',
  }), 'Số nhà 1, Phường Ba Đình, Thành phố Hà Nội')
})
