import { motion } from 'framer-motion'
import { Link, Outlet, useLocation } from 'react-router-dom'
import CustomerHeader from '../components/customer/CustomerHeader'
import { CUSTOMER_NAV_LINKS, splitTo } from '../features/header/headerNavigation'

export default function CustomerLayout() {
  const location = useLocation()
  const isActiveLink = (to) => {
    const target = splitTo(to)
    if (target.pathname === '/shop') return location.pathname === '/shop' && location.search !== '?collection=new'
    return location.pathname === target.pathname
  }

  return (
    <div className="min-h-screen bg-background">
      <CustomerHeader />

      {/* Main Content */}
      <main className="pt-20">
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.3 }}>
          <Outlet />
        </motion.div>
      </main>

      {/* Mobile Bottom Nav */}
      <nav className="md:hidden fixed bottom-0 w-full bg-surface/90 backdrop-blur-2xl border-t border-outline-variant/20 z-50">
        <div className="flex justify-around items-center h-20 px-4">
          {CUSTOMER_NAV_LINKS.slice(0, 5).map((link) => {
            const icons = {
              '/': 'home',
              '/shop': 'storefront',
              '/ai-stylist': 'auto_awesome',
              '/cart': 'shopping_bag',
              '/orders': 'receipt_long',
            }
            const { pathname } = splitTo(link.to)
            return (
              <Link
                key={link.to}
                to={link.to}
                className={`flex flex-col items-center gap-1 no-underline ${
                  isActiveLink(link.to) ? 'text-primary' : 'text-on-surface-variant opacity-60'
                }`}
              >
                <span className="material-symbols-outlined text-2xl">{icons[pathname] || 'home'}</span>
                <span className="text-[10px] font-medium">{link.label}</span>
              </Link>
            )
          })}
        </div>
      </nav>


    </div>
  )
}
