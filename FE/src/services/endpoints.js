const API_GATEWAY = import.meta.env.VITE_API_GATEWAY || import.meta.env.VITE_API_BASE_URL || 'http://localhost:3000/api'

export const ENDPOINTS = {
  AUTH: `${API_GATEWAY}/auth`,
  USERS: `${API_GATEWAY}/users`,
  PRODUCTS: `${API_GATEWAY}/products`,
  INVENTORY: `${API_GATEWAY}/inventory`,
  CART: `${API_GATEWAY}/cart`,
  ORDERS: `${API_GATEWAY}/orders`,
  PAYMENT: `${API_GATEWAY}/payment`,
  AI_STYLIST: `${API_GATEWAY}/ai-stylist`,
  VECTOR_SEARCH: `${API_GATEWAY}/vector-search`,
  KNOWLEDGE_GRAPH: `${API_GATEWAY}/knowledge-graph`,
  RECOMMENDATIONS: `${API_GATEWAY}/recommendations`,
  ANALYTICS: `${API_GATEWAY}/analytics`,
  NOTIFICATIONS: `${API_GATEWAY}/notifications`,
}
