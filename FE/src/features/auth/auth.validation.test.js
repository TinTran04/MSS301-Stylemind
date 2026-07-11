import test from 'node:test'
import assert from 'node:assert/strict'

import { getEmailValidationMessage, normalizeEmailInput } from './auth.validation.js'

test('getEmailValidationMessage blocks empty email', () => {
  assert.equal(getEmailValidationMessage(''), 'Vui lòng nhập email.')
  assert.equal(getEmailValidationMessage('   '), 'Vui lòng nhập email.')
})

test('getEmailValidationMessage blocks invalid email format', () => {
  assert.equal(getEmailValidationMessage('abc'), 'Email không hợp lệ.')
  assert.equal(getEmailValidationMessage('abc@'), 'Email không hợp lệ.')
})

test('getEmailValidationMessage accepts a normal email address', () => {
  assert.equal(getEmailValidationMessage('abc@gmail.com'), '')
})

test('normalizeEmailInput trims and lowercases email', () => {
  assert.equal(normalizeEmailInput('  Test@Example.COM '), 'test@example.com')
})
