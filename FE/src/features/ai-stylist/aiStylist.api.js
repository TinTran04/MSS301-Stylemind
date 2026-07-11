import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

export async function sendChatMessage(message, conversationId) {
  return apiClient.post(`${ENDPOINTS.AI_STYLIST}/chat`, {
    message,
    conversationId: conversationId || undefined,
  })
}

export async function getChatHistory() {
  return apiClient.get(`${ENDPOINTS.AI_STYLIST}/history`)
}

export async function getBundles() {
  return apiClient.get(`${ENDPOINTS.AI_STYLIST}/bundles`)
}
