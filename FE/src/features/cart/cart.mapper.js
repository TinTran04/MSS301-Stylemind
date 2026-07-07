import { resolveCartItemImage } from './cart.display.js'

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
      imageUrl: null,
      images: [],
      isAiRecommended: item.isAiRecommended,
      sourceBundleId: item.sourceBundleId,
    }
  }

  const variant = item.variant || {}
  const product = variant.product || {}
  const price = Number(variant.priceOverride || product.basePrice || 0)
  const resolvedImage = resolveCartItemImage({
    imageUrl: item.imageUrl,
    productImageUrl: item.productImageUrl,
    primaryImageUrl: item.primaryImageUrl,
    mainImageUrl: item.mainImageUrl,
    variantInfo: item.variantInfo,
    variant,
    images: item.images,
  }) || resolveCartItemImage(product)

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
    imageUrl: resolvedImage,
    images: resolvedImage ? [resolvedImage] : [],
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
