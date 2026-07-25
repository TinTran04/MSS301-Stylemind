import { ChevronDown, Search } from 'lucide-react'
import { Link } from 'react-router-dom'
import Drawer from '../../components/common/Drawer'
import { buildShopCategoryPath, groupCategories, splitTo } from './headerNavigation'

export default function MobileNavigationDrawer({ isOpen, onClose, onOpenSearch, location, navLinks, categories, categoryStatus, onRetry }) {
  const groups = groupCategories(categories)
  const isActiveLink = (to) => {
    const target = splitTo(to)
    return location.pathname === target.pathname && (!target.search || location.search === target.search)
  }

  return (
    <Drawer isOpen={isOpen} onClose={onClose} title="Khám phá StyleMind" panelClassName="max-w-sm">
      <div className="space-y-6">
        <button
          type="button"
          onClick={onOpenSearch}
          className="flex min-h-12 w-full items-center justify-between rounded-lg border border-outline-variant/40 px-4 text-left text-sm text-on-surface-variant transition-colors hover:border-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
        >
          Tìm kiếm sản phẩm <Search size={18} aria-hidden="true" />
        </button>

        <nav aria-label="Điều hướng chính" className="border-y border-outline-variant/30 py-3">
          {navLinks.map((link) => (
            <Link
              key={link.to}
              to={link.to}
              onClick={onClose}
              aria-current={isActiveLink(link.to) ? 'page' : undefined}
              className={`flex min-h-12 items-center text-base no-underline ${isActiveLink(link.to) ? 'font-semibold text-primary' : 'text-on-surface-variant'}`}
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <section aria-labelledby="mobile-category-heading">
          <h3 id="mobile-category-heading" className="font-label-sm uppercase tracking-[0.14em] text-on-surface-variant">Danh mục</h3>
          {categoryStatus === 'loading' && <p className="mt-4 text-sm text-on-surface-variant">Đang tải danh mục...</p>}
          {categoryStatus === 'error' && (
            <div className="mt-4 flex items-center gap-3 text-sm text-on-surface-variant">
              <span>Không thể tải danh mục.</span>
              <button type="button" onClick={onRetry} className="font-semibold text-primary underline underline-offset-4">Thử lại</button>
            </div>
          )}
          {categoryStatus === 'ready' && (
            <div className="mt-3 divide-y divide-outline-variant/30 border-y border-outline-variant/30">
              {groups.map((group) => (
                <details key={group.id} className="group py-1">
                  <summary className="flex min-h-12 cursor-pointer list-none items-center justify-between text-sm font-semibold text-primary focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary">
                    {group.label}<ChevronDown className="transition-transform group-open:rotate-180" size={17} aria-hidden="true" />
                  </summary>
                  <div className="pb-3 pl-3">
                    {group.parent && (
                      <Link to={buildShopCategoryPath(group.parent)} onClick={onClose} className="block py-2 text-sm font-semibold text-primary no-underline">
                        Xem tất cả {group.parent.name}
                      </Link>
                    )}
                    {group.categories.map((category) => (
                      <Link key={category.id} to={buildShopCategoryPath(category)} onClick={onClose} className="block py-2 text-sm text-on-surface-variant no-underline hover:text-primary">
                        {category.name}
                      </Link>
                    ))}
                  </div>
                </details>
              ))}
            </div>
          )}
        </section>
      </div>
    </Drawer>
  )
}
