import test from 'node:test'
import assert from 'node:assert/strict'

import {
  getTargetDemographicLabel,
  getTargetDemographicOptions,
  matchesTargetDemographic,
  normalizeTargetDemographic,
} from './product.demographic.js'

test('normalizeTargetDemographic maps common aliases to canonical backend values', () => {
  assert.equal(normalizeTargetDemographic('Nam'), 'MEN')
  assert.equal(normalizeTargetDemographic('nữ'), 'WOMEN')
  assert.equal(normalizeTargetDemographic('Unisex'), 'UNISEX')
})

test('getTargetDemographicLabel returns Vietnamese display labels', () => {
  assert.equal(getTargetDemographicLabel('MEN'), 'Nam')
  assert.equal(getTargetDemographicLabel('women'), 'Nữ')
  assert.equal(getTargetDemographicLabel('UNISEX'), 'Unisex')
})

test('getTargetDemographicOptions exposes the storefront chip labels', () => {
  assert.deepEqual(
    getTargetDemographicOptions().map((item) => item.label),
    ['Tất cả', 'Nam', 'Nữ', 'Unisex'],
  )
})

test('matchesTargetDemographic treats aliases as equivalent', () => {
  assert.equal(matchesTargetDemographic('MEN', 'Nam'), true)
  assert.equal(matchesTargetDemographic('WOMEN', 'Nữ'), true)
  assert.equal(matchesTargetDemographic('UNISEX', 'Unisex'), true)
  assert.equal(matchesTargetDemographic('MEN', 'Nữ'), false)
})

