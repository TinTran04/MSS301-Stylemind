import test from 'node:test'
import assert from 'node:assert/strict'

import { formatDateTime, parseBackendDate } from './formatDate.js'

test('parseBackendDate treats backend LocalDateTime values as UTC', () => {
  assert.equal(parseBackendDate('2026-07-21T09:43:00').toISOString(), '2026-07-21T09:43:00.000Z')
})

test('formatDateTime renders backend UTC timestamps in Vietnam time', () => {
  assert.match(formatDateTime('2026-07-21T09:43:00'), /16:43/)
  assert.match(formatDateTime('2026-07-21T09:43:00Z'), /16:43/)
})
