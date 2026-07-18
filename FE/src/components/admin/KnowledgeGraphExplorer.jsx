import { useMemo, useState } from 'react'
import { Search, RefreshCw, Boxes, GitBranch, Lightbulb } from 'lucide-react'
import KnowledgeSourceReview from './KnowledgeSourceReview'
import {
  conceptTypeLabel,
  relationLabel,
  CONCEPT_TYPE_COLORS,
  CONCEPT_TYPE_LABELS,
} from '../../features/ai-stylist/knowledge.constants'

const MAX_NEIGHBORS = 14

// Ego-graph of the selected concept: the node in the middle, its direct
// neighbors on a circle. Far more readable than rendering the whole graph.
function EgoGraph({ concept, neighbors, onSelect }) {
  const shown = neighbors.slice(0, MAX_NEIGHBORS)
  const width = 460
  const height = 300
  const cx = width / 2
  const cy = height / 2
  const radius = 110

  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="w-full">
      {shown.map((n, idx) => {
        const angle = (idx / shown.length) * 2 * Math.PI - Math.PI / 2
        const x = cx + radius * Math.cos(angle)
        const y = cy + radius * Math.sin(angle)
        const midX = (cx + x) / 2
        const midY = (cy + y) / 2
        return (
          <g key={`${n.concept.id}-${idx}`}>
            <line x1={cx} y1={cy} x2={x} y2={y} stroke="#c4c7c7" strokeWidth={1 + n.edge.weight * 2} strokeOpacity={0.55} />
            <text x={midX} y={midY - 3} textAnchor="middle" fontSize={7.5} fill="#747878">
              {relationLabel(n.edge.relation)}{n.direction === 'in' ? ' ←' : ' →'}
            </text>
            <g className="cursor-pointer" onClick={() => onSelect(n.concept.id)}>
              <circle cx={x} cy={y} r={16} fill="#ffffff" stroke={CONCEPT_TYPE_COLORS[n.concept.type] || '#c4c7c7'} strokeWidth={2} />
              <text x={x} y={y > cy ? y + 27 : y - 21} textAnchor="middle" fontSize={8.5} fontWeight={500} fill="#1c1b1b">
                {n.concept.name.length > 18 ? `${n.concept.name.slice(0, 17)}…` : n.concept.name}
              </text>
            </g>
          </g>
        )
      })}
      <circle cx={cx} cy={cy} r={24} fill={CONCEPT_TYPE_COLORS[concept.type] || '#1c1b1b'} />
      <text x={cx} y={cy - 30} textAnchor="middle" fontSize={10} fontWeight={600} fill="#1c1b1b">
        {concept.name}
      </text>
      {neighbors.length > shown.length && (
        <text x={width - 6} y={height - 8} textAnchor="end" fontSize={8} fill="#747878">
          +{neighbors.length - shown.length} quan hệ khác
        </text>
      )}
    </svg>
  )
}

