import test from 'node:test'
import assert from 'node:assert/strict'
import { shouldClearAuthForUnauthorized } from './authRedirect.js'

test('clears auth only for token/session 401 responses', () => {
  assert.equal(shouldClearAuthForUnauthorized({ status: 401, errorCode: 'AUTH_TOKEN_EXPIRED' }), true)
  assert.equal(shouldClearAuthForUnauthorized({ status: 401, errorCode: 'AUTH_TOKEN_INVALID' }), true)
  assert.equal(shouldClearAuthForUnauthorized({ status: 401, errorCode: 'AUTH_INVALID_CREDENTIALS', url: '/api/v1/auth/login' }), false)
  assert.equal(shouldClearAuthForUnauthorized({ status: 401, errorCode: 'ORDER_CHECKOUT_UNAUTHORIZED' }), false)
})

test('skipAuthRedirect keeps checkout requests from logging the user out on 401', () => {
  assert.equal(shouldClearAuthForUnauthorized({
    status: 401,
    errorCode: 'AUTH_TOKEN_INVALID',
    skipAuthRedirect: true,
    url: '/api/v1/orders',
  }), false)
})
