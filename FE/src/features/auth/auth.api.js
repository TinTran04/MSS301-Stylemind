import { ENDPOINTS } from '../../services/endpoints'

export async function loginUser(email, password) {
  // Mock for now - will use ENDPOINTS.AUTH when backend is available
  return { user: { id: '1', name: 'Guest', email, role: 'customer' }, token: 'mock-token' }
}

export async function registerUser(data) {
  return { user: { id: '1', name: data.name, email: data.email, role: 'customer' }, token: 'mock-token' }
}

export async function logoutUser() {
  localStorage.removeItem('auth_token')
  return true
}

export async function getCurrentUser() {
  return { id: '1', name: 'Guest User', email: 'guest@example.com', role: 'customer' }
}
