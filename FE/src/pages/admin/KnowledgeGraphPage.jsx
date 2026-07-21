import { useState, useEffect, useMemo } from 'react'
import {
  Network,
  ZoomIn,
  ZoomOut,
  RefreshCw,
  GitBranch,
  Info,
  Tag,
  FileText,
  Search,
  Sparkles,
  Layers,
  ArrowRight,
} from 'lucide-react'
import { getKnowledgeGraphOverview } from '../../features/ai-stylist/adminKnowledge.api'

// Color map tailored for luxury/modern fashion design system
const TYPE_CONFIG = {
  occasion: {
    label: 'Dịp sử dụng',
    color: '#f59e0b', // Amber
    bg: '#fef3c7',
    border: '#d97706',
    ring: 1,
  },
  body_context: {
    label: 'Vóc dáng',
    color: '#6366f1', // Indigo
    bg: '#e0e7ff',
    border: '#4f46e5',
    ring: 1,
  },
  style_preference: {
    label: 'Phong cách',
    color: '#ec4899', // Pink
    bg: '#fce7f3',
    border: '#db2777',
    ring: 2,
  },
  color: {
    label: 'Màu sắc',
    color: '#06b6d4', // Cyan
    bg: '#cffaff',
    border: '#0891b2',
    ring: 2,
  },
  item_type: {
    label: 'Sản phẩm',
    color: '#10b981', // Emerald
    bg: '#d1fae5',
    border: '#059669',
    ring: 3,
  },
}

function getTypeMeta(type) {
  const normalized = (type || '').toLowerCase()
  if (normalized.includes('occasion')) return TYPE_CONFIG.occasion
  if (normalized.includes('body')) return TYPE_CONFIG.body_context
  if (normalized.includes('style') || normalized.includes('pref')) return TYPE_CONFIG.style_preference
  if (normalized.includes('color')) return TYPE_CONFIG.color
  if (normalized.includes('item')) return TYPE_CONFIG.item_type
  return {
    label: type || 'Khái niệm',
    color: '#64748b',
    bg: '#f1f5f9',
    border: '#475569',
    ring: 2,
  }
}

