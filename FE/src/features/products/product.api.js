import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=600&h=800&fit=crop'

function uniqueValues(values, fallback) {
  const unique = [...new Set(values.filter(Boolean))]
  return unique.length > 0 ? unique : fallback
}

function getPrimaryImage(images = []) {
  return images.find((image) => image.isPrimary)?.imageUrl || images[0]?.imageUrl || FALLBACK_IMAGE
}

function getDisplayPrice(product) {
  const override = product.variants?.find((variant) => variant.priceOverride)?.priceOverride
  return Number(override || product.basePrice || 0)
}

function getAiMatchScore(product) {
  const idSeed = String(product.id || product.name || '').split('').reduce((sum, char) => sum + char.charCodeAt(0), 0)
  return 70 + (idSeed % 29)
}

function isRecentlyCreated(createdAt) {
  if (!createdAt) return false
  const created = new Date(createdAt).getTime()
  if (Number.isNaN(created)) return false
  return Date.now() - created < 1000 * 60 * 60 * 24 * 30
}

export function mapProduct(product) {
  if (!product) return null

  const images = [getPrimaryImage(product.images)]
  const variants = product.variants || []
  const firstVariant = variants[0]

  return {
    ...product,
    id: String(product.id),
    category: product.categoryName || String(product.categoryId || ''),
    price: getDisplayPrice(product),
    images,
    colors: uniqueValues(variants.map((variant) => variant.color), ['Default']),
    sizes: uniqueValues(variants.map((variant) => variant.size), ['One Size']),
    material: firstVariant?.material || product.aestheticStyle || 'Material pending',
    sku: firstVariant?.sku || product.id,
    aiMatchScore: product.aiMatchScore || getAiMatchScore(product),
    isNew: product.isNew ?? isRecentlyCreated(product.createdAt),
    rating: product.rating || 4.6,
    reviews: product.reviews || 0,
    availableVariantId: firstVariant?.id || null,
    variants,
  }
}

function mapProductList(pageResponse) {
  const content = Array.isArray(pageResponse) ? pageResponse : pageResponse?.content || []
  return content.map(mapProduct).filter(Boolean)
}

function toSortParam(sort) {
  const sortMap = {
    price_asc: 'basePrice,asc',
    price_desc: 'basePrice,desc',
    rating: 'createdAt,desc',
    ai_match: 'createdAt,desc',
  }
  return sortMap[sort] || sort || 'createdAt,desc'
}

export async function getProducts(filters = {}) {
  const params = {
    page: filters.page ?? 0,
    size: filters.size ?? 50,
    sort: toSortParam(filters.sort),
  }

  if (filters.search) params.search = filters.search
  if (filters.minPrice != null) params.minPrice = filters.minPrice
  if (filters.maxPrice != null) params.maxPrice = filters.maxPrice
  if (filters.category && !Number.isNaN(Number(filters.category))) {
    params.category = Number(filters.category)
  }

  const response = await apiClient.get(ENDPOINTS.PRODUCTS, { params })
  let products = mapProductList(response)

  if (filters.category && Number.isNaN(Number(filters.category))) {
    products = products.filter((product) => product.category === filters.category)
  }

  if (filters.colors?.length) {
    products = products.filter((product) => product.colors.some((color) => filters.colors.includes(color)))
  }

  return products
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
