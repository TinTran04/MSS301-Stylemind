import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getVietnamesePhoneValidationMessage,
  normalizeVietnamesePhoneInput,
} from './phone.utils.js'

test('normalizes common Vietnamese phone separators for the form check', () => {
  assert.equal(normalizeVietnamesePhoneInput('0912 345 678'), '0912345678')
  assert.equal(normalizeVietnamesePhoneInput('+84 (912) 345-678'), '+84912345678')
})

test('accepts local and E.164-shaped Vietnamese phone input', () => {
  assert.equal(getVietnamesePhoneValidationMessage('0912345678'), '')
  assert.equal(getVietnamesePhoneValidationMessage('+84912345678'), '')
})

test('rejects blank and malformed phone input before submitting the form', () => {
  assert.match(getVietnamesePhoneValidationMessage(''), /Vui lòng nhập/)
  assert.match(getVietnamesePhoneValidationMessage('12345'), /Việt Nam hợp lệ/)
  assert.match(getVietnamesePhoneValidationMessage('+14155552671'), /Việt Nam hợp lệ/)
})
