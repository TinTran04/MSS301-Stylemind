import { useEffect } from 'react'
import AppRouter from './router'
import { getCurrentUser } from '../features/auth/auth.api'
import useAuthStore from '../features/auth/auth.store'
import { getAuthToken } from '../services/apiClient'

export default function App() {
  const setUser = useAuthStore((s) => s.setUser)
  const logout = useAuthStore((s) => s.logout)

  // On first load, re-validate the stored token by fetching the real profile
  // from the backend. This ensures the role is always authoritative (not just
  // what was last cached in localStorage) and keeps the session alive.
  useEffect(() => {
    if (!getAuthToken()) return

    getCurrentUser()
      .then((user) => {
        if (user) setUser(user)
      })
      .catch(() => {
        // Token expired or revoked — clear stale session so guards redirect
        logout()
      })
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return <AppRouter />
}