export default function KnowledgeGraphExplorer({ graph, loading, error, onReload }) {
  const [query, setQuery] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [selectedId, setSelectedId] = useState(null)

  const conceptById = useMemo(() => {
    const map = {}
    for (const c of graph?.concepts || []) map[c.id] = c
    return map
  }, [graph])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return (graph?.concepts || []).filter((c) => {
      if (typeFilter && c.type !== typeFilter) return false
      if (!q) return true
      return (
        c.name.toLowerCase().includes(q) ||
        c.id.toLowerCase().includes(q) ||
        (c.aliases || []).some((a) => a.toLowerCase().includes(q))
      )
    })
  }, [graph, query, typeFilter])

  const selected = selectedId ? conceptById[selectedId] : null

  const neighbors = useMemo(() => {
    if (!selected || !graph) return []
    const result = []
    for (const edge of graph.edges) {
      if (edge.source === selected.id && conceptById[edge.target]) {
        result.push({ edge, concept: conceptById[edge.target], direction: 'out' })
      } else if (edge.target === selected.id && conceptById[edge.source]) {
        result.push({ edge, concept: conceptById[edge.source], direction: 'in' })
      }
    }
    return result
  }, [selected, graph, conceptById])

  const selectedRules = useMemo(() => {
    if (!selected || !graph) return []
    return graph.rules.filter((r) => r.concept_id === selected.id)
  }, [selected, graph])

  if (loading) {
    return <div className="py-16 text-center text-sm text-on-surface-variant">Đang tải đồ thị tri thức từ Neo4j...</div>
  }
  if (error) {
    return (
      <div className="py-16 text-center">
        <p className="text-sm text-error mb-3">{error}</p>
        <button onClick={onReload} className="px-3 py-1.5 rounded-lg text-xs font-medium bg-primary text-on-primary hover:opacity-90">
          Thử lại
        </button>
      </div>
    )
  }
  if (!graph) return null

  return (
    <div className="space-y-4">
      {/* Stats */}
      <div className="grid grid-cols-3 gap-4">
        {[
          { icon: Boxes, label: 'Khái niệm', value: graph.concepts.length },
          { icon: GitBranch, label: 'Quan hệ', value: graph.edges.length },
          { icon: Lightbulb, label: 'Quy tắc', value: graph.rules.length },
        ].map(({ icon: Icon, label, value }) => (
          <div key={label} className="bg-surface-container-lowest rounded-xl p-4 ambient-shadow flex items-center gap-3">
            <Icon size={18} className="text-primary" />
            <div>
              <p className="text-xl font-semibold text-primary leading-none">{value}</p>
              <p className="text-xs text-on-surface-variant mt-1">{label}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* Concept browser */}
        <div className="lg:col-span-4 bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden flex flex-col">
          <div className="p-3 border-b border-outline-variant/20 space-y-2">
            <div className="flex items-center gap-2 bg-surface-container-low rounded-lg px-2.5 py-1.5">
              <Search size={13} className="text-on-surface-variant shrink-0" />
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Tìm khái niệm..."
                className="flex-1 bg-transparent text-xs text-on-surface outline-none placeholder:text-on-surface-variant/60"
              />
            </div>
            <div className="flex items-center gap-2">
              <select
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
                className="flex-1 bg-surface-container-low border border-outline-variant/20 rounded-lg px-2 py-1.5 text-xs text-on-surface focus:outline-none"
              >
                <option value="">Mọi loại ({graph.concepts.length})</option>
                {Object.entries(CONCEPT_TYPE_LABELS).map(([type, label]) => (
                  <option key={type} value={type}>{label}</option>
                ))}
              </select>
              <button onClick={onReload} title="Làm mới" className="p-1.5 rounded-lg bg-surface-container-low hover:bg-surface-container-high">
                <RefreshCw size={13} className="text-on-surface-variant" />
              </button>
            </div>
          </div>
          <div className="flex-1 overflow-y-auto custom-scrollbar max-h-[480px]">
            {filtered.length === 0 && (
              <p className="p-4 text-xs text-on-surface-variant text-center">Không tìm thấy khái niệm nào.</p>
            )}
            {filtered.map((concept) => (
              <button
                key={concept.id}
                onClick={() => setSelectedId(concept.id)}
                className={`w-full flex items-center gap-2.5 px-3 py-2 text-left transition-colors ${
                  concept.id === selectedId ? 'bg-primary text-on-primary' : 'hover:bg-surface-container-low'
                }`}
              >
                <span
                  className="w-2 h-2 rounded-full shrink-0"
                  style={{ backgroundColor: CONCEPT_TYPE_COLORS[concept.type] || '#c4c7c7' }}
                />
                <span className="flex-1 min-w-0">
                  <span className="block text-xs font-medium truncate">{concept.name}</span>
                  <span className={`block text-[10px] truncate ${concept.id === selectedId ? 'text-on-primary/70' : 'text-on-surface-variant'}`}>
                    {conceptTypeLabel(concept.type)}
                    {concept.ingested ? ' · từ nguồn ingest' : ''}
                  </span>
                </span>
              </button>
            ))}
          </div>
        </div>

        {/* Detail + ego graph */}
        <div className="lg:col-span-8 bg-surface-container-lowest rounded-xl p-5 ambient-shadow">
          {!selected ? (
            <div className="py-20 text-center text-sm text-on-surface-variant">
              Chọn một khái niệm bên trái để xem quan hệ và quy tắc của nó.
            </div>
          ) : (
            <div className="space-y-4">
              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <span
                    className="w-3 h-3 rounded-full"
                    style={{ backgroundColor: CONCEPT_TYPE_COLORS[selected.type] || '#c4c7c7' }}
                  />
                  <h3 className="font-title-lg text-primary">{selected.name}</h3>
                  <span className="px-2 py-0.5 rounded-full bg-surface-container-high text-[10px] text-on-surface-variant">
                    {conceptTypeLabel(selected.type)}
                  </span>
                  <span className="text-[10px] font-mono text-on-surface-variant">{selected.id}</span>
                </div>
                {selected.description && (
                  <p className="text-sm text-on-surface-variant mt-1.5">{selected.description}</p>
                )}
                {selected.aliases?.length > 0 && (
                  <div className="flex flex-wrap gap-1 mt-2">
                    {selected.aliases.map((alias) => (
                      <span key={alias} className="px-1.5 py-0.5 rounded bg-surface-container text-[10px] text-on-surface-variant">
                        {alias}
                      </span>
                    ))}
                  </div>
                )}
              </div>

              {neighbors.length > 0 ? (
                <div className="bg-surface-container-low/60 rounded-xl">
                  <EgoGraph concept={selected} neighbors={neighbors} onSelect={setSelectedId} />
                </div>
              ) : (
                <p className="text-xs text-on-surface-variant">Khái niệm này chưa có quan hệ nào với khái niệm khác.</p>
              )}

              {/* Reuse review layout for this concept's edges & rules */}
              <KnowledgeSourceReview
                concepts={[selected, ...neighbors.map((n) => n.concept)].filter(
                  (c, i, arr) => arr.findIndex((x) => x.id === c.id) === i
                )}
                edges={neighbors.map((n) => n.edge)}
                rules={selectedRules}
                sections={['edges', 'rules']}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