export default function KnowledgeGraphPage() {
  const [graphData, setGraphData] = useState({ concepts: [], rules: [] })
  const [selectedNodeId, setSelectedNodeId] = useState(null)
  const [hoveredNodeId, setHoveredNodeId] = useState(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [activeTypeFilter, setActiveTypeFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [zoomLevel, setZoomLevel] = useState(1)

  const fetchGraph = () => {
    setLoading(true)
    getKnowledgeGraphOverview()
      .then((res) => {
        if (!res) {
          setGraphData({ concepts: [], rules: [] })
          return
        }
        const concepts = res.concepts || res.nodes || []
        const rules = res.rules || res.relationships || []
        setGraphData({
          concepts: Array.isArray(concepts) ? concepts : [],
          rules: Array.isArray(rules) ? rules : [],
        })
        if (concepts.length > 0) {
          setSelectedNodeId(concepts[0].id)
        }
      })
      .catch(() => setGraphData({ concepts: [], rules: [] }))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    fetchGraph()
  }, [])

  // Filter concepts based on search & category filter
  const filteredConcepts = useMemo(() => {
    return graphData.concepts.filter((c) => {
      const meta = getTypeMeta(c.type)
      const matchesSearch =
        !searchTerm.trim() ||
        (c.name || c.id).toLowerCase().includes(searchTerm.toLowerCase()) ||
        (c.description || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
        (c.aliases || []).some((a) => a.toLowerCase().includes(searchTerm.toLowerCase()))

      const matchesFilter =
        activeTypeFilter === 'ALL' ||
        (activeTypeFilter === 'OCCASION' && c.type?.toLowerCase().includes('occasion')) ||
        (activeTypeFilter === 'BODY' && c.type?.toLowerCase().includes('body')) ||
        (activeTypeFilter === 'STYLE' && (c.type?.toLowerCase().includes('style') || c.type?.toLowerCase().includes('pref'))) ||
        (activeTypeFilter === 'ITEM' && c.type?.toLowerCase().includes('item')) ||
        (activeTypeFilter === 'COLOR' && c.type?.toLowerCase().includes('color'))

      return matchesSearch && matchesFilter
    })
  }, [graphData.concepts, searchTerm, activeTypeFilter])

  // Extract directional links between concepts based on rules payload
  const links = useMemo(() => {
    const conceptIdMap = new Map(graphData.concepts.map((c) => [c.id, c]))
    const linkList = []

    graphData.rules.forEach((rule) => {
      const sourceId = rule.concept_id
      if (!sourceId || !conceptIdMap.has(sourceId)) return

      // Look for target concept IDs mentioned in payload items/targets
      const payloadStr = JSON.stringify(rule.payload || {}).toLowerCase()

      graphData.concepts.forEach((targetConcept) => {
        if (targetConcept.id === sourceId) return

        // Match concept ID or item names inside rule payload
        const targetCleanId = targetConcept.id.toLowerCase().replace('item_', '').replace('style_', '')

        if (payloadStr.includes(targetConcept.id.toLowerCase()) || (targetCleanId.length > 3 && payloadStr.includes(targetCleanId))) {
          linkList.push({
            id: `${sourceId}->${targetConcept.id}`,
            source: sourceId,
            target: targetConcept.id,
            ruleType: rule.type,
            ruleText: rule.payload?.rule || '',
          })
        }
      })
    })

    return linkList
  }, [graphData.concepts, graphData.rules])

  // Compute multi-ring concentric layout positions for nodes
  const layout = useMemo(() => {
    const center = { x: 450, y: 375 }

    // Group filtered concepts into 3 concentric rings based on type
    const ring1 = [] // Occasions & Body (Inner ring, Radius 160)
    const ring2 = [] // Styles & Colors (Middle ring, Radius 260)
    const ring3 = [] // Items (Outer ring, Radius 350)

    filteredConcepts.forEach((c) => {
      const meta = getTypeMeta(c.type)
      if (meta.ring === 1) ring1.push(c)
      else if (meta.ring === 2) ring2.push(c)
      else ring3.push(c)
    })

    const positions = {}

    const placeRing = (nodes, radius) => {
      const step = (2 * Math.PI) / (nodes.length || 1)
      nodes.forEach((node, idx) => {
        const angle = idx * step - Math.PI / 2
        positions[node.id] = {
          x: center.x + radius * Math.cos(angle),
          y: center.y + radius * Math.sin(angle),
        }
      })
    }

    placeRing(ring1, 150)
    placeRing(ring2, 250)
    placeRing(ring3, 340)

    return { center, positions }
  }, [filteredConcepts])

  const selectedNode = graphData.concepts.find((c) => c.id === selectedNodeId)
  const selectedNodeRules = graphData.rules.filter((r) => r.concept_id === selectedNodeId)

  // Active links connected to selected or hovered node
  const activeNodeId = hoveredNodeId || selectedNodeId
  const activeLinks = useMemo(() => {
    if (!activeNodeId) return new Set()
    const active = new Set()
    links.forEach((l) => {
      if (l.source === activeNodeId || l.target === activeNodeId) {
        active.add(l.id)
        active.add(l.source)
        active.add(l.target)
      }
    })
    return active
  }, [activeNodeId, links])

  return (
    <div className="space-y-6">
      {/* Header & Controls */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="font-headline-md text-primary flex items-center gap-2">
            <Network className="text-primary" size={28} /> Đồ thị tri thức AI
          </h1>
          <p className="text-sm text-on-surface-variant mt-1">
            Trực quan hóa quan hệ giữa Khái niệm (Concepts) và Quy tắc phối đồ (Rules) từ AI Stylist Engine
          </p>
        </div>
        <div className="flex items-center gap-3">
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm khái niệm, từ khóa..."
              className="pl-9 pr-4 py-2 rounded-xl text-xs bg-surface-container-lowest border border-outline-variant focus:outline-none focus:border-primary w-48 lg:w-64"
            />
          </div>
          <div className="flex gap-1 bg-surface-container-lowest p-1 rounded-xl border border-outline-variant/30">
            <button
              onClick={() => setZoomLevel((z) => Math.min(z + 0.2, 1.6))}
              className="p-1.5 rounded-lg hover:bg-surface-container-high text-on-surface"
              title="Phóng to"
            >
              <ZoomIn size={16} />
            </button>
            <button
              onClick={() => setZoomLevel((z) => Math.max(z - 0.2, 0.6))}
              className="p-1.5 rounded-lg hover:bg-surface-container-high text-on-surface"
              title="Thu nhỏ"
            >
              <ZoomOut size={16} />
            </button>
            <button
              onClick={fetchGraph}
              className="p-1.5 rounded-lg hover:bg-surface-container-high text-on-surface"
              title="Tải lại"
            >
              <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
            </button>
          </div>
        </div>
      </div>

      {/* Category Filter Chips */}
      <div className="flex flex-wrap items-center gap-2">
        <span className="text-xs font-label-sm uppercase text-on-surface-variant flex items-center gap-1 mr-2">
          <Layers size={14} /> Phân loại:
        </span>
        {[
          { key: 'ALL', label: `Tất cả (${graphData.concepts.length})` },
          { key: 'OCCASION', label: 'Dịp sử dụng' },
          { key: 'BODY', label: 'Vóc dáng' },
          { key: 'STYLE', label: 'Phong cách' },
          { key: 'ITEM', label: 'Sản phẩm' },
          { key: 'COLOR', label: 'Màu sắc' },
        ].map((chip) => (
          <button
            key={chip.key}
            onClick={() => setActiveTypeFilter(chip.key)}
            className={`px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
              activeTypeFilter === chip.key
                ? 'bg-primary text-on-primary shadow-sm'
                : 'bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-high border border-outline-variant/30'
            }`}
          >
            {chip.label}
          </button>
        ))}
      </div>

      {/* Main Canvas + Detail Drawer */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* SVG Graph Canvas */}
        <div className="lg:col-span-8 bg-surface-container-lowest rounded-2xl border border-outline-variant/30 ambient-shadow overflow-hidden relative min-h-[600px] flex flex-col justify-between">
          <div className="p-4 border-b border-outline-variant/20 flex items-center justify-between text-xs text-on-surface-variant bg-surface-container-low/40">
            <span className="font-medium text-primary flex items-center gap-1">
              <Sparkles size={14} className="text-amber-500" /> Sơ đồ mối quan hệ AI ({filteredConcepts.length} khái niệm, {links.length} quy tắc liên kết)
            </span>
            <span>Rê chuột để xem liên kết • Nhấp để chọn</span>
          </div>

          {loading ? (
            <div className="flex items-center justify-center h-[520px] text-sm text-on-surface-variant gap-2">
              <RefreshCw size={18} className="animate-spin text-primary" /> Đang đồng bộ đồ thị từ AI Service...
            </div>
          ) : filteredConcepts.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-[520px] text-center p-8 space-y-3">
              <Info size={36} className="text-on-surface-variant/50" />
              <p className="text-sm font-medium text-primary">Không tìm thấy khái niệm phù hợp</p>
              <button
                onClick={() => {
                  setSearchTerm('')
                  setActiveTypeFilter('ALL')
                }}
                className="text-xs text-primary underline"
              >
                Đặt lại bộ lọc
              </button>
            </div>
          ) : (
            <div className="w-full h-[540px] overflow-auto flex items-center justify-center p-4">
              <svg
                viewBox="0 0 900 750"
                className="w-full h-full transition-transform duration-300"
                style={{ transform: `scale(${zoomLevel})` }}
              >
                {/* SVG Definitions for Arrowhead Markers */}
                <defs>
                  <marker
                    id="arrow-default"
                    viewBox="0 0 10 10"
                    refX="22"
                    refY="5"
                    markerWidth="6"
                    markerHeight="6"
                    orient="auto-start-reverse"
                  >
                    <path d="M 0 0 L 10 5 L 0 10 z" fill="#cbd5e1" />
                  </marker>
                  <marker
                    id="arrow-active"
                    viewBox="0 0 10 10"
                    refX="24"
                    refY="5"
                    markerWidth="8"
                    markerHeight="8"
                    orient="auto-start-reverse"
                  >
                    <path d="M 0 0 L 10 5 L 0 10 z" fill="#d97706" />
                  </marker>
                </defs>

                {/* Concentric Guide Circles */}
                <circle cx={layout.center.x} cy={layout.center.y} r={150} fill="none" stroke="#f1f5f9" strokeWidth={1.5} strokeDasharray="4 4" />
                <circle cx={layout.center.y} cy={layout.center.y} r={250} fill="none" stroke="#f1f5f9" strokeWidth={1.5} strokeDasharray="4 4" />
                <circle cx={layout.center.x} cy={layout.center.y} r={340} fill="none" stroke="#f8fafc" strokeWidth={1} strokeDasharray="4 4" />

                {/* Center Hub */}
                <g transform={`translate(${layout.center.x}, ${layout.center.y})`}>
                  <circle r={36} fill="#0f172a" opacity={0.9} />
                  <text textAnchor="middle" y={-2} fontSize={10} fontWeight={700} fill="#ffffff">
                    StyleMind
                  </text>
                  <text textAnchor="middle" y={10} fontSize={8} fontWeight={500} fill="#94a3b8">
                    AI Engine
                  </text>
                </g>

                {/* Directional Connection Edges (Arrows) */}
                {links.map((link) => {
                  const p1 = layout.positions[link.source]
                  const p2 = layout.positions[link.target]
                  if (!p1 || !p2) return null

                  const isActive = activeLinks.has(link.id)
                  const isHighlighted = activeNodeId && (link.source === activeNodeId || link.target === activeNodeId)

                  // Compute quadratic curve control point
                  const midX = (p1.x + p2.x) / 2
                  const midY = (p1.y + p2.y) / 2
                  const dx = p2.x - p1.x
                  const dy = p2.y - p1.y
                  const norm = Math.sqrt(dx * dx + dy * dy) || 1
                  const controlX = midX - (dy / norm) * 25
                  const controlY = midY + (dx / norm) * 25

                  return (
                    <path
                      key={link.id}
                      d={`M ${p1.x} ${p1.y} Q ${controlX} ${controlY} ${p2.x} ${p2.y}`}
                      fill="none"
                      stroke={isHighlighted ? '#d97706' : '#cbd5e1'}
                      strokeWidth={isHighlighted ? 2.5 : 1}
                      strokeOpacity={activeNodeId ? (isHighlighted ? 1 : 0.15) : 0.4}
                      markerEnd={isHighlighted ? 'url(#arrow-active)' : 'url(#arrow-default)'}
                      className="transition-all duration-300"
                    />
                  )
                })}

                {/* Concept Nodes */}
                {filteredConcepts.map((node) => {
                  const pos = layout.positions[node.id]
                  if (!pos) return null

                  const meta = getTypeMeta(node.type)
                  const isSelected = selectedNodeId === node.id
                  const isHovered = hoveredNodeId === node.id
                  const isConnected = activeLinks.has(node.id)
                  const isDimmed = activeNodeId && !isConnected && activeNodeId !== node.id

                  const radius = isSelected ? 24 : isHovered ? 22 : 18

                  return (
                    <g
                      key={node.id}
                      transform={`translate(${pos.x}, ${pos.y})`}
                      className="cursor-pointer transition-all duration-300"
                      opacity={isDimmed ? 0.25 : 1}
                      onClick={() => setSelectedNodeId(node.id)}
                      onMouseEnter={() => setHoveredNodeId(node.id)}
                      onMouseLeave={() => setHoveredNodeId(null)}
                    >
                      {/* Glow effect for selected node */}
                      {(isSelected || isHovered) && (
                        <circle r={radius + 6} fill={meta.color} opacity={0.25} className="animate-pulse" />
                      )}

                      {/* Main Node Circle */}
                      <circle
                        r={radius}
                        fill={meta.bg}
                        stroke={isSelected ? '#000000' : meta.border}
                        strokeWidth={isSelected ? 3 : 2}
                      />

                      {/* Label Text */}
                      <text
                        textAnchor="middle"
                        y={3}
                        fontSize={9}
                        fontWeight={600}
                        fill="#0f172a"
                        pointerEvents="none"
                      >
                        {(node.name || node.id).slice(0, 10)}
                      </text>
                    </g>
                  )
                })}
              </svg>
            </div>
          )}

          {/* Type Legend Footer */}
          <div className="p-3 bg-surface-container-low/60 border-t border-outline-variant/20 flex flex-wrap items-center justify-around text-xs gap-3">
            {Object.entries(TYPE_CONFIG).map(([key, cfg]) => (
              <div key={key} className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded-full border border-black/10" style={{ backgroundColor: cfg.color }} />
                <span className="text-on-surface-variant font-medium">{cfg.label}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Detail Panel */}
        <div className="lg:col-span-4 space-y-4">
          {/* Concept Detail */}
          <div className="bg-surface-container-lowest rounded-2xl p-5 border border-outline-variant/30 ambient-shadow space-y-4">
            {selectedNode ? (
              <>
                <div className="flex items-start justify-between border-b border-outline-variant/20 pb-3">
                  <div>
                    <span
                      className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full inline-block mb-1"
                      style={{
                        backgroundColor: getTypeMeta(selectedNode.type).bg,
                        color: getTypeMeta(selectedNode.type).border,
                      }}
                    >
                      {getTypeMeta(selectedNode.type).label}
                    </span>
                    <h3 className="font-title-lg text-primary font-bold">{selectedNode.name || selectedNode.id}</h3>
                  </div>
                  <code className="text-[10px] bg-surface-container px-2 py-1 rounded text-on-surface-variant font-mono">
                    {selectedNode.id}
                  </code>
                </div>

                {selectedNode.description && (
                  <div>
                    <span className="text-[10px] uppercase font-bold text-on-surface-variant block mb-1">Mô tả</span>
                    <p className="text-xs text-on-surface bg-surface-container-low/60 p-3 rounded-xl leading-relaxed">
                      {selectedNode.description}
                    </p>
                  </div>
                )}

                {selectedNode.aliases && selectedNode.aliases.length > 0 && (
                  <div>
                    <span className="text-[10px] uppercase font-bold text-on-surface-variant block mb-1.5 flex items-center gap-1">
                      <Tag size={12} /> Từ đồng nghĩa (Aliases)
                    </span>
                    <div className="flex flex-wrap gap-1.5">
                      {selectedNode.aliases.map((alias, idx) => (
                        <span
                          key={idx}
                          className="text-xs bg-surface-container-high px-2.5 py-1 rounded-lg text-primary font-medium"
                        >
                          {alias}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </>
            ) : (
              <div className="text-center py-8 text-on-surface-variant text-sm">Nhấp vào một nút trên sơ đồ để xem chi tiết</div>
            )}
          </div>

          {/* AI Rules associated with selected Concept */}
          <div className="bg-surface-container-lowest rounded-2xl p-5 border border-outline-variant/30 ambient-shadow space-y-3">
            <div className="flex items-center justify-between border-b border-outline-variant/20 pb-2">
              <h3 className="font-title-md text-primary font-semibold flex items-center gap-2 text-xs uppercase tracking-wider">
                <FileText size={15} /> Quy tắc phối đồ AI ({selectedNodeRules.length})
              </h3>
            </div>

            {selectedNodeRules.length === 0 ? (
              <p className="text-xs text-on-surface-variant italic text-center py-4">
                Chưa có quy tắc phối đồ riêng cho khái niệm này.
              </p>
            ) : (
              <div className="space-y-2.5 max-h-[300px] overflow-y-auto pr-1">
                {selectedNodeRules.map((rule, idx) => (
                  <div key={rule.id || idx} className="bg-surface-container-low rounded-xl p-3.5 text-xs space-y-1.5 border border-outline-variant/20">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-primary flex items-center gap-1 font-mono text-[11px]">
                        <GitBranch size={12} className="text-amber-600" /> {rule.type}
                      </span>
                      {rule.priority && (
                        <span className="text-[10px] text-on-surface-variant bg-surface-container px-2 py-0.5 rounded-full font-medium">
                          Ưu tiên: {rule.priority}
                        </span>
                      )}
                    </div>
                    {rule.payload?.rule && (
                      <p className="text-on-surface text-xs leading-relaxed italic pt-1 border-t border-outline-variant/10">
                        "{rule.payload.rule}"
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
