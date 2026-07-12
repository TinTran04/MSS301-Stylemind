import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

// Chat API is served by the Python ai-stylist service, proxied by the gateway
// under /api/v1/ai-stylist/sessions/** (rewritten to /api/v1/sessions/** downstream).
const SESSIONS = `${ENDPOINTS.AI_STYLIST}/sessions`

// The LLM agent pipeline can take well over the default 10s client timeout,
// so chat sends get their own generous timeout (gateway route allows 120s).
const CHAT_TIMEOUT_MS = 120000

export async function listSessions(userId) {
  return apiClient.get(SESSIONS, { params: { user_id: userId } })
}

export async function createSession(userId, title) {
  return apiClient.post(SESSIONS, { user_id: userId, title: title || undefined })
}

export async function deleteSession(sessionId) {
  return apiClient.delete(`${SESSIONS}/${sessionId}`)
}

export async function getSessionMessages(sessionId) {
  return apiClient.get(`${SESSIONS}/${sessionId}/messages`)
}

export async function sendChatMessage(sessionId, content) {
  return apiClient.post(
    `${SESSIONS}/${sessionId}/messages`,
    { content },
    { timeout: CHAT_TIMEOUT_MS }
  )
}
