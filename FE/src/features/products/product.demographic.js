const CANONICAL_TARGETS = {
  MEN: 'MEN',
  WOMEN: 'WOMEN',
  UNISEX: 'UNISEX',
}

const TARGET_DEMOGRAPHIC_ALIASES = {
  MEN: new Set(['MEN', 'MALE', 'NAM']),
  WOMEN: new Set(['WOMEN', 'FEMALE', 'NU']),
  UNISEX: new Set(['UNISEX']),
}

const TARGET_DEMOGRAPHIC_OPTIONS = [
  { value: '', label: 'Tất cả' },
  { value: CANONICAL_TARGETS.MEN, label: 'Nam' },
  { value: CANONICAL_TARGETS.WOMEN, label: 'Nữ' },
  { value: CANONICAL_TARGETS.UNISEX, label: 'Unisex' },
]

const TARGET_DEMOGRAPHIC_ADMIN_OPTIONS = TARGET_DEMOGRAPHIC_OPTIONS.filter((option) => option.value)

function normalizeComparable(value) {
  return String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toUpperCase()
}

export function normalizeTargetDemographic(value) {
  const normalized = normalizeComparable(value)
  if (!normalized) return ''

  for (const [canonical, aliases] of Object.entries(TARGET_DEMOGRAPHIC_ALIASES)) {
    if (aliases.has(normalized)) {
      return canonical
    }
  }

  return normalized
}

export function getTargetDemographicLabel(value) {
  const normalized = normalizeTargetDemographic(value)
  const option = TARGET_DEMOGRAPHIC_OPTIONS.find((item) => item.value === normalized)
  return option?.label || String(value || '')
}

export function getTargetDemographicOptions() {
  return TARGET_DEMOGRAPHIC_OPTIONS
}

export function getTargetDemographicAdminOptions() {
  return TARGET_DEMOGRAPHIC_ADMIN_OPTIONS
}

export function matchesTargetDemographic(productValue, filterValue) {
  const normalizedFilter = normalizeTargetDemographic(filterValue)
  if (!normalizedFilter) return true

  const normalizedProduct = normalizeTargetDemographic(productValue)
  const aliases = TARGET_DEMOGRAPHIC_ALIASES[normalizedFilter] || new Set([normalizedFilter])
  return aliases.has(normalizedProduct)
}

