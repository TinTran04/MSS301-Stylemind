function uniqueValues(values) {
  return [...new Set(values.filter(Boolean))]
}

function primaryImage(images) {
  return images.find((image) => image.isPrimary) || images[0] || null
}

export function mapProduct(product) {
  if (!product) return null

  const images = Array.isArray(product.images) ? product.images : []
  const variants = Array.isArray(product.variants) ? product.variants : []
  const firstVariant = variants[0] || null
  const primary = primaryImage(images)

  return {
    ...product,
    id: String(product.id),
    category: product.categoryName || '',
    price: Number(product.basePrice || 0),
    images,
    primaryImageUrl: primary?.imageUrl || null,
    colors: uniqueValues(variants.map((variant) => variant.color)),
    sizes: uniqueValues(variants.map((variant) => variant.size)),
    material: firstVariant?.material || '',
    sku: firstVariant?.sku || '',
    availableVariantId: firstVariant?.id || null,
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
