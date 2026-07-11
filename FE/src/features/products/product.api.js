import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'
import { mapProduct, mapProductPage } from './product.mapper'

function toSortParam(sort) {
  const sortMap = {
    price_asc: 'basePrice,asc',
    price_desc: 'basePrice,desc',
    newest: 'createdAt,desc',
  }
  return sortMap[sort] || sort || 'createdAt,desc'
}

export async function getProductPage(filters = {}) {
  const params = {
    page: filters.page ?? 0,
    size: filters.size ?? 50,
    sort: toSortParam(filters.sort),
  }

  if (filters.search) params.search = filters.search
  if (filters.minPrice != null) params.minPrice = filters.minPrice
  if (filters.maxPrice != null) params.maxPrice = filters.maxPrice
  if (filters.targetDemographic) params.targetDemographic = filters.targetDemographic
  if (filters.category && !Number.isNaN(Number(filters.category))) {
    params.category = Number(filters.category)
  }

  const response = await apiClient.get(ENDPOINTS.PRODUCTS, { params })
  return mapProductPage(response)
}

export async function getProducts(filters = {}) {
  const page = await getProductPage(filters)
  return page.content
}

export async function getProductById(id) {
  const response = await apiClient.get(`${ENDPOINTS.PRODUCTS}/${id}`)
  return mapProduct(response)
}

export async function searchProducts(query) {
  return getProducts({ search: query })
}

export async function getCategories() {
  const response = await apiClient.get(ENDPOINTS.CATEGORIES)
  return Array.isArray(response) ? response : []
}
