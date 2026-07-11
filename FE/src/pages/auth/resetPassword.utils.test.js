import test from 'node:test'
import assert from 'node:assert/strict'

import { resolveResetPasswordContext } from './resetPassword.utils.js'

test('resolveResetPasswordContext prefers query params for admin invite links', () => {
  const context = resolveResetPasswordContext({
    searchParams: new URLSearchParams('email=invite%40example.com&token=abc123'),
    sessionContext: { email: 'ignored@example.com', resetToken: 'session-token' },
  })

  assert.deepEqual(context, {
    mode: 'setup',
    email: 'invite@example.com',
    resetToken: 'abc123',
  })
})

test('resolveResetPasswordContext falls back to session context for forgot-password flow', () => {
  const context = resolveResetPasswordContext({
    searchParams: new URLSearchParams(''),
    sessionContext: { email: 'reset@example.com', resetToken: 'token-123' },
  })

  assert.deepEqual(context, {
    mode: 'reset',
    email: 'reset@example.com',
    resetToken: 'token-123',
  })
})

test('resolveResetPasswordContext returns null when neither query nor session is available', () => {
  assert.equal(
    resolveResetPasswordContext({
      searchParams: new URLSearchParams(''),
      sessionContext: null,
    }),
    null,
  )
})
