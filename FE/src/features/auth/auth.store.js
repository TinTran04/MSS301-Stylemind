import { create } from 'zustand'
import { clearAuthSession, getAuthToken, getStoredUser, setAuthSession } from '../../services/apiClient'

const storedUser = getStoredUser()
const storedToken = getAuthToken()

const useAuthStore = create((set) => ({
  user: storedUser,
  token: storedToken,
  isAuthenticated: Boolean(storedToken && storedUser),
  role: storedUser?.role || null,
  loading: false,

  login: (sessionOrUser) => {
    const user = sessionOrUser?.user || sessionOrUser
    const token = sessionOrUser?.token || getAuthToken()
    const role = user?.role?.toLowerCase() || null
    setAuthSession({ user, token })
    set({ user, token, isAuthenticated: Boolean(user && token), role })
  },
  logout: () => {
    clearAuthSession()
    set({ user: null, token: null, isAuthenticated: false, role: null })
  },
  setUser: (user) => {
    setAuthSession({ user, token: getAuthToken() })
    set({ user, role: user?.role?.toLowerCase() || null })
  },
  setLoading: (loading) => set({ loading }),
}))

export default useAuthStore
