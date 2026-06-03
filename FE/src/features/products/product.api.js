import { mockProducts } from '../../data/mockProducts'

export async function getProducts(filters = {}) {
  let results = [...mockProducts]
  if (filters.category) {
    results = results.filter(p => p.category === filters.category)
  }
  if (filters.search) {
    const q = filters.search.toLowerCase()
    results = results.filter(p => p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q))
  }
  if (filters.minPrice) results = results.filter(p => p.price >= filters.minPrice)
  if (filters.maxPrice) results = results.filter(p => p.price <= filters.maxPrice)
  if (filters.sort === 'price_asc') results.sort((a, b) => a.price - b.price)
  if (filters.sort === 'price_desc') results.sort((a, b) => b.price - a.price)
  if (filters.sort === 'rating') results.sort((a, b) => b.rating - a.rating)
  if (filters.sort === 'ai_match') results.sort((a, b) => b.aiMatchScore - a.aiMatchScore)
  return results
}

export async function getProductById(id) {
  return mockProducts.find(p => p.id === id) || null
}

export async function searchProducts(query) {
  const q = query.toLowerCase()
  return mockProducts.filter(p => p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q))
}
