import { useState, useEffect } from 'react'
import { Network, ZoomIn, ZoomOut, RefreshCw, GitBranch, Plus } from 'lucide-react'
import { getKnowledgeGraph } from '../../features/analytics/analytics.api'

const nodeColors = {
  style: '#E6E6FA',
  material: '#cca730',
  occasion: '#e0e0dd',
  fit: '#c8c6c5',
  color: '#ffe088',
  customer: '#000',
  silhouette: '#dbdad9',
}

export default function KnowledgeGraphPage() {
  const [graph, setGraph] = useState(null)
  const [selectedNode, setSelectedNode] = useState(null)

  useEffect(() => {
    getKnowledgeGraph().then(setGraph)
  }, [])

  if (!graph) return <div className="p-8 text-on-surface-variant">Loading...</div>

  // Simple node positions for visualization
  const nodePositions = graph.nodes.reduce((acc, node, idx) => {
    const angle = (idx / graph.nodes.length) * 2 * Math.PI
    const radius = 180
    acc[node.id] = { x: 300 + radius * Math.cos(angle), y: 250 + radius * Math.sin(angle) }
    return acc
  }, {})

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-headline-md text-primary">Knowledge Graph</h1>
          <p className="text-sm text-on-surface-variant mt-1">Manage AI knowledge relationships</p>
        </div>
        <div className="flex gap-2">
          <button className="bg-surface-container p-2 rounded-lg hover:bg-surface-container-high"><ZoomIn size={16} /></button>
          <button className="bg-surface-container p-2 rounded-lg hover:bg-surface-container-high"><ZoomOut size={16} /></button>
          <button className="bg-surface-container p-2 rounded-lg hover:bg-surface-container-high"><RefreshCw size={16} /></button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Graph Visualization */}
        <div className="lg:col-span-8 bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
          <svg viewBox="0 0 600 500" className="w-full h-[500px]">
            {/* Relationships */}
            {graph.relationships.map((rel, idx) => {
              const from = nodePositions[rel.from]
              const to = nodePositions[rel.to]
              if (!from || !to) return null
              return (
                <line key={idx} x1={from.x} y1={from.y} x2={to.x} y2={to.y} stroke="#c4c7c7" strokeWidth={rel.strength * 3} strokeOpacity={0.6} />
              )
            })}
            {/* Nodes */}
            {graph.nodes.map((node) => {
              const pos = nodePositions[node.id]
              const isSelected = selectedNode?.id === node.id
              return (
                <g key={node.id} className="cursor-pointer" onClick={() => setSelectedNode(node)}>
                  <circle cx={pos.x} cy={pos.y} r={isSelected ? 28 : 22} fill={nodeColors[node.type] || '#e0e0dd'} stroke={isSelected ? '#000' : 'none'} strokeWidth={2} />
                  <text x={pos.x} y={pos.y + 4} textAnchor="middle" fontSize={10} fontWeight={500} fill="#1c1b1b">{node.label.slice(0, 6)}</text>
                </g>
              )
            })}
          </svg>
        </div>

        {/* Detail Panel */}
        <div className="lg:col-span-4 space-y-4">
          <div className="bg-surface-container-lowest rounded-xl p-5 ambient-shadow">
            {selectedNode ? (
              <div className="space-y-4">
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded-full" style={{ backgroundColor: nodeColors[selectedNode.type] }} />
                  <h3 className="font-title-lg text-primary">{selectedNode.label}</h3>
                </div>
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between"><span className="text-on-surface-variant">Type</span><span className="text-primary capitalize">{selectedNode.type}</span></div>
                  <div className="flex justify-between"><span className="text-on-surface-variant">Confidence</span><span className="text-primary">{(selectedNode.confidence * 100).toFixed(0)}%</span></div>
                </div>
                <div>
                  <h4 className="font-label-sm uppercase text-on-surface-variant mb-2">Connected To</h4>
                  <div className="space-y-1">
                    {graph.relationships.filter((r) => r.from === selectedNode.id || r.to === selectedNode.id).map((rel, idx) => {
                      const otherId = rel.from === selectedNode.id ? rel.to : rel.from
                      const other = graph.nodes.find((n) => n.id === otherId)
                      return (
                        <div key={idx} className="flex items-center justify-between p-2 bg-surface-container-low rounded text-xs">
                          <span className="text-primary">{other?.label}</span>
                          <span className="text-on-surface-variant">{rel.type}</span>
                        </div>
                      )
                    })}
                  </div>
                </div>
              </div>
            ) : (
              <div className="text-center py-8 text-on-surface-variant text-sm">Select a node to view details</div>
            )}
          </div>

          {/* Rules Editor */}
          <div className="bg-surface-container-lowest rounded-xl p-5 ambient-shadow">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-title-lg text-primary">Rules</h3>
              <button className="p-1.5 rounded hover:bg-surface-container-high"><Plus size={14} className="text-on-surface-variant" /></button>
            </div>
            <div className="space-y-3">
              <div className="bg-surface-container-low rounded-lg p-3">
                <div className="flex items-center gap-2 text-xs text-on-surface-variant mb-1"><GitBranch size={12} /> IF</div>
                <p className="text-sm text-primary">Style = Minimalist</p>
              </div>
              <div className="bg-surface-container-low rounded-lg p-3">
                <div className="flex items-center gap-2 text-xs text-on-surface-variant mb-1"><GitBranch size={12} /> THEN</div>
                <p className="text-sm text-primary">Recommend Neutral Tones</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
