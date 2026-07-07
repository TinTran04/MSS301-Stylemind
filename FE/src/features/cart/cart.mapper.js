const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=600&h=800&fit=crop'

function getImage(images = []) {
  return images.find((image) => image.isPrimary)?.imageUrl || images[0]?.imageUrl || FALLBACK_IMAGE
}

export function mapCartItem(item) {
  if (item.available === false) {
    return {
      id: item.variantId,
      cartItemId: item.id,
      variantId: item.variantId,
      available: false,
      unavailableMessage: item.unavailableMessage || 'Sản phẩm này không còn khả dụng.',
      name: 'Sản phẩm',
      price: 0,
      quantity: item.quantity || 1,
      size: null,
      color: null,
      material: null,
      images: [FALLBACK_IMAGE],
      isAiRecommended: item.isAiRecommended,
      sourceBundleId: item.sourceBundleId,
    }
  }

  const variant = item.variant || {}
  const product = variant.product || {}
  const price = Number(variant.priceOverride || product.basePrice || 0)

  return {
    id: product.id || variant.id || item.variantId,
    cartItemId: item.id,
    variantId: item.variantId,
    availableVariantId: item.variantId,
    available: true,
    name: product.name || variant.sku || 'Sản phẩm',
    price,
    quantity: item.quantity || 1,
    size: variant.size || 'Một cỡ',
    color: variant.color || 'Mặc định',
    material: variant.material || null,
    images: [getImage(product.images)],
    isAiRecommended: item.isAiRecommended,
    sourceBundleId: item.sourceBundleId,
  }
}

export function mapCart(response) {
  return {
    cartId: response?.cartId || null,
    items: (response?.items || []).map(mapCartItem),
    totalAmount: Number(response?.totalAmount || 0),
    totalQuantity: Number(response?.totalQuantity || 0),
  }
}
