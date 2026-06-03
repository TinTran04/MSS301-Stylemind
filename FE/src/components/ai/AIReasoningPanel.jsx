import { Brain, Check } from 'lucide-react'

export default function AIReasoningPanel({ reasoning }) {
  if (!reasoning) return null

  return (
    <div className="bg-surface-container-low rounded-xl p-4 border border-outline-variant/10">
      <div className="flex items-center gap-2 mb-3">
        <Brain size={16} className="text-tertiary" />
        <span className="font-label-sm uppercase text-on-surface-variant">AI Reasoning</span>
        <span className="ml-auto text-xs text-on-surface-variant">
          Confidence: {reasoning.confidence}%
        </span>
      </div>
      <div className="space-y-2">
        {reasoning.factors.map((factor, idx) => (
          <div key={idx} className="flex items-center gap-2 text-xs text-on-surface-variant">
            <Check size={12} className="text-green-status" />
            {factor}
          </div>
        ))}
      </div>
    </div>
  )
}
