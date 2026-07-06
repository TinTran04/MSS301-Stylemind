import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getAdminProductErrorMessage,
  validateProductFields,
  validateVariantFields,
} from './admin-product-errors.js'
import { CREATE_PRODUCT_STEPS } from './admin-product-flow.js'

test('PRODUCT_REQUIRES_VARIANT guides admin to add variants', () => {
  const result = getAdminProductErrorMessage({ errorCode: 'PRODUCT_REQUIRES_VARIANT', status: 409 })
  assert.equal(result.title, 'Product cannot be published yet')
  assert.match(result.message, /Add at least one variant/)
  assert.equal(result.actionLabel, 'Go to Variants')
  assert.equal(result.targetStep, CREATE_PRODUCT_STEPS.VARIANTS)
  assert.equal(result.errorCode, 'PRODUCT_REQUIRES_VARIANT')
})

test('LAST_ACTIVE_VARIANT guides admin to deactivate first', () => {
  const result = getAdminProductErrorMessage({ errorCode: 'LAST_ACTIVE_VARIANT', status: 409 })
  assert.equal(result.title, 'Cannot delete the final variant')
  assert.match(result.message, /Deactivate the product before deleting it/)
  assert.equal(result.actionLabel, 'Deactivate product first')
  assert.equal(result.targetStep, CREATE_PRODUCT_STEPS.VARIANTS)
})

test('duplicate SKU (real code SKU_EXISTS) maps to field-level SKU guidance', () => {
  const result = getAdminProductErrorMessage({ errorCode: 'SKU_EXISTS', status: 400 })
  assert.equal(result.title, 'SKU already exists')
  assert.match(result.message, /Each variant SKU must be unique/)
  assert.equal(result.targetStep, CREATE_PRODUCT_STEPS.VARIANTS)
  assert.ok(result.fieldErrors && result.fieldErrors.sku)
})

test('defensive duplicate SKU aliases are also handled', () => {
  for (const code of ['DUPLICATE_SKU', 'SKU_ALREADY_EXISTS', 'VARIANT_SKU_EXISTS']) {
    assert.equal(getAdminProductErrorMessage({ errorCode: code, status: 400 }).title, 'SKU already exists')
  }
})

test('product validation maps to Product Info guidance and field errors', () => {
  const result = getAdminProductErrorMessage(
    { errorCode: 'VALIDATION_ERROR', status: 400 },
    { action: 'saveProduct', fieldErrors: { name: 'Product name is required.' } },
  )
  assert.equal(result.title, 'Please check the product information')
  assert.equal(result.targetStep, CREATE_PRODUCT_STEPS.PRODUCT_INFO)
  assert.equal(result.fieldErrors.name, 'Product name is required.')
})

test('variant validation maps to Variants guidance and preserves field errors', () => {
  const result = getAdminProductErrorMessage(
    { errorCode: 'VALIDATION_ERROR', status: 400 },
    { action: 'saveVariant', fieldErrors: { color: 'Color is required.' } },
  )
  assert.equal(result.title, 'Please check the variant information')
  assert.equal(result.targetStep, CREATE_PRODUCT_STEPS.VARIANTS)
  assert.equal(result.fieldErrors.color, 'Color is required.')
})

test('category loading failure gives retry guidance', () => {
  const result = getAdminProductErrorMessage(
    { message: 'Network Error' },
    { action: 'loadCategories' },
  )
  assert.equal(result.title, 'Could not load categories')
  assert.match(result.message, /refresh or try again later/)
  assert.equal(result.actionLabel, 'Retry')
})

test('image upload failure is framed as partial success', () => {
  const result = getAdminProductErrorMessage({ status: 500 }, { action: 'uploadImage' })
  assert.equal(result.title, 'Product saved, but image upload failed')
  assert.match(result.message, /product was created successfully/)
  assert.equal(result.targetStep, CREATE_PRODUCT_STEPS.IMAGES)
})

test('network error (no status) maps to service unavailable', () => {
  const result = getAdminProductErrorMessage({ message: 'Network Error', errorCode: 'ERR_NETWORK' })
  assert.equal(result.title, 'Service temporarily unavailable')
  assert.match(result.message, /could not reach the server/)
})

test('401 maps to session expired', () => {
  assert.equal(getAdminProductErrorMessage({ status: 401 }).title, 'Session expired')
})

test('403 maps to permission denied', () => {
  assert.equal(getAdminProductErrorMessage({ status: 403 }).title, 'Permission denied')
})

test('auth takes precedence over uploadImage context', () => {
  const result = getAdminProductErrorMessage({ status: 401 }, { action: 'uploadImage' })
  assert.equal(result.title, 'Session expired')
})

test('unknown error uses generic primary copy and retains technical detail separately', () => {
  const result = getAdminProductErrorMessage({ message: 'weird', errorCode: 'ODD', status: 400 })
  assert.equal(result.title, 'Something went wrong')
  assert.equal(result.message, 'The action could not be completed. Please try again.')
  assert.equal(result.technicalMessage, 'weird')
  assert.equal(result.errorCode, 'ODD')
})

test('category conflicts map to actionable category guidance', () => {
  const result = getAdminProductErrorMessage({ errorCode: 'CATEGORY_IN_USE', status: 409 })
  assert.equal(result.title, 'Category is still in use')
  assert.match(result.message, /reassign those products/)
})

test('product field validation identifies required and invalid values', () => {
  assert.deepEqual(validateProductFields({
    name: ' ',
    basePrice: '0',
    categoryId: '',
    status: 'UNKNOWN',
  }), {
    name: 'Product name is required.',
    basePrice: 'Base price must be greater than 0.',
    categoryId: 'Category is required.',
    status: 'Select a valid product status.',
  })
})

test('variant field validation identifies required and invalid values', () => {
  assert.deepEqual(validateVariantFields({
    sku: '',
    size: ' ',
    color: '',
    priceOverride: '-1',
  }), {
    sku: 'SKU is required.',
    size: 'Size is required.',
    color: 'Color is required.',
    priceOverride: 'Price override must be greater than 0.',
  })
})
