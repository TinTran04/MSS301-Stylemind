function normalizeText(value) {
  return String(value ?? '').trim().replace(/\s+/g, ' ')
}

function uniqueOrdered(values) {
  const seen = new Set()
  const result = []
  values.forEach((value) => {
    const normalized = normalizeText(value)
    if (!normalized) return
    const key = normalized.toLowerCase()
    if (seen.has(key)) return
    seen.add(key)
    result.push(normalized)
  })
  return result
}

export function formatVariantStock(stockQuantity) {
  const stock = Number(stockQuantity ?? 0)
  return stock > 0 ? `Còn ${stock}` : 'Hết hàng'
}

export function formatVariantPrice(variant, basePrice) {
  const amount = variant?.priceOverride != null && variant.priceOverride !== ''
    ? Number(variant.priceOverride)
    : Number(basePrice ?? 0)
  if (!Number.isFinite(amount)) return '—'
  return `${amount.toLocaleString('vi-VN')} đ`
}

export function formatVariantStatus(variant) {
  return variant?.active === false ? 'Ngừng bán' : 'Đang bán'
}

export function groupVariantsBySize(variants = []) {
  const groups = new Map()
  variants.forEach((variant) => {
    const size = normalizeText(variant?.size) || 'Khác'
    const key = size.toLowerCase()
    if (!groups.has(key)) {
      groups.set(key, {
        key,
        size,
        variants: [],
      })
    }
    groups.get(key).variants.push(variant)
  })
  return Array.from(groups.values())
}

export function summarizeVariants(variants = []) {
  const total = variants.length
  if (total === 0) {
    return {
      countLabel: 'Chưa có biến thể',
      hintLabel: '',
    }
  }

  const sizes = uniqueOrdered(variants.map((variant) => variant?.size))
  if (sizes.length >= 2) {
    return {
      countLabel: `${total} biến thể`,
      hintLabel: sizes.slice(0, 2).join(', '),
    }
  }

  const combos = uniqueOrdered(
    variants.map((variant) => {
      const size = normalizeText(variant?.size)
      const color = normalizeText(variant?.color)
      if (size && color) return `${size}/${color}`
      return size || color
    })
  )

  return {
    countLabel: `${total} biến thể`,
    hintLabel: combos.slice(0, 2).join(', '),
  }
}
