import test from 'node:test'
import assert from 'node:assert/strict'

import { createCheckoutAttemptKey, isCurrentCheckoutAttempt } from './checkoutAttempt.js'

test('current checkout attempt is tied to its attempt and order', () => {
  const state = { attemptId: 2, activeOrderId: 'order-b' }

  assert.equal(isCurrentCheckoutAttempt(state, 2, 'order-b'), true)
  assert.equal(isCurrentCheckoutAttempt(state, 1, 'order-b'), false)
  assert.equal(isCurrentCheckoutAttempt(state, 2, 'order-a'), false)
})

test('checkout attempt keys are generated for individual attempts', () => {
  const first = createCheckoutAttemptKey()
  const second = createCheckoutAttemptKey()

  assert.notEqual(first, second)
  assert.ok(first.length > 0)
  assert.ok(second.length > 0)
})
