import { Sparkles } from 'lucide-react'

const suggestions = [
  'Gợi ý cho tôi một outfit đi ăn tối công việc',
  'Mùa hè này đang thịnh hành gì?',
  'Tôi cần một look cuối tuần thoải mái',
  'Giúp tôi xây dựng tủ đồ capsule',
  'Gợi ý các lựa chọn thời trang bền vững',
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
