import { useState, useEffect, useRef } from 'react'
import { motion } from 'framer-motion'
import { Link, useLocation, Outlet, useNavigate } from 'react-router-dom'
import { ShoppingBag, User, Menu, X, Search, LogOut, Settings } from 'lucide-react'
import { useCart } from '../hooks/useCart'
import { useAuth } from '../hooks/useAuth'
import { getInitials } from '../features/auth/auth.utils'

const navLinks = [
  { to: '/', label: 'Home' },
  { to: '/shop', label: 'Shop' },
  { to: '/ai-stylist', label: 'AI Stylist' },
  { to: '/shop?collection=new', label: 'Collections' },
  { to: '/orders', label: 'Orders' },
]

export default function CustomerLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const [scrolled, setScrolled] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const profileRef = useRef(null)
  const { itemCount, loadCart } = useCart()
  const { user, isAuthenticated, logout } = useAuth()

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20)
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  useEffect(() => {
    setMobileOpen(false)
  }, [location])

  useEffect(() => {
    loadCart()
  }, [loadCart])

  // Close profile dropdown when clicking outside
  useEffect(() => {
    const handler = (e) => {
      if (profileRef.current && !profileRef.current.contains(e.target)) {
        setProfileOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const handleLogout = () => {
    logout()
    setProfileOpen(false)
    navigate('/')
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <nav
        className={`fixed top-0 w-full z-50 transition-all duration-300 ${
          scrolled
            ? 'bg-surface/90 backdrop-blur-xl border-b border-outline-variant/20 shadow-sm py-3'
            : 'bg-surface/80 backdrop-blur-xl border-b border-outline-variant/20 py-5'
        }`}
      >
        <div className="max-w-[1440px] mx-auto px-6 md:px-16 flex items-center justify-between h-12">
          <Link to="/" className="font-display-lg tracking-tighter text-primary no-underline">
            StyleMind
          </Link>

          <div className="hidden md:flex items-center gap-8">
            {navLinks.map((link) => (
              <Link
                key={link.to}
                to={link.to}
                className={`font-label-md no-underline transition-all duration-200 ${
                  location.pathname === link.to
                    ? 'text-primary border-b-2 border-primary pb-1'
                    : 'text-on-surface-variant hover:text-primary'
                }`}
              >
                {link.label}
              </Link>
            ))}
          </div>

          <div className="flex items-center gap-4">
            <Link to="/shop" className="p-2 rounded-full hover:bg-surface-container transition-colors">
              <Search size={20} className="text-on-surface-variant" />
            </Link>
            <Link to="/cart" className="p-2 rounded-full hover:bg-surface-container transition-colors relative">
              <ShoppingBag size={20} className="text-on-surface-variant" />
              {itemCount > 0 && (
                <span className="absolute -top-1 -right-1 bg-primary text-on-primary text-[10px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
                  {itemCount}
                </span>
              )}
            </Link>
            {isAuthenticated ? (
              <div className="relative" ref={profileRef}>
                <button
                  onClick={() => setProfileOpen((o) => !o)}
                  className="flex items-center gap-2 p-1 rounded-full hover:bg-surface-container transition-colors"
                  aria-label="Account menu"
                >
                  <div className="w-8 h-8 rounded-full bg-primary text-on-primary text-xs font-semibold flex items-center justify-center">
                    {getInitials(user?.name || user?.fullName || user?.email)}
                  </div>
                </button>
                {profileOpen && (
                  <div className="absolute right-0 mt-2 w-52 bg-surface-container-lowest rounded-xl shadow-lg border border-outline-variant/20 py-1 z-50">
                    <div className="px-4 py-3 border-b border-outline-variant/10">
                      <p className="text-sm font-medium text-primary truncate">{user?.name || user?.fullName || user?.email}</p>
                      <p className="text-xs text-on-surface-variant truncate">{user?.email}</p>
                    </div>
                    <Link
                      to="/orders"
                      onClick={() => setProfileOpen(false)}
                      className="flex items-center gap-3 px-4 py-2.5 text-sm text-on-surface-variant hover:bg-surface-container-high no-underline transition-colors"
                    >
                      <Settings size={14} /> My Orders
                    </Link>
                    <button
                      onClick={handleLogout}
                      className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-error hover:bg-error-container/20 transition-colors"
                    >
                      <LogOut size={14} /> Sign Out
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <Link to="/login" className="p-2 rounded-full hover:bg-surface-container transition-colors">
                <User size={20} className="text-on-surface-variant" />
              </Link>
            )}
            <button
              className="md:hidden p-2 rounded-full hover:bg-surface-container transition-colors"
              onClick={() => setMobileOpen(!mobileOpen)}
            >
              {mobileOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
          </div>
        </div>

        {/* Mobile Menu */}
        {mobileOpen && (
          <div className="md:hidden bg-surface border-t border-outline-variant/20 px-6 py-4">
            {navLinks.map((link) => (
              <Link
                key={link.to}
                to={link.to}
                className={`block py-3 font-label-md no-underline ${
                  location.pathname === link.to ? 'text-primary font-semibold' : 'text-on-surface-variant'
                }`}
              >
                {link.label}
              </Link>
            ))}
          </div>
        )}
      </nav>

      {/* Main Content */}
      <main className="pt-20">
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.3 }}>
          <Outlet />
        </motion.div>
      </main>

      {/* Mobile Bottom Nav */}
      <nav className="md:hidden fixed bottom-0 w-full bg-surface/90 backdrop-blur-2xl border-t border-outline-variant/20 z-50">
        <div className="flex justify-around items-center h-20 px-4">
          {navLinks.slice(0, 5).map((link) => {
            const icons = {
              '/': 'home',
              '/shop': 'storefront',
              '/ai-stylist': 'auto_awesome',
              '/cart': 'shopping_bag',
              '/orders': 'receipt_long',
            }
            return (
              <Link
                key={link.to}
                to={link.to}
                className={`flex flex-col items-center gap-1 no-underline ${
                  location.pathname === link.to ? 'text-primary' : 'text-on-surface-variant opacity-60'
                }`}
              >
                <span className="material-symbols-outlined text-2xl">{icons[link.to] || 'home'}</span>
                <span className="text-[10px] font-medium">{link.label}</span>
              </Link>
            )
          })}
        </div>
      </nav>


    </div>
  )
}
