import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

export async function getProfile() {
  return apiClient.get(`${ENDPOINTS.USERS}/style-profile`)
}

export async function updateProfile(data) {
  return apiClient.put(`${ENDPOINTS.USERS}/style-profile`, data)
}

export async function getAddresses() {
  return apiClient.get(`${ENDPOINTS.USERS}/addresses`)
}
