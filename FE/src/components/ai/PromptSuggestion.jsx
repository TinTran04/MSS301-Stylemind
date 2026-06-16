import { Sparkles } from 'lucide-react'

const suggestions = [
  "Find me an outfit for a business dinner",
  "What's trending for summer?",
  "I need a casual weekend look",
  "Help me build a capsule wardrobe",
  "Recommend sustainable fashion options",
]

export default function PromptSuggestion({ onSelect }) {
  return (
    <div className="flex flex-wrap gap-2">
      {suggestions.map((suggestion, idx) => (
        <button
          key={idx}
          onClick={() => onSelect(suggestion)}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-surface-container text-xs text-on-surface-variant hover:bg-surface-container-high hover:text-primary transition-colors border border-outline-variant/10"
        >
          <Sparkles size={10} className="text-tertiary" />
          {suggestion}
        </button>
      ))}
    </div>
  )
}
