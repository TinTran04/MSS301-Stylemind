import { useState, useEffect, useCallback, useMemo } from 'react'
import {
  Network,
  FileText,
  Plus,
  Loader2,
  CheckCircle,
  Trash2,
  ExternalLink,
  AlertTriangle,
  ZoomIn,
  ZoomOut,
  RefreshCw,
  GitBranch,
  Info,
  Tag,
  Search,
  Sparkles,
  Layers,
} from 'lucide-react'
import StatusBadge from '../../components/admin/StatusBadge'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog'
import KnowledgeSourceReview from '../../components/admin/KnowledgeSourceReview'
import useAuthStore from '../../features/auth/auth.store'
import {
  ingestKnowledge,
  listKnowledgeSources,
  getKnowledgeSource,
  approveKnowledgeSource,
  deleteKnowledgeSource,
  getKnowledgeGraphOverview,
} from '../../features/ai-stylist/adminKnowledge.api'
import { formatDateTime } from '../../utils/formatDate'

const TABS = [
  { id: 'sources', label: 'Nguồn tri thức', icon: FileText },
  { id: 'graph', label: 'Đồ thị tri thức', icon: Network },
]

// Color map tailored for luxury/modern fashion design system
const TYPE_CONFIG = {
  occasion: { label: 'Dịp sử dụng', color: '#f59e0b', bg: '#fef3c7', border: '#d97706', ring: 1 },
  body_context: { label: 'Vóc dáng', color: '#6366f1', bg: '#e0e7ff', border: '#4f46e5', ring: 1 },
  style_preference: { label: 'Phong cách', color: '#ec4899', bg: '#fce7f3', border: '#db2777', ring: 2 },
  color: { label: 'Màu sắc', color: '#06b6d4', bg: '#cffaff', border: '#0891b2', ring: 2 },
  item_type: { label: 'Sản phẩm', color: '#10b981', bg: '#d1fae5', border: '#059669', ring: 3 },
}

function getTypeMeta(type) {
  const normalized = (type || '').toLowerCase()
  if (normalized.includes('occasion')) return TYPE_CONFIG.occasion
  if (normalized.includes('body')) return TYPE_CONFIG.body_context
  if (normalized.includes('style') || normalized.includes('pref')) return TYPE_CONFIG.style_preference
  if (normalized.includes('color')) return TYPE_CONFIG.color
  if (normalized.includes('item')) return TYPE_CONFIG.item_type
  return { label: type || 'Khái niệm', color: '#64748b', bg: '#f1f5f9', border: '#475569', ring: 2 }
}

