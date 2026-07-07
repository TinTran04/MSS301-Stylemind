import { getSizeOptions, getColorOptions, isVariantAvailable } from './product.variant-selection.js'

function primaryImage(images) {
  return images.find((image) => image.isPrimary) || images[0] || null
}

export function mapProduct(product) {
  if (!product) return null

  const images = Array.isArray(product.images) ? product.images : []
  const variants = Array.isArray(product.variants) ? product.variants : []
  const firstVariant = variants[0] || null
  // Prefer an in-stock/active variant for quick-add flows (product card, AI
  // recommendations) that don't offer a size/color picker — falling back to
  // the first variant only when none is actually available.
  const defaultVariant = variants.find(isVariantAvailable) || firstVariant
  const primary = primaryImage(images)

  return {
    ...product,
    id: String(product.id),
    category: product.categoryName || '',
    price: Number(product.basePrice || 0),
    images,
    primaryImageUrl: primary?.imageUrl || null,
    colors: getColorOptions(variants, null),
    sizes: getSizeOptions(variants, null),
    material: firstVariant?.material || '',
    sku: firstVariant?.sku || '',
    availableVariantId: defaultVariant?.id || null,
    variants,
  }
}

export function mapProductPage(pageResponse) {
  const source = pageResponse || {}
  const content = Array.isArray(source) ? source : source.content || []

  return {
    content: content.map(mapProduct).filter(Boolean),
    page: source.page ?? 0,
    size: source.size ?? content.length,
    totalElements: source.totalElements ?? content.length,
    totalPages: source.totalPages ?? (content.length > 0 ? 1 : 0),
    first: source.first ?? true,
    last: source.last ?? true,
    empty: source.empty ?? content.length === 0,
  }
}
