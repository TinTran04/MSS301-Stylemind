import {
  formatStatusLabel,
  formatTimelineStatusLabel,
  getOrderTimelineSteps,
  normalizeOrderStatus,
} from './orderStatus.js'
import { calculateTotal } from '../cart/cart.utils.js'

function buildTimeline(order) {
  const status = normalizeOrderStatus(order.orderStatus)
  const history = Array.isArray(order.statusHistory) ? order.statusHistory : []
  const statusDates = new Map()
  const initialStatus = normalizeOrderStatus(history[0]?.previousStatus || order.orderStatus)
  if (initialStatus) {
    statusDates.set(initialStatus, order.createdAt || order.updatedAt || null)
  }

  history.forEach((entry) => {
    const nextStatus = normalizeOrderStatus(entry.newStatus)
    if (nextStatus && entry.timestamp) {
      statusDates.set(nextStatus, entry.timestamp)
    }
  })

  const steps = getOrderTimelineSteps(order)

  return steps.map((step) => ({
    status: step,
    label: formatTimelineStatusLabel(step, order.paymentMethod),
    date: statusDates.get(step) || (step === status ? order.updatedAt || order.createdAt : null),
    completed: statusDates.has(step) || step === status,
  }))
}

function numberOrNull(value) {
  if (value === null || value === undefined || value === '') return null
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : null
}

function buildPricingSummary(order, items) {
  const itemSubtotal = items.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0)
  const subtotal = numberOrNull(order.productSubtotal ?? order.subtotalAmount)
  const discount = numberOrNull(order.discountAmount) ?? 0
  const shipping = numberOrNull(order.shippingFee)
  const tax = numberOrNull(order.taxAmount)
  const hasBackendBreakdown = subtotal !== null || order.discountAmount != null || shipping !== null || tax !== null
  const fallbackPricing = calculateTotal(items, order.paymentMethod)
  const taxableSubtotal = Math.max((subtotal ?? itemSubtotal) - discount, 0)
  const exactTotal = hasBackendBreakdown
    ? (numberOrNull(order.totalAmount) ?? taxableSubtotal + (shipping ?? 0) + (tax ?? 0))
    : fallbackPricing.exactTotal

  return {
    subtotal: hasBackendBreakdown ? (subtotal ?? itemSubtotal) : fallbackPricing.subtotal,
    discountAmount: hasBackendBreakdown ? discount : 0,
    shipping: hasBackendBreakdown ? (shipping ?? 0) : fallbackPricing.shipping,
    tax: hasBackendBreakdown ? (tax ?? 0) : fallbackPricing.tax,
    roundingAdjustment: 0,
    exactTotal,
    total: exactTotal,
    hasBreakdown: true,
    source: hasBackendBreakdown ? 'backend' : 'fallback',
  }
}

function mapReturnRequest(returnRequest) {
  if (!returnRequest) return null
  return {
    ...returnRequest,
    attachments: (returnRequest.attachments || []).map((attachment) => ({
      ...attachment,
      fileName: attachment.fileName || 'Ảnh bằng chứng',
      imageDataUrl: attachment.imageDataUrl || '',
    })).filter((attachment) => attachment.imageDataUrl),
  }
}

export function mapOrder(order) {
  if (!order) return null
  const items = (order.items || []).map((item) => ({
    id: item.id,
    variantId: item.variantId,
    name: item.productName || item.name || item.sku || item.variantId || 'Mặt hàng trong đơn',
    image: item.primaryImageUrl || item.image || 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=200&h=260&fit=crop',
    size: item.size || 'Mặc định',
    color: item.color || 'Mặc định',
    price: Number(item.priceAtPurchase || 0),
    quantity: item.quantity || 1,
  }))
  const pricing = buildPricingSummary(order, items)

  return {
    ...order,
    id: order.id,
    date: order.createdAt || order.updatedAt,
    status: normalizeOrderStatus(order.orderStatus).toLowerCase(),
    statusLabel: formatStatusLabel(order.orderStatus),
    statusHistory: order.statusHistory || [],
    subtotal: pricing.subtotal,
    discountAmount: pricing.discountAmount,
    shippingFee: pricing.shipping,
    taxAmount: pricing.tax,
    roundingAdjustment: pricing.roundingAdjustment,
    exactTotal: pricing.exactTotal,
    hasPricingBreakdown: pricing.hasBreakdown,
    pricingSource: pricing.source,
    total: pricing.total,
    shippingAddress: order.shippingAddress,
    items,
    deliveryImages: (order.deliveryImages || []).map((image) => ({
      id: image.id,
      orderId: image.orderId,
      fileName: image.fileName || 'Ảnh nhận hàng',
      contentType: image.contentType || 'image/jpeg',
      sizeBytes: Number(image.sizeBytes || 0),
      imageDataUrl: image.imageDataUrl || '',
      uploadedAt: image.uploadedAt,
    })).filter((image) => image.imageDataUrl),
    latestReturnRequest: mapReturnRequest(order.latestReturnRequest),
    returnHistory: (order.returnHistory || []).map(mapReturnRequest).filter(Boolean),
    hasPendingReturnRequest: Boolean(order.hasPendingReturnRequest),
    timeline: buildTimeline(order),
  }
}

export function mapOrderSummary(order) {
  if (!order) return null
  return {
    ...order,
    id: order.id,
    date: order.createdAt,
    status: normalizeOrderStatus(order.orderStatus || order.status).toLowerCase(),
    total: Number(order.totalAmount || 0),
    totalAmount: Number(order.totalAmount || 0),
    itemCount: Number(order.itemCount || 0),
    items: [],
    latestReturnRequest: mapReturnRequest(order.latestReturnRequest),
    hasPendingReturnRequest: Boolean(order.hasPendingReturnRequest),
  }
}

export function mergeOrderSummaryUpdate(orders, updatedOrder) {
  if (!updatedOrder?.id) return orders
  const summaryUpdate = mapOrderSummary(updatedOrder)
  if (!summaryUpdate) return orders

  return (orders || []).map((order) => (
    order.id === updatedOrder.id
      ? {
          ...order,
          ...summaryUpdate,
          itemCount: order.itemCount || summaryUpdate.itemCount,
          items: order.items || [],
        }
      : order
  ))
}

function shouldHydrateOrderSummary(order) {
  return normalizeOrderStatus(order?.orderStatus || order?.status) === 'COMPLETED'
    && !order?.latestReturnRequest
}

export async function hydrateOrderSummariesWithDetails(orders, fetchOrderDetail) {
  if (!Array.isArray(orders) || typeof fetchOrderDetail !== 'function') return orders
  const candidates = orders.filter(shouldHydrateOrderSummary)
  if (candidates.length === 0) return orders

  const results = await Promise.allSettled(candidates.map((order) => fetchOrderDetail(order.id)))
  return results.reduce((currentOrders, result) => (
    result.status === 'fulfilled' && result.value
      ? mergeOrderSummaryUpdate(currentOrders, result.value)
      : currentOrders
  ), orders)
}

export function getOrderTimestamp(order) {
  const value = order?.createdAt || order?.date || order?.updatedAt
  const timestamp = value ? new Date(value).getTime() : 0
  return Number.isNaN(timestamp) ? 0 : timestamp
}
