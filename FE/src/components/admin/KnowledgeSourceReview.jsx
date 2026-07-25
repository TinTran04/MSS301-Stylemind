import { Lightbulb, GitBranch, Boxes, ArrowRight } from 'lucide-react'
import {
  conceptTypeLabel,
  relationLabel,
  ruleTypeLabel,
  CONCEPT_TYPE_COLORS,
  RULE_PAYLOAD_LABELS,
} from '../../features/ai-stylist/knowledge.constants'

// Renders one extracted knowledge delta (concepts / edges / rules) in a
// human-reviewable layout so admins can verify the extraction is correct
// before approving it into the live knowledge graph.

function TypeDot({ type }) {
  return (
    <span
      className="w-2 h-2 rounded-full shrink-0"
      style={{ backgroundColor: CONCEPT_TYPE_COLORS[type] || '#c4c7c7' }}
    />
  )
}

function SectionHeader({ icon: Icon, title, count }) {
  return (
    <div className="flex items-center gap-2 mb-3">
      <Icon size={15} className="text-primary" />
      <h3 className="font-title-lg text-primary">{title}</h3>
      <span className="px-2 py-0.5 rounded-full bg-surface-container-high text-xs font-medium text-on-surface-variant">
        {count}
      </span>
    </div>
  )
}

function PayloadValue({ value }) {
  if (value == null || value === '') return null
  if (typeof value === 'string' || typeof value === 'number') {
    return <span className="text-on-surface">{String(value)}</span>
  }
  if (Array.isArray(value)) {
    if (value.every((v) => typeof v === 'string' || typeof v === 'number')) {
      return (
        <span className="flex flex-wrap gap-1">
          {value.map((v, i) => (
            <span key={i} className="px-1.5 py-0.5 rounded bg-surface-container text-[11px] text-on-surface">
              {String(v)}
            </span>
          ))}
        </span>
      )
    }
    return (
      <pre className="text-[10px] text-on-surface-variant whitespace-pre-wrap break-words">
        {JSON.stringify(value, null, 1)}
      </pre>
    )
  }
  return (
    <pre className="text-[10px] text-on-surface-variant whitespace-pre-wrap break-words">
      {JSON.stringify(value, null, 1)}
    </pre>
  )
}

export default function KnowledgeSourceReview({
  concepts = [],
  edges = [],
  rules = [],
  // Which sections to render; concepts are always used for id → name lookup.
  sections = ['concepts', 'edges', 'rules'],
}) {
  const nameById = concepts.reduce((acc, c) => {
    acc[c.id] = c.name
    return acc
  }, {})
  const conceptName = (id) => nameById[id] || id

  const conceptsByType = concepts.reduce((acc, concept) => {
    ;(acc[concept.type] = acc[concept.type] || []).push(concept)
    return acc
  }, {})

  return (
    <div className="space-y-6">
      {/* Concepts grouped by type */}
      {sections.includes('concepts') && (
      <div>
        <SectionHeader icon={Boxes} title="Khái niệm" count={concepts.length} />
        {concepts.length === 0 && (
          <p className="text-sm text-on-surface-variant">Không có khái niệm nào được trích xuất.</p>
        )}
        <div className="space-y-4">
          {Object.entries(conceptsByType).map(([type, group]) => (
            <div key={type}>
              <div className="flex items-center gap-2 mb-2">
                <TypeDot type={type} />
                <span className="font-label-sm uppercase text-on-surface-variant">
                  {conceptTypeLabel(type)} ({group.length})
                </span>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-2">
                {group.map((concept) => (
                  <div key={concept.id} className="bg-surface-container-low rounded-lg p-3">
                    <p className="text-sm font-medium text-primary">{concept.name}</p>
                    <p className="text-[10px] font-mono text-on-surface-variant mt-0.5">{concept.id}</p>
                    {concept.description && (
                      <p className="text-xs text-on-surface-variant mt-1.5 leading-relaxed">{concept.description}</p>
                    )}
                    {concept.aliases?.length > 0 && (
                      <div className="flex flex-wrap gap-1 mt-2">
                        {concept.aliases.map((alias) => (
                          <span key={alias} className="px-1.5 py-0.5 rounded bg-surface-container-high text-[10px] text-on-surface-variant">
                            {alias}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
      )}

      {/* Edges as readable statements */}
      {sections.includes('edges') && (
      <div>
        <SectionHeader icon={GitBranch} title="Quan hệ" count={edges.length} />
        {edges.length === 0 && (
          <p className="text-sm text-on-surface-variant">Không có quan hệ nào được trích xuất.</p>
        )}
        <div className="space-y-1.5">
          {edges.map((edge, idx) => (
            <div key={idx} className="bg-surface-container-low rounded-lg px-3 py-2.5">
              <div className="flex items-center gap-2 flex-wrap text-sm">
                <span className="font-medium text-primary">{conceptName(edge.source)}</span>
                <span className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-surface-container-high text-[11px] text-on-surface-variant">
                  {relationLabel(edge.relation)} <ArrowRight size={10} />
                </span>
                <span className="font-medium text-primary">{conceptName(edge.target)}</span>
                <span className="ml-auto flex items-center gap-1.5 shrink-0" title={`Trọng số ${edge.weight}`}>
                  <span className="w-16 h-1 rounded-full bg-surface-container-high overflow-hidden">
                    <span className="block h-full bg-primary" style={{ width: `${Math.round(edge.weight * 100)}%` }} />
                  </span>
                  <span className="text-[10px] text-on-surface-variant">{Number(edge.weight).toFixed(2)}</span>
                </span>
              </div>
              {edge.explanation && (
                <p className="text-xs text-on-surface-variant mt-1">{edge.explanation}</p>
              )}
            </div>
          ))}
        </div>
      </div>
      )}

      {/* Rules with payload details */}
      {sections.includes('rules') && (
      <div>
        <SectionHeader icon={Lightbulb} title="Quy tắc" count={rules.length} />
        {rules.length === 0 && (
          <p className="text-sm text-on-surface-variant">Không có quy tắc nào được trích xuất.</p>
        )}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-2">
          {rules.map((rule) => (
            <div key={rule.id} className="bg-surface-container-low rounded-lg p-3">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="px-2 py-0.5 rounded-full bg-primary text-on-primary text-[10px] font-semibold">
                  {ruleTypeLabel(rule.type)}
                </span>
                <span className="text-sm font-medium text-primary">{conceptName(rule.concept_id)}</span>
                <span className="ml-auto text-[10px] text-on-surface-variant" title="Độ ưu tiên">
                  Ưu tiên {Number(rule.priority).toFixed(2)}
                </span>
              </div>
              {rule.payload && Object.keys(rule.payload).length > 0 && (
                <dl className="mt-2 space-y-1">
                  {Object.entries(rule.payload).map(([key, value]) => (
                    <div key={key} className="flex gap-2 text-xs items-baseline">
                      <dt className="text-on-surface-variant shrink-0 min-w-[70px]">
                        {RULE_PAYLOAD_LABELS[key] || key}
                      </dt>
                      <dd className="flex-1 min-w-0"><PayloadValue value={value} /></dd>
                    </div>
                  ))}
                </dl>
              )}
            </div>
          ))}
        </div>
      </div>
      )}
    </div>
  )
}
