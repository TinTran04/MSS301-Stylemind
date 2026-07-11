import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

export async function getIndexJobs(filters = {}) {
  const params = new URLSearchParams()
  Object.keys(filters).forEach((key) => {
    if (filters[key]) params.append(key, filters[key])
  })
  const qs = params.toString()
  return apiClient.get(`${ENDPOINTS.ADMIN_AI_INDEX_JOBS}${qs ? `?${qs}` : ''}`)
}

export async function createIndexJob(payload) {
  return apiClient.post(ENDPOINTS.ADMIN_AI_INDEX_JOBS, payload)
}
