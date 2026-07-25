export const CUSTOMER_NAV_LINKS = [
  { to: '/', label: 'Trang chủ' },
  { to: '/shop', label: 'Cửa hàng' },
  { to: '/ai-stylist', label: 'Stylist AI' },
  { to: '/orders', label: 'Đơn hàng' },
]

export function splitTo(to) {
  const [pathname, search = ''] = to.split('?')
  return {
    pathname,
    search: search ? `?${search}` : '',
  }
}

export function buildSearchPath(query) {
  const normalizedQuery = String(query || '').trim()
  return normalizedQuery ? `/shop?search=${encodeURIComponent(normalizedQuery)}` : null
}

export function buildShopCategoryPath(category) {
  const categoryId = Number(category?.id)
  return Number.isFinite(categoryId) && categoryId > 0 ? `/shop?category=${categoryId}` : '/shop'
}

export function groupCategories(categories) {
  const safeCategories = Array.isArray(categories) ? categories : []
  const topLevelCategories = safeCategories.filter((category) => !category.parentId)
  const childrenByParentId = new Map()

  safeCategories
    .filter((category) => category.parentId)
    .forEach((category) => {
      const children = childrenByParentId.get(category.parentId) || []
      children.push(category)
      childrenByParentId.set(category.parentId, children)
    })

  const parentGroups = topLevelCategories
    .filter((category) => childrenByParentId.has(category.id))
    .map((parent) => ({
      id: `parent-${parent.id}`,
      label: parent.name,
      parent,
      categories: childrenByParentId.get(parent.id),
    }))

  const standaloneCategories = topLevelCategories.filter((category) => !childrenByParentId.has(category.id))
  if (standaloneCategories.length) {
    parentGroups.push({
      id: 'explore',
      label: 'Khám phá',
      parent: null,
      categories: standaloneCategories,
    })
  }

  return parentGroups
}
