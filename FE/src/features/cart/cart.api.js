import apiClient, { getGuestSessionId } from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'
import { mapCart, mapCartItem } from './cart.mapper.js'

export { mapCartItem }

function cartHeaders() {
  return {
    'X-Guest-Session-Id': getGuestSessionId(),
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
