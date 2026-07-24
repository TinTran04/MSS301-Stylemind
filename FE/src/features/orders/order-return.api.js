import apiClient from '../../services/apiClient.js'
import { ENDPOINTS } from '../../services/endpoints.js'

function appendFiles(formData, fieldName, files = []) {
  Array.from(files || []).forEach((file) => {
    if (file) formData.append(fieldName, file)
  })
}

export async function createOrderReturnRequest(orderId, payload, options = {}) {
  const formData = new FormData()
  formData.append('reasonCode', payload.reasonCode)
  if (payload.customerNote) formData.append('customerNote', payload.customerNote)
  appendFiles(formData, 'images', payload.images)
  return apiClient.post(`${ENDPOINTS.ORDERS}/${orderId}/return-requests`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      ...(options.idempotencyKey ? { 'Idempotency-Key': options.idempotencyKey } : {}),
    },
  })
}

export async function submitReturnBankInfo(orderId, returnRequestId, payload) {
  return apiClient.patch(`${ENDPOINTS.ORDERS}/${orderId}/return-requests/${returnRequestId}/bank-info`, payload)
}

export function buildReturnFormData(payload, fileFieldName = 'images') {
  const formData = new FormData()
  Object.entries(payload || {}).forEach(([key, value]) => {
    if (key === fileFieldName || value === undefined || value === null || value === '') return
    formData.append(key, value)
  })
  appendFiles(formData, fileFieldName, payload?.[fileFieldName])
  return formData
}
