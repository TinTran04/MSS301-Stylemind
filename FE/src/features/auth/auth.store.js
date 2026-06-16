import { create } from 'zustand'

const useAuthStore = create((set) => ({
  user: null,
  isAuthenticated: false,
  role: null,
  loading: false,

  login: (user) => set({ user, isAuthenticated: true, role: user.role }),
  logout: () => set({ user: null, isAuthenticated: false, role: null }),
  setUser: (user) => set({ user }),
  setLoading: (loading) => set({ loading }),
}))

export default useAuthStore
