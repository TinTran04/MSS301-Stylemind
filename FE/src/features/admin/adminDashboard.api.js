import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

// Real admin-dashboard aggregates. Each owning service exposes its own
// ADMIN-only summary endpoint; the dashboard composes them client-side so a
// single failing service only affects its own card (see AdminDashboardPage).
// apiClient unwraps the ApiResponse envelope, so these resolve to the raw DTO.

export function getOrderSummary() {
  return apiClient.get(`${ENDPOINTS.ADMIN_ORDERS}/summary`)
}

export function getProductSummary() {
  return apiClient.get(`${ENDPOINTS.ADMIN_PRODUCTS}/summary`)
}

export function getUserSummary() {
  return apiClient.get(`${ENDPOINTS.ADMIN_USERS}/summary`)
}

export function getNotificationSummary() {
  return apiClient.get(`${ENDPOINTS.ADMIN_NOTIFICATIONS}/summary`)
}
