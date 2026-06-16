import { Sparkles } from 'lucide-react'
import Badge from '../common/Badge'

export default function OutfitCard({ outfit, products }) {
  const outfitProducts = outfit.products.map((id) => products.find((p) => p.id === id)).filter(Boolean)

  return (
    <div className="bg-ai-lavender/20 rounded-2xl p-5 border border-ai-lavender/30">
      <div className="flex items-center gap-2 mb-3">
        <Badge variant="ai">
          <Sparkles size={12} />
          {outfit.aiScore}% Match
        </Badge>
        <span className="font-label-sm text-on-surface-variant uppercase">{outfit.name}</span>
      </div>
      <div className="grid grid-cols-3 gap-2 mb-3">
        {outfitProducts.map((product) => (
          <img
            key={product.id}
            src={product.images[0]}
            alt={product.name}
            className="w-full aspect-square object-cover rounded-lg"
          />
        ))}
      </div>
      <p className="text-xs text-on-surface-variant">{outfit.description}</p>
    </div>
  )
}
