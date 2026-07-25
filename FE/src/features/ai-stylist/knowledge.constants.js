// Vietnamese labels + colors for the knowledge-graph domain vocabulary used by
// the Python ai-stylist service (see services/concept/ingestion.py allowed sets).

export const CONCEPT_TYPE_LABELS = {
  item_type: 'Loại trang phục',
  style: 'Phong cách',
  occasion: 'Dịp / hoàn cảnh',
  body_context: 'Dáng người',
  preference: 'Sở thích',
  material_property: 'Chất liệu',
  color: 'Màu sắc',
  user_context: 'Ngữ cảnh người dùng',
}

// Monochrome-friendly accents from the app theme palette.
export const CONCEPT_TYPE_COLORS = {
  item_type: '#1c1b1b',
  style: '#4B0082',
  occasion: '#735c00',
  body_context: '#2e7d32',
  preference: '#5e5f5d',
  material_property: '#cca730',
  color: '#e9c349',
  user_context: '#747878',
}

export const RELATION_LABELS = {
  PAIRS_WITH: 'phối hợp với',
  PREFERS: 'ưu tiên',
  AVOIDS: 'tránh',
  COMPATIBLE_WITH: 'tương thích với',
}

export const RULE_TYPE_LABELS = {
  style_rule: 'Quy tắc phong cách',
  body_rule: 'Quy tắc dáng người',
  occasion_rule: 'Quy tắc theo dịp',
  modesty_rule: 'Quy tắc kín đáo',
  preferred_item_types: 'Trang phục nên chọn',
  avoided_item_types: 'Trang phục nên tránh',
  preferred_colors: 'Màu nên chọn',
  preferred_targets: 'Nhóm ưu tiên',
  excluded_items: 'Loại trừ',
  pairing_rules: 'Quy tắc phối đồ',
}

export const RULE_PAYLOAD_LABELS = {
  advice: 'Lời khuyên',
  rationale: 'Lý do',
  colors: 'Màu sắc',
  items: 'Trang phục',
  avoid_items: 'Nên tránh',
  contexts: 'Ngữ cảnh',
  constraints: 'Ràng buộc',
  pairings: 'Phối hợp',
  examples: 'Ví dụ',
}

export function conceptTypeLabel(type) {
  return CONCEPT_TYPE_LABELS[type] || type
}

export function relationLabel(relation) {
  return RELATION_LABELS[relation] || relation
}

export function ruleTypeLabel(type) {
  return RULE_TYPE_LABELS[type] || type
}
