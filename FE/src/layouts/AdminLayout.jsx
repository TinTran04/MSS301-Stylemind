import { useState } from 'react'
import { Link, useLocation, Outlet, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard, Package, Warehouse, ShoppingCart, Users, UserCog,
  Brain, Network, BarChart3, Settings, Bell, Search, LogOut, ChevronLeft, ChevronRight, Store
} from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import { getInitials } from '../features/auth/auth.utils'

const sidebarLinks = [
  { to: '/admin', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/admin/products', label: 'Products', icon: Package },
  { to: '/admin/inventory', label: 'Inventory', icon: Warehouse },
  { to: '/admin/orders', label: 'Orders', icon: ShoppingCart },
  { to: '/admin/customers', label: 'Customers', icon: Users },
  { to: '/admin/users', label: 'User Management', icon: UserCog },
  { to: '/admin/notifications', label: 'Notifications', icon: Bell },
  { to: '/admin/ai-pipeline', label: 'AI Pipeline', icon: Brain },
  { to: '/admin/knowledge-graph', label: 'Knowledge Graph', icon: Network },
  { to: '/admin/recommendations', label: 'Recommendations', icon: BarChart3 },
  { to: '/admin/settings', label: 'Settings', icon: Settings },
]

export default function AdminLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const [collapsed, setCollapsed] = useState(false)
  const { user, logout } = useAuth()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const initials = getInitials(user?.name || user?.fullName || user?.email)
  const displayName = user?.name || user?.fullName || user?.email || 'Admin'

  return (
    <div className="min-h-screen bg-background">
      {/* Sidebar */}
      <aside
        className={`fixed left-0 top-0 h-full bg-surface-container-lowest border-r border-outline-variant/20 z-40 transition-all duration-300 flex flex-col ${
          collapsed ? 'w-[72px]' : 'w-64'
        }`}
      >
        <div className={`p-6 border-b border-outline-variant/20 ${collapsed ? 'px-4' : ''}`}>
          {!collapsed && (
            <Link to="/admin" className="font-display-lg tracking-tighter text-primary no-underline">
              StyleMind
            </Link>
          )}
          {collapsed && (
            <Link to="/admin" className="font-display-lg tracking-tighter text-primary no-underline text-lg">
              SM
            </Link>
          )}
        </div>

        <div className={`p-4 border-b border-outline-variant/20 ${collapsed ? 'px-2' : ''}`}>
          {!collapsed && (
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-full bg-primary text-on-primary flex items-center justify-center text-sm font-semibold">
                {initials}
              </div>
              <div className="overflow-hidden">
                <p className="text-sm font-medium text-on-surface truncate">{displayName}</p>
                <p className="text-xs text-on-surface-variant truncate">Administrator</p>
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
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm no-underline transition-all duration-200 ${
                  isActive
                    ? 'bg-primary text-on-primary shadow-md'
                    : 'text-on-surface-variant hover:bg-surface-container-high'
                } ${collapsed ? 'justify-center' : ''}`}
                title={collapsed ? link.label : undefined}
              >
                <Icon size={18} />
                {!collapsed && <span>{link.label}</span>}
              </Link>
            )
          })}
        </nav>

        <div className={`p-3 border-t border-outline-variant/20 space-y-1 ${collapsed ? 'px-2' : ''}`}>
          <Link
            to="/"
            className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm no-underline text-on-surface-variant hover:bg-surface-container-high transition-all ${
              collapsed ? 'justify-center' : ''
            }`}
            title={collapsed ? 'Back to Store' : undefined}
          >
            <Store size={18} />
            {!collapsed && <span>Back to Store</span>}
          </Link>
          <button
            onClick={handleLogout}
            className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm text-error hover:bg-error-container/20 transition-all ${
              collapsed ? 'justify-center' : ''
            }`}
            title={collapsed ? 'Sign Out' : undefined}
          >
            <LogOut size={18} />
            {!collapsed && <span>Sign Out</span>}
          </button>
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="w-full flex items-center justify-center gap-3 px-3 py-2.5 rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-high transition-all"
          >
            {collapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
            {!collapsed && <span>Collapse</span>}
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className={`transition-all duration-300 ${collapsed ? 'ml-[72px]' : 'ml-64'}`}>
        {/* Top Bar */}
        <header className="sticky top-0 z-30 glass-header h-16 flex items-center justify-between px-8">
          <div className="flex items-center gap-4 flex-1">
            <div className="relative max-w-md w-full">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant" />
              <input
                type="text"
                placeholder="Search..."
                className="w-full pl-9 pr-4 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none focus:ring-1 focus:ring-tertiary-container"
              />
            </div>
          </div>
          <div className="flex items-center gap-4">
            <Link
              to="/admin/notifications"
              className="p-2 rounded-full hover:bg-surface-container-high relative transition-colors"
            >
              <Bell size={20} className="text-on-surface-variant" />
              <span className="absolute top-1 right-1 w-2 h-2 bg-error rounded-full"></span>
            </Link>
            <button
              onClick={handleLogout}
              className="flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm text-error hover:bg-error-container/20 transition-colors"
              title="Sign Out"
            >
              <LogOut size={16} />
              <span className="hidden md:inline">Sign Out</span>
            </button>
            <div className="w-9 h-9 rounded-full bg-primary text-on-primary flex items-center justify-center text-sm font-semibold">
              {initials}
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="p-6 md:p-8 min-h-[calc(100vh-4rem)]">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
