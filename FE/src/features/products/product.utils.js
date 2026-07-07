export function getAIMatchLabel(score) {
  if (score >= 90) return 'Phù hợp tuyệt vời'
  if (score >= 75) return 'Phù hợp tốt'
  return 'Phù hợp trung bình'
}

export function getAIMatchColor(score) {
  if (score >= 90) return 'bg-ai-lavender text-ai-indigo'
  if (score >= 75) return 'bg-tertiary-fixed/30 text-tertiary'
  return 'bg-surface-container-high text-on-surface-variant'
}

export function getStockStatus(stock) {
  if (stock <= 0) return 'out_of_stock'
  if (stock <= 5) return 'low_stock'
  return 'in_stock'
}

export function getStockStatusLabel(status) {
  const labels = { in_stock: 'Còn hàng', low_stock: 'Sắp hết hàng', out_of_stock: 'Hết hàng' }
  return labels[status] || status
}

export function getStockStatusColor(status) {
  const colors = {
    in_stock: 'bg-green-status/10 text-green-status',
    low_stock: 'bg-tertiary-fixed/30 text-tertiary',
    out_of_stock: 'bg-error-container text-error',
  }
  return colors[status] || ''
}
