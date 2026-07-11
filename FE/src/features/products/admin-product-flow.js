export const CREATE_PRODUCT_STEPS = Object.freeze({
  PRODUCT_INFO: 'product-info',
  VARIANTS: 'variants',
  IMAGES: 'images',
})

export function buildInitialProductPayload(formValues) {
  return {
    ...formValues,
    status: 'INACTIVE',
  }
}

export function canPublishProduct(product) {
  return Array.isArray(product?.variants) && product.variants.length > 0
}

export function getNextCreateStep(currentStep) {
  if (currentStep === CREATE_PRODUCT_STEPS.PRODUCT_INFO) {
    return CREATE_PRODUCT_STEPS.VARIANTS
  }
  return CREATE_PRODUCT_STEPS.IMAGES
}
