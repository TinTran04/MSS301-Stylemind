import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

export async function getAdminOrders(filters = {}) {
  const params = new URLSearchParams()
  const effectiveFilters = {
    ...filters,
    sort: filters.sort || 'createdAt,desc',
  }
  Object.keys(effectiveFilters).forEach(key => {
    if (effectiveFilters[key] !== undefined && effectiveFilters[key] !== null && effectiveFilters[key] !== '') {
      params.append(key, effectiveFilters[key])
    }
  })
  const qs = params.toString()
  return apiClient.get(`${ENDPOINTS.ADMIN_ORDERS}${qs ? `?${qs}` : ''}`)
}

export async function getAdminOrder(orderId) {
  return apiClient.get(`${ENDPOINTS.ADMIN_ORDERS}/${encodeURIComponent(orderId)}`)
}

export async function updateAdminOrderStatus(id, statusPayload) {
  return apiClient.patch(`${ENDPOINTS.ADMIN_ORDERS}/${id}/status`, statusPayload)
}

export async function adminCancelOrder(orderId, payload) {
  return apiClient.post(`${ENDPOINTS.ADMIN_ORDERS}/${orderId}/cancel`, payload)
}

export async function approveOrderCancellation(id) {
  return apiClient.patch(`${ENDPOINTS.ADMIN_ORDERS}/order-cancellations/${id}/approve`)
}

export async function rejectOrderCancellation(id, payload) {
  return apiClient.patch(`${ENDPOINTS.ADMIN_ORDERS}/order-cancellations/${id}/reject`, payload)
}

export async function getPendingCancellationSummary() {
  return apiClient.get(`${ENDPOINTS.ADMIN_ORDERS}/cancellations/summary`)
}

export async function completeOrderRefund(orderId, refundId, payload) {
  return apiClient.post(`${ENDPOINTS.ADMIN_ORDERS}/${orderId}/refunds/${refundId}/complete`, payload)
}

export async function failOrderRefund(orderId, refundId, payload) {
  return apiClient.post(`${ENDPOINTS.ADMIN_ORDERS}/${orderId}/refunds/${refundId}/fail`, payload)
}

export async function getAdminOrderAnalytics() {
  // If there's an analytics endpoint for orders
  return apiClient.get(`${ENDPOINTS.ANALYTICS}/orders`)
}
