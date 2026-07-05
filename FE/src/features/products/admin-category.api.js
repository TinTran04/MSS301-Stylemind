import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

export async function getAdminCategories() {
  const response = await apiClient.get(ENDPOINTS.ADMIN_CATEGORIES)
  return Array.isArray(response) ? response : []
}

export async function createCategory({ name, slug, parentId }) {
  return apiClient.post(ENDPOINTS.ADMIN_CATEGORIES, { name, slug, parentId })
}

export async function updateCategory(id, { name, slug, parentId }) {
  return apiClient.put(`${ENDPOINTS.ADMIN_CATEGORIES}/${id}`, { name, slug, parentId })
}

export async function deleteCategory(id) {
  return apiClient.delete(`${ENDPOINTS.ADMIN_CATEGORIES}/${id}`)
}
