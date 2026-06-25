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
    setAuthSession({ user, token })
    set({ user, token, isAuthenticated: Boolean(user && token), role: user?.role || null })
  },
  logout: () => {
    clearAuthSession()
    set({ user: null, token: null, isAuthenticated: false, role: null })
  },
  setUser: (user) => {
    setAuthSession({ user, token: getAuthToken() })
    set({ user, role: user?.role || null })
  },
  setLoading: (loading) => set({ loading }),
}))

export default useAuthStore
