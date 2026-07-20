// Vietnamese labels + colors for the knowledge-graph domain vocabulary used by
// the Python ai-stylist service (see services/concept/ingestion.py allowed sets).

export const CONCEPT_TYPE_LABELS = {
  item_type: 'Loß║íi trang phß╗Ñc',
  style: 'Phong c├ích',
  occasion: 'Dß╗ïp / ho├án cß║únh',
  body_context: 'D├íng ng╞░ß╗¥i',
  preference: 'Sß╗ƒ th├¡ch',
  material_property: 'Chß║Ñt liß╗çu',
  color: 'M├áu sß║»c',
  user_context: 'Ngß╗» cß║únh ng╞░ß╗¥i d├╣ng',
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
  PAIRS_WITH: 'phß╗æi hß╗úp vß╗¢i',
  PREFERS: '╞░u ti├¬n',
  AVOIDS: 'tr├ính',
  COMPATIBLE_WITH: 't╞░╞íng th├¡ch vß╗¢i',
}

export const RULE_TYPE_LABELS = {
  style_rule: 'Quy tß║»c phong c├ích',
  body_rule: 'Quy tß║»c d├íng ng╞░ß╗¥i',
  occasion_rule: 'Quy tß║»c theo dß╗ïp',
  modesty_rule: 'Quy tß║»c k├¡n ─æ├ío',
  preferred_item_types: 'Trang phß╗Ñc n├¬n chß╗ìn',
  avoided_item_types: 'Trang phß╗Ñc n├¬n tr├ính',
  preferred_colors: 'M├áu n├¬n chß╗ìn',
  preferred_targets: 'Nh├│m ╞░u ti├¬n',
  excluded_items: 'Loß║íi trß╗½',
  pairing_rules: 'Quy tß║»c phß╗æi ─æß╗ô',
}

export const RULE_PAYLOAD_LABELS = {
  advice: 'Lß╗¥i khuy├¬n',
  rationale: 'L├╜ do',
  colors: 'M├áu sß║»c',
  items: 'Trang phß╗Ñc',
  avoid_items: 'N├¬n tr├ính',
  contexts: 'Ngß╗» cß║únh',
  constraints: 'R├áng buß╗Öc',
  pairings: 'Phß╗æi hß╗úp',
  examples: 'V├¡ dß╗Ñ',
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