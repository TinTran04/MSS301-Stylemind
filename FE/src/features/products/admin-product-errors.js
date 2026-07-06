import { CREATE_PRODUCT_STEPS } from './admin-product-flow.js'

// Maps backend product-admin errors to friendly, actionable UI messages WITHOUT
// changing the backend contract. The backend keeps its exact codes/messages
// (e.g. PRODUCT_REQUIRES_VARIANT, LAST_ACTIVE_VARIANT, SKU_EXISTS); this only
// changes how they are presented to the admin. The technical `errorCode` is
// returned too so the UI can show it as a small secondary detail.
//
// apiClient normalizes axios/API errors to { message, errorCode, status }.
// Network failures arrive with status === undefined.

// The real project code is SKU_EXISTS; the others are accepted defensively in
// case the contract is extended, per the task's duplicate-SKU guidance.
const DUPLICATE_SKU_CODES = new Set([
  'SKU_EXISTS',
  'DUPLICATE_SKU',
  'SKU_ALREADY_EXISTS',
  'VARIANT_SKU_EXISTS',
])

export function validateProductFields(product) {
  const errors = {}
  if (!product?.name?.trim()) errors.name = 'Product name is required.'
  if (!Number.isFinite(Number(product?.basePrice)) || Number(product.basePrice) <= 0) {
    errors.basePrice = 'Base price must be greater than 0.'
  }
  if (!product?.categoryId) errors.categoryId = 'Category is required.'
  if (!['ACTIVE', 'INACTIVE', 'DISCONTINUED'].includes(product?.status)) {
    errors.status = 'Select a valid product status.'
  }
  return errors
}

export function validateVariantFields(variant) {
  const errors = {}
  if (!variant?.sku?.trim()) errors.sku = 'SKU is required.'
  if (!variant?.size?.trim()) errors.size = 'Size is required.'
  if (!variant?.color?.trim()) errors.color = 'Color is required.'
  if (variant?.priceOverride !== '' && variant?.priceOverride != null
      && (!Number.isFinite(Number(variant.priceOverride)) || Number(variant.priceOverride) <= 0)) {
    errors.priceOverride = 'Price override must be greater than 0.'
  }
  return errors
}

/**
 * @param {{message?:string, errorCode?:string, status?:number}} error
 * @param {{action?:string, fieldErrors?:object}} [context]
 * @returns {{title:string, message:string, actionLabel?:string, targetStep?:string, fieldErrors?:object, errorCode?:string}}
 */
export function getAdminProductErrorMessage(error, context = {}) {
  const code = error?.errorCode
  const status = error?.status

  // 1) Known business contracts — exact backend codes, friendly presentation.
  if (code === 'PRODUCT_REQUIRES_VARIANT') {
    return {
      title: 'Product cannot be published yet',
      message: 'Add at least one variant before publishing this product.',
      actionLabel: 'Go to Variants',
      targetStep: CREATE_PRODUCT_STEPS.VARIANTS,
      errorCode: code,
    }
  }
  if (code === 'LAST_ACTIVE_VARIANT') {
    return {
      title: 'Cannot delete the final variant',
      message: 'This is the last variant of an active product. Deactivate the product before deleting it.',
      actionLabel: 'Deactivate product first',
      targetStep: CREATE_PRODUCT_STEPS.VARIANTS,
      errorCode: code,
    }
  }
  if (DUPLICATE_SKU_CODES.has(code)) {
    return {
      title: 'SKU already exists',
      message: 'Use a different SKU. Each variant SKU must be unique.',
      targetStep: CREATE_PRODUCT_STEPS.VARIANTS,
      fieldErrors: { sku: 'This SKU is already in use.' },
      errorCode: code,
    }
  }
  if (code === 'CATEGORY_IN_USE') {
    return {
      title: 'Category is still in use',
      message: 'Please reassign those products before deleting this category.',
      errorCode: code,
    }
  }
  if (code === 'CATEGORY_HAS_CHILDREN') {
    return {
      title: 'Category has subcategories',
      message: 'Move or delete its subcategories before deleting this category.',
      errorCode: code,
    }
  }
  if (code === 'SLUG_EXISTS') {
    return {
      title: 'Category slug already exists',
      message: 'Use a different slug for this category.',
      fieldErrors: { slug: 'This category slug is already in use.' },
      errorCode: code,
    }
  }
  if (code === 'VALIDATION_ERROR' && context.action === 'saveVariant') {
    return {
      title: 'Please check the variant information',
      message: 'SKU, size, and color are required. Price override must be greater than 0 if provided.',
      targetStep: CREATE_PRODUCT_STEPS.VARIANTS,
      fieldErrors: context.fieldErrors || {},
      errorCode: code,
    }
  }
  if (code === 'VALIDATION_ERROR') {
    return {
      title: 'Please check the product information',
      message: 'Some required product information is missing or invalid.',
      targetStep: CREATE_PRODUCT_STEPS.PRODUCT_INFO,
      fieldErrors: context.fieldErrors || {},
      errorCode: code,
    }
  }

  // 2) Auth — take precedence over generic/context handling.
  if (status === 401) {
    return {
      title: 'Session expired',
      message: 'Your session has expired. Please sign in again.',
      errorCode: code,
    }
  }
  if (status === 403) {
    return {
      title: 'Permission denied',
      message: 'You do not have permission to perform this admin action.',
      errorCode: code,
    }
  }

  if (context.action === 'loadCategories') {
    return {
      title: 'Could not load categories',
      message: 'Product categories could not be loaded. Please refresh or try again later.',
      actionLabel: 'Retry',
      errorCode: code,
    }
  }

  // 3) Image upload after the product already exists = partial success.
  //    The product must NOT be rolled back; admin can retry from the Images step.
  if (context.action === 'uploadImage') {
    return {
      title: 'Product saved, but image upload failed',
      message: 'The product was created successfully, but one or more images could not be uploaded. You can retry uploading images from this product form.',
      targetStep: CREATE_PRODUCT_STEPS.IMAGES,
      errorCode: code,
    }
  }

  // 4) Network / service unavailable (no status, or 5xx).
  if (status === undefined || status === 0 || status >= 500) {
    return {
      title: 'Service temporarily unavailable',
      message: 'We could not reach the server. Please check your connection or try again.',
      errorCode: code,
    }
  }

  // 5) Fallback — friendly primary; keep the raw server message as the detail.
  return {
    title: 'Something went wrong',
    message: 'The action could not be completed. Please try again.',
    technicalMessage: error?.message,
    errorCode: code,
  }
}
