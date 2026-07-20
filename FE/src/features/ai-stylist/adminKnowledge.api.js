import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

// Knowledge management is served by the Python ai-stylist service, proxied by the
// gateway under /api/v1/ai-stylist/knowledge/** (rewritten to /api/v1/knowledge/**).
const KNOWLEDGE = `${ENDPOINTS.AI_STYLIST}/knowledge`

// Ingestion scrapes URLs with Firecrawl then runs two LLM extraction passes ΓÇö
// the gateway route allows 5 minutes for this path.
const INGEST_TIMEOUT_MS = 300000
// Approving embeds every concept and writes to Neo4j + Qdrant.
const APPROVE_TIMEOUT_MS = 120000

export async function ingestKnowledge({ userId, title, texts, urls }) {
  return apiClient.post(
    `${KNOWLEDGE}/ingest`,
    {
      user_id: userId,
      title: title || undefined,
      texts: texts || [],
      urls: urls || [],
    },
    { timeout: INGEST_TIMEOUT_MS }
  )
}

export async function listKnowledgeSources(userId) {
  return apiClient.get(`${KNOWLEDGE}/sources`, { params: { user_id: userId } })
}

export async function getKnowledgeSource(sourceId) {
  return apiClient.get(`${KNOWLEDGE}/sources/${sourceId}`)
}

export async function approveKnowledgeSource(sourceId) {
  return apiClient.post(`${KNOWLEDGE}/sources/${sourceId}/approve`, null, {
    timeout: APPROVE_TIMEOUT_MS,
  })
}

export async function deleteKnowledgeSource(sourceId) {
  return apiClient.delete(`${KNOWLEDGE}/sources/${sourceId}`)
}

export async function getKnowledgeGraphOverview() {
  return apiClient.get(`${KNOWLEDGE}/graph`, { timeout: 30000 })
}