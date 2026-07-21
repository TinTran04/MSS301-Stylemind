import apiClient from '../../services/apiClient'
import { ENDPOINTS } from '../../services/endpoints'

export async function getProfile() {
  return apiClient.get(`${ENDPOINTS.USERS}/profile`)
}

export async function getAddresses() {
  return apiClient.get(`${ENDPOINTS.USERS}/addresses`)
}

export async function getProvinces() {
  return apiClient.get(`${ENDPOINTS.USERS}/administrative/provinces`)
}

export async function getWards(provinceCode) {
  return apiClient.get(`${ENDPOINTS.USERS}/administrative/provinces/${provinceCode}/wards`)
}

export async function createAddress(data) {
  return apiClient.post(`${ENDPOINTS.USERS}/addresses`, data)
}

export async function updateAddress(addressId, data) {
  return apiClient.put(`${ENDPOINTS.USERS}/addresses/${addressId}`, data)
}

export async function deleteAddress(addressId) {
  return apiClient.delete(`${ENDPOINTS.USERS}/addresses/${addressId}`)
}

export async function setDefaultAddress(addressId) {
  return apiClient.patch(`${ENDPOINTS.USERS}/addresses/${addressId}/default`)
}
