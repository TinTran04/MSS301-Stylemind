import {
  formatStatusLabel,
  normalizeOrderStatus,
  ORDER_TIMELINE_STEPS,
} from './orderStatus.js'

function buildTimeline(order) {
  const status = normalizeOrderStatus(order.orderStatus)
  const currentIndex = Math.max(ORDER_TIMELINE_STEPS.indexOf(status), 0)

  return ORDER_TIMELINE_STEPS.map((step, index) => ({
    status: step,
    label: formatStatusLabel(step),
    date: index <= currentIndex ? order.updatedAt || order.createdAt : null,
    completed: index <= currentIndex,
  }))
}

export function mapOrder(order) {
  if (!order) return null

  return {
    ...order,
    id: order.id,
    date: order.createdAt || order.updatedAt,
    status: normalizeOrderStatus(order.orderStatus).toLowerCase(),
    statusLabel: formatStatusLabel(order.orderStatus),
    total: Number(order.totalAmount || 0),
    shippingAddress: order.shippingAddress,
    items: (order.items || []).map((item) => {
      const image = item.primaryImageUrl || item.imageUrl || item.image || null

      return {
        id: item.id,
        variantId: item.variantId,
        catalogVariantId: item.catalogVariantId,
        productId: item.productId,
        sku: item.sku,
        name: item.productName || item.name || item.variantId || 'Mặt hàng trong đơn',
        image,
        imageUrl: image,
        size: item.size || 'Một cỡ',
        color: item.color || 'Mặc định',
        material: item.material || '',
        price: Number(item.priceAtPurchase || 0),
        quantity: item.quantity || 1,
      }
    }),
    timeline: buildTimeline(order),
  }
}
