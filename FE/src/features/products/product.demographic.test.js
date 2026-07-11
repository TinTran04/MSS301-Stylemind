import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getTargetDemographicLabel,
  getTargetDemographicOptions,
  matchesTargetDemographic,
  normalizeTargetDemographic,
} from './product.demographic.js'

test('normalizeTargetDemographic maps common aliases to the English backend enum', () => {
  assert.equal(normalizeTargetDemographic('Nam'), 'MALE')
  assert.equal(normalizeTargetDemographic('nữ'), 'FEMALE')
  assert.equal(normalizeTargetDemographic('Unisex'), 'UNISEX')
})

test('getTargetDemographicLabel returns Vietnamese display labels', () => {
  assert.equal(getTargetDemographicLabel('MALE'), 'Nam')
  assert.equal(getTargetDemographicLabel('female'), 'Nữ')
  assert.equal(getTargetDemographicLabel('UNISEX'), 'Unisex')
})

test('getTargetDemographicOptions exposes the storefront chip labels', () => {
  assert.deepEqual(
    getTargetDemographicOptions().map((item) => item.label),
    ['Tất cả', 'Nam', 'Nữ', 'Unisex'],
  )
})

test('matchesTargetDemographic treats aliases as equivalent', () => {
  assert.equal(matchesTargetDemographic('MALE', 'Nam'), true)
  assert.equal(matchesTargetDemographic('FEMALE', 'Nữ'), true)
  assert.equal(matchesTargetDemographic('UNISEX', 'Unisex'), true)
  assert.equal(matchesTargetDemographic('MALE', 'Nữ'), false)
})

