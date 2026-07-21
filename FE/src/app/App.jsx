import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import AppRouter from './router'
import { getCurrentUser } from '../features/auth/auth.api'
import useAuthStore from '../features/auth/auth.store'
import { getAuthToken } from '../services/apiClient'

function ScrollToTop() {
  const { pathname, search } = useLocation()

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  }, [pathname, search])

  return null
}

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

  return (
    <>
      <ScrollToTop />
      <AppRouter />
    </>
  )
}
