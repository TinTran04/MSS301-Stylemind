import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Bell, ChevronDown, LogOut, MapPin, Menu, Search, Settings, ShoppingBag, User, X } from 'lucide-react'
import { useCart } from '../../hooks/useCart'
import { useAuth } from '../../hooks/useAuth'
import { getInitials } from '../auth/auth.utils'
import { CUSTOMER_NAV_LINKS, buildSearchPath, splitTo } from './headerNavigation'
import { useHeaderCategories } from './useHeaderCategories'
import HeaderSearchPanel from './HeaderSearchPanel'
import CategoryMegaMenu from './CategoryMegaMenu'
import MobileNavigationDrawer from './MobileNavigationDrawer'

export default function CustomerHeader() {
  const location = useLocation()
  const navigate = useNavigate()
  const [scrolled, setScrolled] = useState(false)
  const [activePanel, setActivePanel] = useState('none')
  const [profileOpen, setProfileOpen] = useState(false)
  const [query, setQuery] = useState('')
  const profileRef = useRef(null)
  const returnFocusRef = useRef(null)
  const { itemCount, loadCart } = useCart()
  const { user, isAuthenticated, logout } = useAuth()
  const needsCategories = activePanel !== 'none'
  const { categories, status: categoryStatus, retry } = useHeaderCategories(needsCategories)

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20)
    handleScroll()
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  useEffect(() => {
    loadCart()
  }, [loadCart])

  useEffect(() => {
    setActivePanel('none')
    setProfileOpen(false)
  }, [location.pathname, location.search])

  useEffect(() => {
    const handler = (event) => {
      if (profileRef.current && !profileRef.current.contains(event.target)) setProfileOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  useEffect(() => {
    if (activePanel === 'none') {
      const returnFocus = returnFocusRef.current
      if (returnFocus) {
        requestAnimationFrame(() => returnFocus.focus())
        returnFocusRef.current = null
      }
      document.body.style.overflow = ''
      return undefined
    }
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = ''
    }
  }, [activePanel])

  const openPanel = (panel, event) => {
    returnFocusRef.current = event?.currentTarget || document.activeElement
    setProfileOpen(false)
    setActivePanel(panel)
  }

  const closePanel = () => setActivePanel('none')
  const isActiveLink = (to) => {
    const target = splitTo(to)
    if (target.search) return location.pathname === target.pathname && location.search === target.search
    if (target.pathname === '/shop') return location.pathname === '/shop' && location.search !== '?collection=new'
    return location.pathname === target.pathname
  }
  const handleLogout = () => {
    logout()
    setProfileOpen(false)
    navigate('/')
  }
  const handleSearchSubmit = () => {
    const path = buildSearchPath(query)
    if (!path) return
    closePanel()
    navigate(path)
  }
  const mobileOpenSearch = (event) => {
    const trigger = event.currentTarget
    closePanel()
    requestAnimationFrame(() => openPanel('search', { currentTarget: trigger }))
  }

  return (
    <>
      <header className={`fixed inset-x-0 top-0 z-50 border-b border-outline-variant/25 bg-surface transition-shadow duration-200 ${scrolled ? 'shadow-sm' : ''}`}>
        <div className="mx-auto flex h-20 max-w-[1440px] items-center justify-between gap-4 px-5 sm:px-8 lg:px-12">
          <Link to="/" className="shrink-0 font-display text-3xl text-primary no-underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary" aria-label="StyleMind, về trang chủ">
            StyleMind
          </Link>

          <nav aria-label="Điều hướng chính" className="hidden items-center gap-6 lg:flex">
            {CUSTOMER_NAV_LINKS.map((link) => link.to === '/shop' ? (
              <div key={link.to} className="flex items-center gap-0.5">
                <Link
                  to={link.to}
                  aria-current={isActiveLink(link.to) ? 'page' : undefined}
                  className={`border-b-2 py-1 text-sm font-medium no-underline transition-colors ${isActiveLink(link.to) ? 'border-primary text-primary' : 'border-transparent text-on-surface-variant hover:text-primary'}`}
                >
                  {link.label}
                </Link>
                <button
                  type="button"
                  onClick={(event) => openPanel(activePanel === 'mega-menu' ? 'none' : 'mega-menu', event)}
                  aria-label="Mở danh mục Cửa hàng"
                  aria-controls="stylemind-category-menu"
                  aria-expanded={activePanel === 'mega-menu'}
                  title="Mở danh mục Cửa hàng"
                  className="flex h-10 w-8 items-center justify-center rounded-lg text-on-surface-variant transition-colors hover:bg-surface-container-high hover:text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                >
                  <ChevronDown size={15} aria-hidden="true" />
                </button>
              </div>
            ) : (
              <Link
                key={link.to}
                to={link.to}
                aria-current={isActiveLink(link.to) ? 'page' : undefined}
                className={`border-b-2 py-1 text-sm font-medium no-underline transition-colors ${isActiveLink(link.to) ? 'border-primary text-primary' : 'border-transparent text-on-surface-variant hover:text-primary'}`}
              >
                {link.label}
              </Link>
            ))}
          </nav>

          <div className="flex items-center gap-1 sm:gap-2">
            <button
              type="button"
              onClick={(event) => openPanel(activePanel === 'search' ? 'none' : 'search', event)}
              aria-label="Tìm kiếm sản phẩm"
              aria-controls="stylemind-search-panel"
              aria-expanded={activePanel === 'search'}
              title="Tìm kiếm sản phẩm"
              className="flex h-10 w-10 items-center justify-center rounded-lg text-on-surface-variant transition-colors hover:bg-surface-container-high hover:text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
            >
              <Search size={20} aria-hidden="true" />
            </button>
            <Link to="/cart" className="relative flex h-10 w-10 items-center justify-center rounded-lg text-on-surface-variant no-underline transition-colors hover:bg-surface-container-high hover:text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary" aria-label={`Giỏ hàng${itemCount ? `, ${itemCount} sản phẩm` : ''}`} title="Giỏ hàng">
              <ShoppingBag size={20} aria-hidden="true" />
              {itemCount > 0 && <span className="absolute right-0 top-0 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-bold text-on-primary">{itemCount}</span>}
            </Link>
            {isAuthenticated ? (
              <div className="relative" ref={profileRef}>
                <button
                  type="button"
                  onClick={() => setProfileOpen((open) => !open)}
                  aria-label="Menu tài khoản"
                  aria-expanded={profileOpen}
                  className="flex h-10 w-10 items-center justify-center rounded-lg transition-colors hover:bg-surface-container-high focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                >
                  <span className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-xs font-semibold text-on-primary">{getInitials(user?.name || user?.email)}</span>
                </button>
                {profileOpen && (
                  <div className="absolute right-0 mt-2 w-56 rounded-lg border border-outline-variant/30 bg-surface-container-lowest py-1 shadow-lg">
                    <div className="border-b border-outline-variant/20 px-4 py-3">
                      <p className="truncate text-sm font-medium text-primary">{user?.name || user?.email}</p>
                      <p className="truncate text-xs text-on-surface-variant">{user?.email}</p>
                    </div>
                    <Link to="/profile" onClick={() => setProfileOpen(false)} className="flex items-center gap-3 px-4 py-3 text-sm text-on-surface-variant no-underline hover:bg-surface-container-high"><MapPin size={15} aria-hidden="true" />Địa chỉ giao hàng</Link>
                    <Link to="/orders" onClick={() => setProfileOpen(false)} className="flex items-center gap-3 px-4 py-3 text-sm text-on-surface-variant no-underline hover:bg-surface-container-high"><Settings size={15} aria-hidden="true" />Đơn hàng của tôi</Link>
                    <Link to="/notifications" onClick={() => setProfileOpen(false)} className="flex items-center gap-3 px-4 py-3 text-sm text-on-surface-variant no-underline hover:bg-surface-container-high"><Bell size={15} aria-hidden="true" />Thông báo</Link>
                    <button type="button" onClick={handleLogout} className="flex w-full items-center gap-3 px-4 py-3 text-sm text-error hover:bg-error-container/20"><LogOut size={15} aria-hidden="true" />Đăng xuất</button>
                  </div>
                )}
              </div>
            ) : (
              <Link to="/login" className="flex h-10 w-10 items-center justify-center rounded-lg text-on-surface-variant no-underline transition-colors hover:bg-surface-container-high hover:text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary" aria-label="Tài khoản" title="Tài khoản">
                <User size={20} aria-hidden="true" />
              </Link>
            )}
            <button
              type="button"
              onClick={(event) => openPanel(activePanel === 'mobile-navigation' ? 'none' : 'mobile-navigation', event)}
              aria-label={activePanel === 'mobile-navigation' ? 'Đóng menu điều hướng' : 'Mở menu điều hướng'}
              aria-expanded={activePanel === 'mobile-navigation'}
              title={activePanel === 'mobile-navigation' ? 'Đóng menu' : 'Mở menu'}
              className="flex h-10 w-10 items-center justify-center rounded-lg text-on-surface-variant transition-colors hover:bg-surface-container-high hover:text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary lg:hidden"
            >
              {activePanel === 'mobile-navigation' ? <X size={20} aria-hidden="true" /> : <Menu size={20} aria-hidden="true" />}
            </button>
          </div>
        </div>
      </header>

      <HeaderSearchPanel
        isOpen={activePanel === 'search'}
        onClose={closePanel}
        onSubmit={handleSearchSubmit}
        query={query}
        onQueryChange={setQuery}
        categories={categories}
        categoryStatus={categoryStatus}
        onRetry={retry}
      />
      <div id="stylemind-category-menu">
        <CategoryMegaMenu isOpen={activePanel === 'mega-menu'} onClose={closePanel} categories={categories} categoryStatus={categoryStatus} onRetry={retry} />
      </div>
      <MobileNavigationDrawer
        isOpen={activePanel === 'mobile-navigation'}
        onClose={closePanel}
        onOpenSearch={mobileOpenSearch}
        location={location}
        navLinks={CUSTOMER_NAV_LINKS}
        categories={categories}
        categoryStatus={categoryStatus}
        onRetry={retry}
      />
    </>
  )
}
