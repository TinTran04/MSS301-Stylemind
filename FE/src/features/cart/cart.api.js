import apiClient, { getGuestSessionId } from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=600&h=800&fit=crop'

function cartHeaders() {
  return {
    'X-Guest-Session-Id': getGuestSessionId(),
  }
}

function getImage(images = []) {
  return images.find((image) => image.isPrimary)?.imageUrl || images[0]?.imageUrl || FALLBACK_IMAGE
}

export function mapCartItem(item) {
  const variant = item.variant || {}
  const product = variant.product || {}
  const price = Number(variant.priceOverride || product.basePrice || 0)

  return {
    id: product.id || variant.id || item.variantId,
    cartItemId: item.id,
    variantId: item.variantId,
    availableVariantId: item.variantId,
    name: product.name || variant.sku || 'Product',
    price,
    quantity: item.quantity || 1,
    size: variant.size || 'One Size',
    color: variant.color || 'Default',
    material: variant.material || 'Material pending',
    images: [getImage(product.images)],
    isAiRecommended: item.isAiRecommended,
    sourceBundleId: item.sourceBundleId,
  }
}

function mapCart(response) {
  return {
    cartId: response?.cartId || null,
    items: (response?.items || []).map(mapCartItem),
    totalAmount: Number(response?.totalAmount || 0),
    totalQuantity: Number(response?.totalQuantity || 0),
  }
}

export async function getCart() {
  const response = await apiClient.get(ENDPOINTS.CART, { headers: cartHeaders() })
  return mapCart(response)
}

export async function addToCart(payload) {
  const response = await apiClient.post(ENDPOINTS.CART, payload, { headers: cartHeaders() })
  return mapCart(response)
}

export async function updateCartItem(itemId, quantity) {
  const response = await apiClient.put(`${ENDPOINTS.CART}/${itemId}`, null, {
    headers: cartHeaders(),
    params: { quantity },
  })
  return mapCart(response)
}

export async function removeCartItem(id) {
  await apiClient.delete(`${ENDPOINTS.CART}/${id}`, { headers: cartHeaders() })
  return true
}

export async function mergeCart(guestSessionId) {
  const response = await apiClient.post(`${ENDPOINTS.CART}/merge`, { guestSessionId })
  return mapCart(response)
}