function IngestForm({ userId, onIngested }) {
  const [title, setTitle] = useState('')
  const [urlsText, setUrlsText] = useState('')
  const [rawText, setRawText] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const urls = urlsText.split('\n').map((u) => u.trim()).filter(Boolean)
  const texts = rawText.trim() ? [rawText.trim()] : []
  const canSubmit = !submitting && (urls.length > 0 || texts.length > 0)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!canSubmit) return
    setSubmitting(true)
    setError('')
    try {
      const result = await ingestKnowledge({ userId, title: title.trim(), texts, urls })
      setTitle('')
      setUrlsText('')
      setRawText('')
      onIngested(result)
    } catch (err) {
      setError(err.message || 'Không thể trích xuất tri thức. Vui lòng thử lại.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="bg-surface-container-lowest rounded-xl ambient-shadow p-5 space-y-3">
      <div className="flex items-center gap-2">
        <Plus size={15} className="text-primary" />
        <h2 className="font-title-lg text-primary">Nạp nguồn tri thức mới</h2>
      </div>
      <p className="text-xs text-on-surface-variant">
        Dán URL bài viết thời trang (mỗi dòng một URL) hoặc nội dung văn bản. Hệ thống sẽ trích xuất
        khái niệm, quan hệ và quy tắc để bạn duyệt trước khi ghi vào đồ thị tri thức.
      </p>

      {error && (
        <div role="alert" className="rounded-lg border border-error/20 bg-error-container/40 px-3 py-2 text-xs text-error flex items-center gap-2">
          <AlertTriangle size={13} className="shrink-0" /> {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        <div className="space-y-3">
          <div>
            <label className="block text-[10px] uppercase text-on-surface-variant mb-1">Tiêu đề (tùy chọn)</label>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="VD: Cẩm nang phối đồ mùa hè"
              maxLength={255}
              className="w-full bg-surface-container-low border border-outline-variant/20 rounded-lg px-3 py-2 text-sm focus:outline-none"
            />
          </div>
          <div>
            <label className="block text-[10px] uppercase text-on-surface-variant mb-1">URL bài viết (mỗi dòng một URL)</label>
            <textarea
              value={urlsText}
              onChange={(e) => setUrlsText(e.target.value)}
              rows={3}
              placeholder={'https://blog.example.com/phoi-do-cong-so'}
              className="w-full bg-surface-container-low border border-outline-variant/20 rounded-lg px-3 py-2 text-sm font-mono focus:outline-none resize-y"
            />
          </div>
        </div>
        <div>
          <label className="block text-[10px] uppercase text-on-surface-variant mb-1">Hoặc dán nội dung văn bản</label>
          <textarea
            value={rawText}
            onChange={(e) => setRawText(e.target.value)}
            rows={7}
            placeholder="Dán nội dung kiến thức thời trang tại đây..."
            className="w-full bg-surface-container-low border border-outline-variant/20 rounded-lg px-3 py-2 text-sm focus:outline-none resize-y"
          />
        </div>
      </div>

      <div className="flex items-center gap-3">
        <button
          type="submit"
          disabled={!canSubmit}
          className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-primary text-on-primary hover:opacity-90 disabled:opacity-50"
        >
          {submitting ? <Loader2 size={14} className="animate-spin" /> : <Plus size={14} />}
          {submitting ? 'Đang trích xuất...' : 'Trích xuất tri thức'}
        </button>
        {submitting && (
          <p className="text-xs text-on-surface-variant">
            Đang đọc nguồn và trích xuất bằng AI — có thể mất 1–3 phút.
          </p>
        )}
      </div>
    </form>
  )
}

function SourceDetailPanel({ detail, loading, onApprove, onDelete, approving }) {
  if (loading) {
    return (
      <div className="bg-surface-container-lowest rounded-xl ambient-shadow p-8 text-center text-sm text-on-surface-variant">
        Đang tải chi tiết nguồn tri thức...
      </div>
    )
  }
  if (!detail) return null

  const isPending = detail.status === 'pending'

  return (
    <div className="bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
      <div className="p-5 border-b border-outline-variant/20">
        <div className="flex items-start justify-between gap-4 flex-wrap">
          <div className="min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <h2 className="font-title-lg text-primary">{detail.title || 'Nguồn không tiêu đề'}</h2>
              <StatusBadge status={detail.status} />
            </div>
            <p className="text-xs text-on-surface-variant mt-1">
              Tạo lúc {formatDateTime(detail.created_at)}
              {detail.approved_at ? ` · Duyệt lúc ${formatDateTime(detail.approved_at)}` : ''}
            </p>
            {detail.sources?.length > 0 && (
              <div className="mt-2 space-y-0.5">
                {detail.sources.map((src) => (
                  <p key={src} className="text-xs text-on-surface-variant flex items-center gap-1.5 min-w-0">
                    <ExternalLink size={10} className="shrink-0" />
                    {src.startsWith('http') ? (
                      <a href={src} target="_blank" rel="noreferrer" className="truncate hover:text-primary underline-offset-2 hover:underline">
                        {src}
                      </a>
                    ) : (
                      <span className="truncate">Văn bản dán trực tiếp ({src})</span>
                    )}
                  </p>
                ))}
              </div>
            )}
          </div>
          <div className="flex items-center gap-2 shrink-0">
            {isPending && (
              <button
                onClick={onApprove}
                disabled={approving}
                className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs font-medium bg-primary text-on-primary hover:opacity-90 disabled:opacity-50"
              >
                {approving ? <Loader2 size={12} className="animate-spin" /> : <CheckCircle size={12} />}
                {approving ? 'Đang ghi vào đồ thị...' : 'Duyệt & ghi vào đồ thị'}
              </button>
            )}
            <button
              onClick={onDelete}
              className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs font-medium bg-surface-container text-error hover:bg-error-container/40"
            >
              <Trash2 size={12} /> Xóa
            </button>
          </div>
        </div>
        {isPending && (
          <p className="mt-3 text-xs text-on-surface-variant bg-surface-container-low rounded-lg px-3 py-2">
            Hãy rà soát các khái niệm, quan hệ và quy tắc bên dưới. Nếu chính xác, bấm
            <span className="font-medium text-primary"> "Duyệt & ghi vào đồ thị"</span> — tri thức sẽ được
            ghi vào Neo4j và đánh chỉ mục vector. Nếu sai, hãy xóa và nạp lại nguồn khác.
          </p>
        )}
      </div>
      <div className="p-5">
        <KnowledgeSourceReview concepts={detail.concepts} edges={detail.edges} rules={detail.rules} />
      </div>
    </div>
  )
}

function GraphExplorerTab({ graphData, graphLoading, onReload }) {
  const [selectedNodeId, setSelectedNodeId] = useState(null)
  const [hoveredNodeId, setHoveredNodeId] = useState(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [activeTypeFilter, setActiveTypeFilter] = useState('ALL')
  const [zoomLevel, setZoomLevel] = useState(1)

  useEffect(() => {
    if (graphData.concepts.length > 0 && !selectedNodeId) {
      setSelectedNodeId(graphData.concepts[0].id)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [graphData.concepts])

  const filteredConcepts = useMemo(() => {
    return graphData.concepts.filter((c) => {
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

  const links = useMemo(() => {
    const conceptIdMap = new Map(graphData.concepts.map((c) => [c.id, c]))
    const linkList = []

    graphData.rules.forEach((rule) => {
      const sourceId = rule.concept_id
      if (!sourceId || !conceptIdMap.has(sourceId)) return

      const payloadStr = JSON.stringify(rule.payload || {}).toLowerCase()

      graphData.concepts.forEach((targetConcept) => {
        if (targetConcept.id === sourceId) return

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

  const layout = useMemo(() => {
    const center = { x: 450, y: 375 }
    const ring1 = []
    const ring2 = []
    const ring3 = []

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
    <div className="space-y-4">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
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
              onClick={onReload}
              className="p-1.5 rounded-lg hover:bg-surface-container-high text-on-surface"
              title="Tải lại"
            >
              <RefreshCw size={16} className={graphLoading ? 'animate-spin' : ''} />
            </button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-8 bg-surface-container-lowest rounded-2xl border border-outline-variant/30 ambient-shadow overflow-hidden relative min-h-[600px] flex flex-col justify-between">
          <div className="p-4 border-b border-outline-variant/20 flex items-center justify-between text-xs text-on-surface-variant bg-surface-container-low/40">
            <span className="font-medium text-primary flex items-center gap-1">
              <Sparkles size={14} className="text-amber-500" /> Sơ đồ mối quan hệ AI ({filteredConcepts.length} khái niệm, {links.length} quy tắc liên kết)
            </span>
            <span>Rê chuột để xem liên kết • Nhấp để chọn</span>
          </div>

          {graphLoading ? (
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
                <defs>
                  <marker id="arrow-default" viewBox="0 0 10 10" refX="22" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
                    <path d="M 0 0 L 10 5 L 0 10 z" fill="#cbd5e1" />
                  </marker>
                  <marker id="arrow-active" viewBox="0 0 10 10" refX="24" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
                    <path d="M 0 0 L 10 5 L 0 10 z" fill="#d97706" />
                  </marker>
                </defs>

                <circle cx={layout.center.x} cy={layout.center.y} r={150} fill="none" stroke="#f1f5f9" strokeWidth={1.5} strokeDasharray="4 4" />
                <circle cx={layout.center.y} cy={layout.center.y} r={250} fill="none" stroke="#f1f5f9" strokeWidth={1.5} strokeDasharray="4 4" />
                <circle cx={layout.center.x} cy={layout.center.y} r={340} fill="none" stroke="#f8fafc" strokeWidth={1} strokeDasharray="4 4" />

                <g transform={`translate(${layout.center.x}, ${layout.center.y})`}>
                  <circle r={36} fill="#0f172a" opacity={0.9} />
                  <text textAnchor="middle" y={-2} fontSize={10} fontWeight={700} fill="#ffffff">StyleMind</text>
                  <text textAnchor="middle" y={10} fontSize={8} fontWeight={500} fill="#94a3b8">AI Engine</text>
                </g>

                {links.map((link) => {
                  const p1 = layout.positions[link.source]
                  const p2 = layout.positions[link.target]
                  if (!p1 || !p2) return null

                  const isActive = activeLinks.has(link.id)
                  const isHighlighted = activeNodeId && (link.source === activeNodeId || link.target === activeNodeId)

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
                      {(isSelected || isHovered) && (
                        <circle r={radius + 6} fill={meta.color} opacity={0.25} className="animate-pulse" />
                      )}
                      <circle r={radius} fill={meta.bg} stroke={isSelected ? '#000000' : meta.border} strokeWidth={isSelected ? 3 : 2} />
                      <text textAnchor="middle" y={3} fontSize={9} fontWeight={600} fill="#0f172a" pointerEvents="none">
                        {(node.name || node.id).slice(0, 10)}
                      </text>
                    </g>
                  )
                })}
              </svg>
            </div>
          )}

          <div className="p-3 bg-surface-container-low/60 border-t border-outline-variant/20 flex flex-wrap items-center justify-around text-xs gap-3">
            {Object.entries(TYPE_CONFIG).map(([key, cfg]) => (
              <div key={key} className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded-full border border-black/10" style={{ backgroundColor: cfg.color }} />
                <span className="text-on-surface-variant font-medium">{cfg.label}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="lg:col-span-4 space-y-4">
          <div className="bg-surface-container-lowest rounded-2xl p-5 border border-outline-variant/30 ambient-shadow space-y-4">
            {selectedNode ? (
              <>
                <div className="flex items-start justify-between border-b border-outline-variant/20 pb-3">
                  <div>
                    <span
                      className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full inline-block mb-1"
                      style={{ backgroundColor: getTypeMeta(selectedNode.type).bg, color: getTypeMeta(selectedNode.type).border }}
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
                        <span key={idx} className="text-xs bg-surface-container-high px-2.5 py-1 rounded-lg text-primary font-medium">
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

export default function KnowledgeGraphPage() {
  const user = useAuthStore((s) => s.user)
  const [tab, setTab] = useState('sources')

  // Sources state
  const [sourcesList, setSourcesList] = useState([])
  const [loadingSources, setLoadingSources] = useState(true)
  const [listError, setListError] = useState('')
  const [selectedId, setSelectedId] = useState(null)
  const [detail, setDetail] = useState(null)
  const [loadingDetail, setLoadingDetail] = useState(false)
  const [toast, setToast] = useState('')
  const [approving, setApproving] = useState(false)
  const [confirm, setConfirm] = useState(null) // { type: 'approve' | 'delete', sourceId }

  // Graph state (loaded lazily when the tab is first opened)
  const [graphData, setGraphData] = useState({ concepts: [], rules: [] })
  const [graphLoading, setGraphLoading] = useState(false)
  const [graphLoaded, setGraphLoaded] = useState(false)

  const showToast = (msg) => {
    setToast(msg)
    setTimeout(() => setToast(''), 5000)
  }

  const fetchSources = useCallback(async () => {
    if (!user?.id) return
    setLoadingSources(true)
    setListError('')
    try {
      setSourcesList(await listKnowledgeSources(user.id))
    } catch (err) {
      setListError(err.message || 'Không thể tải danh sách nguồn tri thức.')
    } finally {
      setLoadingSources(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchSources()
  }, [fetchSources])

  const loadGraph = useCallback(async () => {
    setGraphLoading(true)
    try {
      const res = await getKnowledgeGraphOverview()
      const concepts = res?.concepts || res?.nodes || []
      const rules = res?.rules || res?.relationships || []
      setGraphData({
        concepts: Array.isArray(concepts) ? concepts : [],
        rules: Array.isArray(rules) ? rules : [],
      })
    } catch {
      setGraphData({ concepts: [], rules: [] })
    } finally {
      setGraphLoading(false)
      setGraphLoaded(true)
    }
  }, [])

  useEffect(() => {
    if (tab === 'graph' && !graphLoaded && !graphLoading) loadGraph()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab])

  const selectSource = useCallback(async (sourceId) => {
    setSelectedId(sourceId)
    setLoadingDetail(true)
    try {
      setDetail(await getKnowledgeSource(sourceId))
    } catch (err) {
      setDetail(null)
      showToast(err.message || 'Không thể tải chi tiết nguồn.')
    } finally {
      setLoadingDetail(false)
    }
  }, [])

  const handleIngested = async (result) => {
    showToast(`Đã trích xuất ${result.concepts.length} khái niệm, ${result.edges.length} quan hệ, ${result.rules.length} quy tắc. Hãy rà soát và duyệt bên dưới.`)
    await fetchSources()
    await selectSource(result.source_id)
  }

  const handleApprove = async () => {
    setConfirm(null)
    setApproving(true)
    try {
      const result = await approveKnowledgeSource(selectedId)
      showToast(
        `Đã ghi vào đồ thị: ${result.concepts_upserted} khái niệm, ${result.edges_upserted} quan hệ, ` +
        `${result.rules_upserted} quy tắc, ${result.concept_vectors_upserted} vector.`
      )
      setGraphLoaded(false) // graph tab reloads fresh data next time it's opened
      await fetchSources()
      await selectSource(selectedId)
    } catch (err) {
      showToast(err.message || 'Không thể duyệt nguồn tri thức.')
    } finally {
      setApproving(false)
    }
  }

  const handleDelete = async () => {
    const sourceId = confirm?.sourceId
    setConfirm(null)
    try {
      await deleteKnowledgeSource(sourceId)
      showToast('Đã xóa nguồn tri thức.')
      if (sourceId === selectedId) {
        setSelectedId(null)
        setDetail(null)
      }
      setGraphLoaded(false)
      await fetchSources()
    } catch (err) {
      showToast(err.message || 'Không thể xóa nguồn tri thức.')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="font-headline-md text-primary">Tri thức AI</h1>
          <p className="text-sm text-on-surface-variant mt-1">
            Nạp nguồn tri thức thời trang, rà soát kết quả trích xuất và quản lý đồ thị tri thức
          </p>
        </div>
        <div className="flex rounded-lg bg-surface-container p-1">
          {TABS.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              onClick={() => setTab(id)}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-md text-xs font-medium transition-colors ${
                tab === id ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:text-primary'
              }`}
            >
              <Icon size={13} /> {label}
            </button>
          ))}
        </div>
      </div>

      {toast && (
        <div className="rounded-lg bg-primary/10 border border-primary/10 px-4 py-2.5 text-xs text-primary">{toast}</div>
      )}

      {tab === 'sources' ? (
        <div className="space-y-6">
          <IngestForm userId={user?.id} onIngested={handleIngested} />

          <div className="bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
            <div className="p-4 border-b border-outline-variant/20 flex items-center gap-2">
              <FileText size={15} className="text-primary" />
              <h2 className="font-title-lg text-primary">Danh sách nguồn</h2>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="bg-surface-container-low/50">
                    <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Tiêu đề</th>
                    <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Trạng thái</th>
                    <th className="text-center font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Khái niệm</th>
                    <th className="text-center font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Quan hệ</th>
                    <th className="text-center font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Quy tắc</th>
                    <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Ngày tạo</th>
                    <th className="px-4 py-3" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/5">
                  {loadingSources ? (
                    <tr><td colSpan={7} className="text-center py-8 text-sm text-on-surface-variant">Đang tải danh sách nguồn...</td></tr>
                  ) : listError ? (
                    <tr><td colSpan={7} className="text-center py-8 text-sm text-error">{listError}</td></tr>
                  ) : sourcesList.length === 0 ? (
                    <tr><td colSpan={7} className="text-center py-8 text-sm text-on-surface-variant">Chưa có nguồn tri thức nào. Hãy nạp nguồn đầu tiên ở trên.</td></tr>
                  ) : sourcesList.map((source) => (
                    <tr
                      key={source.id}
                      onClick={() => {
                        if (selectedId === source.id) {
                          setSelectedId(null)
                          setDetail(null)
                        } else {
                          selectSource(source.id)
                        }
                      }}
                      className={`cursor-pointer transition-colors hover:bg-surface-container-high/30 ${selectedId === source.id ? 'bg-surface-container-low' : ''}`}
                    >
                      <td className="px-4 py-3 text-sm font-medium text-primary max-w-[280px] truncate">
                        {source.title || 'Nguồn không tiêu đề'}
                      </td>
                      <td className="px-4 py-3"><StatusBadge status={source.status} /></td>
                      <td className="px-4 py-3 text-center text-sm text-on-surface">{source.concepts_count}</td>
                      <td className="px-4 py-3 text-center text-sm text-on-surface">{source.edges_count}</td>
                      <td className="px-4 py-3 text-center text-sm text-on-surface">{source.rules_count}</td>
                      <td className="px-4 py-3 text-xs text-on-surface-variant">{formatDateTime(source.created_at)}</td>
                      <td className="px-4 py-3 text-right">
                        <button
                          onClick={(e) => {
                            e.stopPropagation()
                            setConfirm({ type: 'delete', sourceId: source.id })
                          }}
                          title="Xóa nguồn"
                          className="p-1.5 rounded-md text-on-surface-variant hover:text-error hover:bg-error-container/30"
                        >
                          <Trash2 size={13} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <SourceDetailPanel
            detail={detail}
            loading={loadingDetail}
            approving={approving}
            onApprove={() => setConfirm({ type: 'approve', sourceId: selectedId })}
            onDelete={() => setConfirm({ type: 'delete', sourceId: selectedId })}
          />
        </div>
      ) : (
        <GraphExplorerTab graphData={graphData} graphLoading={graphLoading} onReload={loadGraph} />
      )}

      <AdminConfirmDialog
        open={confirm?.type === 'approve'}
        title="Duyệt nguồn tri thức?"
        message="Toàn bộ khái niệm, quan hệ và quy tắc đã trích xuất sẽ được ghi vào đồ thị tri thức Neo4j và đánh chỉ mục vector. AI sẽ dùng tri thức này khi tư vấn."
        confirmLabel="Duyệt & ghi"
        loading={approving}
        onConfirm={handleApprove}
        onCancel={() => setConfirm(null)}
      />
      <AdminConfirmDialog
        open={confirm?.type === 'delete'}
        title="Xóa nguồn tri thức?"
        message="Nguồn này sẽ bị xóa. Nếu đã duyệt, tri thức do nguồn này đóng góp cũng sẽ bị gỡ khỏi đồ thị Neo4j và chỉ mục vector."
        confirmLabel="Xóa"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setConfirm(null)}
      />
    </div>
  )
}
