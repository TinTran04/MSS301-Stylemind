function normalizeText(value) {
  return String(value ?? '').trim()
}

function pickImageFromList(images = []) {
  if (!Array.isArray(images)) return null

  for (const image of images) {
    if (typeof image === 'string') {
      const candidate = normalizeText(image)
      if (candidate) return candidate
      continue
    }

    if (image && typeof image === 'object') {
      const candidate = normalizeText(
        image.imageUrl
        || image.productImageUrl
        || image.primaryImageUrl
        || image.mainImageUrl
        || image.url,
      )
      if (candidate) return candidate
    }
  }

  return null
}

export function resolveCartItemImage(item = {}) {
  const directCandidates = [
    item.imageUrl,
    item.productImageUrl,
    item.primaryImageUrl,
    item.mainImageUrl,
    item?.variantInfo?.imageUrl,
    item?.variantInfo?.productImageUrl,
    item?.variant?.product?.primaryImageUrl,
    item?.variant?.product?.productImageUrl,
    item?.variant?.product?.mainImageUrl,
  ]

  for (const candidate of directCandidates) {
    const normalized = normalizeText(candidate)
    if (normalized) return normalized
  }

  return (
    pickImageFromList(item.images)
    || pickImageFromList(item?.variantInfo?.images)
    || pickImageFromList(item?.variant?.product?.images)
    || null
  )
}

export function formatCartVariantSummary(item = {}) {
  const size = normalizeText(item.size)
  const color = normalizeText(item.color)

  if (size && color) {
    return `Kích cỡ: ${size} · Màu sắc: ${color}`
  }

  if (size) {
    return `Kích cỡ: ${size}`
  }

  if (color) {
    return `Màu sắc: ${color}`
  }

  return 'Phân loại sản phẩm'
}
