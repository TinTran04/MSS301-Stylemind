import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

export async function getAdminProducts({ page = 0, size = 20, search = '', category = '', status = '' } = {}) {
  const params = { page, size }
  if (search) params.search = search
  if (category) params.category = category
  if (status) params.status = status
  return apiClient.get(ENDPOINTS.ADMIN_PRODUCTS, { params })
}

export async function getAdminProductById(id) {
  return apiClient.get(`${ENDPOINTS.ADMIN_PRODUCTS}/${id}`)
}

// payload: { categoryIds, name, description, basePrice, targetDemographic, status }
export async function createProduct(payload) {
  return apiClient.post(ENDPOINTS.ADMIN_PRODUCTS, payload)
}

export async function updateProduct(id, payload) {
  return apiClient.put(`${ENDPOINTS.ADMIN_PRODUCTS}/${id}`, payload)
}

export async function updateProductStatus(id, status) {
  return apiClient.patch(`${ENDPOINTS.ADMIN_PRODUCTS}/${id}/status`, { status })
}

export async function deleteProduct(id) {
  return apiClient.delete(`${ENDPOINTS.ADMIN_PRODUCTS}/${id}`)
}

// variant: { sku, size, color, material, priceOverride, stockQuantity, active }
export async function addVariant(productId, variant) {
  return apiClient.post(`${ENDPOINTS.ADMIN_PRODUCTS}/${productId}/variants`, variant)
}

export async function updateVariant(productId, variantId, variant) {
  return apiClient.put(`${ENDPOINTS.ADMIN_PRODUCTS}/${productId}/variants/${variantId}`, variant)
}

export async function deleteVariant(productId, variantId) {
  return apiClient.delete(`${ENDPOINTS.ADMIN_PRODUCTS}/${productId}/variants/${variantId}`)
}

export async function uploadImage(productId, file, isPrimary = false) {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient.post(
    `${ENDPOINTS.ADMIN_PRODUCTS}/${productId}/images?isPrimary=${isPrimary}`,
    formData,
    // Let the browser set the multipart Content-Type with its boundary — overriding it
    // with a static string here would send the header without a boundary and break parsing.
    { headers: { 'Content-Type': undefined } },
  )
}

export async function deleteImage(productId, imageId) {
  return apiClient.delete(`${ENDPOINTS.ADMIN_PRODUCTS}/${productId}/images/${imageId}`)
}
