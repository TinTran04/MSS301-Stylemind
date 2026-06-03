import useAuthStore from '../features/auth/auth.store'

export function useAuth() {
  const user = useAuthStore((s) => s.user)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const role = useAuthStore((s) => s.role)
  const login = useAuthStore((s) => s.login)
  const logout = useAuthStore((s) => s.logout)

  return { user, isAuthenticated, role, login, logout }
}

export default useAuth
