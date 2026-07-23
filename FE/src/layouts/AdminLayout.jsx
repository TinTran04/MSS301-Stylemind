import { useEffect, useState } from 'react'
import { Link, useLocation, Outlet, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard, Package, ShoppingCart, UserCog,
  Network, Bell, LogOut, ChevronLeft, ChevronRight
} from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import { getInitials } from '../features/auth/auth.utils'
import { getPendingCancellationSummary } from '../features/orders/admin-order.api'

const sidebarLinks = [
  { to: '/admin', label: 'Tổng quan', icon: LayoutDashboard },
  { to: '/admin/products', label: 'Sản phẩm', icon: Package },
  { to: '/admin/orders', label: 'Đơn hàng', icon: ShoppingCart },
  { to: '/admin/users', label: 'Quản lý người dùng', icon: UserCog },
  { to: '/admin/notifications', label: 'Thông báo', icon: Bell },
  { to: '/admin/knowledge-graph', label: 'Đồ thị tri thức', icon: Network },
]

export default function AdminLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const [collapsed, setCollapsed] = useState(false)
  const [pendingCancellationCount, setPendingCancellationCount] = useState(0)
  const { user, logout } = useAuth()

  useEffect(() => {
    let active = true
    getPendingCancellationSummary()
      .then((response) => {
        if (!active) return
        setPendingCancellationCount(Number(response?.pendingCount || response?.data?.pendingCount || 0))
      })
      .catch(() => {
        if (active) setPendingCancellationCount(0)
      })
    return () => { active = false }
  }, [])

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const initials = getInitials(user?.name || user?.email)
  const displayName = user?.name || user?.email || 'Quản trị viên'
  const responsiveSidebarWidth = collapsed ? 'md:w-[72px]' : 'md:w-64'
  const responsiveContentMargin = collapsed ? 'md:ml-[72px]' : 'md:ml-64'

  return (
    <div className="min-h-screen bg-background">
      {/* Sidebar */}
      <aside
        className={`fixed left-0 top-0 h-full bg-surface-container-lowest border-r border-outline-variant/20 z-40 transition-all duration-300 flex flex-col w-[72px] ${responsiveSidebarWidth} ${
          collapsed ? 'w-[72px]' : ''
        }`}
      >
        <div className={`p-6 border-b border-outline-variant/20 ${collapsed ? 'px-4' : 'px-3 md:px-6'}`}>
          {!collapsed && (
            <Link to="/admin" className="hidden md:block font-display-lg tracking-tighter text-primary no-underline">
              StyleMind
            </Link>
          )}
          <Link to="/admin" className={`${collapsed ? '' : 'md:hidden'} font-display-lg tracking-tighter text-primary no-underline text-lg`}>
            SM
          </Link>
        </div>

        <div className={`p-4 border-b border-outline-variant/20 ${collapsed ? 'px-2' : ''}`}>
          {!collapsed && (
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-full bg-primary text-on-primary flex items-center justify-center text-sm font-semibold">
                {initials}
              </div>
              <div className="overflow-hidden">
                <p className="text-sm font-medium text-on-surface truncate">{displayName}</p>
                <p className="text-xs text-on-surface-variant truncate">Quản trị viên</p>
              </div>
            </div>
          )}
          {collapsed && (
            <div className="flex justify-center">
              <div className="w-9 h-9 rounded-full bg-primary text-on-primary flex items-center justify-center text-sm font-semibold">
                {initials}
              </div>
            </div>
          )}
        </div>

        <nav className="flex-1 p-3 space-y-1 overflow-y-auto custom-scrollbar">
          {sidebarLinks.map((link) => {
            const Icon = link.icon
            const isActive = location.pathname === link.to
            return (
              <Link
                key={link.to}
                to={link.to}
                aria-label={link.label}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm no-underline transition-all duration-200 ${
                  isActive
                    ? 'bg-primary text-on-primary shadow-md'
                    : 'text-on-surface-variant hover:bg-surface-container-high'
                } ${collapsed ? 'justify-center' : ''}`}
                title={collapsed ? link.label : undefined}
              >
                <span className="relative inline-flex">
                  <Icon size={18} />
                  {link.to === '/admin/orders' && pendingCancellationCount > 0 && (
                    <span className="absolute -right-1 -top-1 h-2.5 w-2.5 rounded-full bg-error ring-2 ring-surface-container-lowest" />
                  )}
                </span>
              {!collapsed && <span className="hidden md:inline">{link.label}</span>}
              </Link>
            )
          })}
        </nav>

        <div className={`p-3 border-t border-outline-variant/20 space-y-1 ${collapsed ? 'px-2' : ''}`}>
          <button
            onClick={handleLogout}
            aria-label="Đăng xuất"
            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-error hover:bg-error-container/20 transition-all ${
              collapsed ? 'justify-center' : ''
            }`}
            title={collapsed ? 'Đăng xuất' : undefined}
          >
            <LogOut size={18} />
            {!collapsed && <span className="hidden md:inline">Đăng xuất</span>}
          </button>
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="w-full flex items-center justify-center gap-3 px-3 py-2.5 rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-high transition-all"
          >
            {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
            {!collapsed && <span className="hidden md:inline">Thu gọn</span>}
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className={`transition-all duration-300 ml-[72px] ${responsiveContentMargin}`}>
        {/* Page Content */}
        <main className="p-6 md:p-8 min-h-[calc(100vh-4rem)]">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
