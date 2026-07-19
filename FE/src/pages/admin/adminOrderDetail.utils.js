export function buildAdminOrderDetailPath(orderId, listSearch = '') {
  const params = new URLSearchParams()
  const returnPath = `/admin/orders${listSearch || ''}`
  params.set('from', returnPath)
  return `/admin/orders/${encodeURIComponent(orderId)}?${params.toString()}`
}

export function resolveAdminOrderBackUrl(search = '') {
  const candidate = new URLSearchParams(search).get('from')
  if (candidate === '/admin/orders' || candidate?.startsWith('/admin/orders?')) {
    return candidate
  }
  return '/admin/orders'
}

export function getOrderItemLineTotal(item) {
  const quantity = Number(item?.quantity ?? 0)
  const unitPrice = Number(item?.priceAtPurchase ?? 0)
  if (!Number.isFinite(quantity) || !Number.isFinite(unitPrice)) return 0
  return quantity * unitPrice
}

export function getOrderSubtotal(items = []) {
  return items.reduce((sum, item) => sum + getOrderItemLineTotal(item), 0)
}

export function getOrderItemDisplay(item = {}) {
  return {
    name: displayValue(item.productName),
    productCode: displayValue(item.productId),
    variantCode: displayValue(item.catalogVariantId || item.variantId),
    sku: displayValue(item.sku),
    color: displayValue(item.color),
    size: displayValue(item.size),
    material: displayValue(item.material),
    imageUrl: item.primaryImageUrl || null,
  }
}

export function displayValue(value) {
  return value === null || value === undefined || String(value).trim() === ''
    ? 'Chưa có thông tin'
    : String(value)
}
