import test from 'node:test'
import assert from 'node:assert/strict'

import { getPageItems } from './pagination.utils.js'

test('pagination keeps small page counts complete', () => {
  assert.deepEqual(getPageItems(0, 4), [0, 1, 2, 3])
})

test('pagination uses compact ellipses for large page counts', () => {
  assert.deepEqual(getPageItems(7, 24), [0, 'ellipsis-6', 6, 7, 8, 'ellipsis-23', 23])
})
