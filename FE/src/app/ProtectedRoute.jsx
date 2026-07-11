import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

/**
 * Guards routes that require an authenticated user.
 * Redirects guests to /login, preserving the attempted location.
 */
export function RequireAuth() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return <Outlet />
}

/**
 * Guards admin-only routes.
 * Guests go to /login; authenticated non-admins go back to the storefront.
 */
export function RequireAdmin() {
  const { isAuthenticated, role } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (role !== 'admin') {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
