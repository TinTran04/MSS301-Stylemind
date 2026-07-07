// Pure helpers for deriving valid size/color selections from a product's real
// variant combinations. Values are matched case/whitespace-insensitively so
// legacy data like "trắng" and "Trắng" collapse into a single option, while
// the original (trimmed) value is always what gets sent back to the caller
// so it still matches the variant exactly as stored.

export function normalizeLabel(value) {
  return (value || '').trim().toLowerCase()
}

function variantsMatching(variants, filters) {
  return variants.filter((variant) =>
    Object.entries(filters).every(([field, value]) => {
      if (!value) return true
      return normalizeLabel(variant[field]) === normalizeLabel(value)
    }),
  )
}

function dedupeOptions(variants, field) {
  const seen = new Map()
  variants.forEach((variant) => {
    const raw = (variant[field] || '').trim()
    if (!raw) return
    const key = normalizeLabel(raw)
    if (!seen.has(key)) seen.set(key, raw)
  })
  return [...seen.values()]
}

export function isVariantAvailable(variant) {
  if (!variant) return false
  if (variant.active === false) return false
  if (variant.stockQuantity != null && variant.stockQuantity <= 0) return false
  return true
}

export function getSizeOptions(variants, selectedColor) {
  const pool = selectedColor ? variantsMatching(variants, { color: selectedColor }) : variants
  return dedupeOptions(pool, 'size')
}

export function getColorOptions(variants, selectedSize) {
  const pool = selectedSize ? variantsMatching(variants, { size: selectedSize }) : variants
  return dedupeOptions(pool, 'color')
}

// Whether an option value (given the other dimension already selected, if any)
// has no in-stock/active variant behind it — used to render it disabled/"Hết hàng".
export function isOptionOutOfStock(variants, field, value, otherField, otherValue) {
  const filters = { [field]: value }
  if (otherValue) filters[otherField] = otherValue
  const matches = variantsMatching(variants, filters)
  if (!matches.length) return false
  return !matches.some(isVariantAvailable)
}

export function resolveVariant(variants, size, color) {
  if (!size || !color) return null
  const matches = variantsMatching(variants, { size, color })
  if (!matches.length) return null
  return matches.find(isVariantAvailable) || matches[0]
}

export function getDisplayedPrice(basePrice, selectedVariant) {
  const amount = selectedVariant?.priceOverride != null && selectedVariant.priceOverride !== ''
    ? Number(selectedVariant.priceOverride)
    : Number(basePrice ?? 0)

  return Number.isFinite(amount) ? amount : Number(basePrice ?? 0)
}

export function getAddToCartState(variants, size, color) {
  if (!size) {
    return { variantId: null, disabled: true, message: 'Vui lòng chọn kích cỡ.' }
  }
  if (!color) {
    return { variantId: null, disabled: true, message: 'Vui lòng chọn màu sắc.' }
  }
  const variant = resolveVariant(variants, size, color)
  if (!variant) {
    return { variantId: null, disabled: true, message: 'Vui lòng chọn phân loại sản phẩm.' }
  }
  if (!isVariantAvailable(variant)) {
    return { variantId: null, disabled: true, message: 'Biến thể này đã hết hàng.' }
  }
  return { variantId: variant.id, disabled: false, message: null }
}

export function getVisibleAddToCartMessage(variants, size, color, attempted = false) {
  if (!attempted) return null
  return getAddToCartState(variants, size, color).message
}
