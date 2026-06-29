import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

export async function getAdminProducts(filters = {}) {
  const params = new URLSearchParams()
  Object.keys(filters).forEach(key => {
    if (filters[key] !== undefined && filters[key] !== null && filters[key] !== '') {
      params.append(key, filters[key])
    }
  })
  const qs = params.toString()
  return apiClient.get(`${ENDPOINTS.ADMIN_PRODUCTS}${qs ? `?${qs}` : ''}`)
}

export async function createProduct(payload) {
  return apiClient.post(ENDPOINTS.ADMIN_PRODUCTS, payload)
}

export async function updateProduct(id, payload) {
  return apiClient.put(`${ENDPOINTS.ADMIN_PRODUCTS}/${id}`, payload)
}

export async function deleteProduct(id) {
  return apiClient.delete(`${ENDPOINTS.ADMIN_PRODUCTS}/${id}`)
}
