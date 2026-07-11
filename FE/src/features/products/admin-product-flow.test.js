import test from 'node:test'
import assert from 'node:assert/strict'
import {
  CREATE_PRODUCT_STEPS,
  buildInitialProductPayload,
  canPublishProduct,
  getNextCreateStep,
} from './admin-product-flow.js'

test('initial product payload always requests INACTIVE', () => {
  assert.equal(
    buildInitialProductPayload({ name: 'Shirt', status: 'ACTIVE' }).status,
    'INACTIVE',
  )
})

test('successful product creation advances to variants', () => {
  assert.equal(
    getNextCreateStep(CREATE_PRODUCT_STEPS.PRODUCT_INFO),
    CREATE_PRODUCT_STEPS.VARIANTS,
  )
})

test('variants advance to images', () => {
  assert.equal(
    getNextCreateStep(CREATE_PRODUCT_STEPS.VARIANTS),
    CREATE_PRODUCT_STEPS.IMAGES,
  )
})

test('product cannot publish without variants', () => {
  assert.equal(canPublishProduct({ variants: [] }), false)
  assert.equal(canPublishProduct({}), false)
  assert.equal(canPublishProduct(null), false)
})

test('first persisted variant enables publishing', () => {
  assert.equal(canPublishProduct({ variants: [{ id: 'variant-1' }] }), true)
})
