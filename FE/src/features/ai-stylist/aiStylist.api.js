import { mockProducts } from '../../data/mockProducts'
import { mockInventory } from '../../data/mockInventory'

function getAvailableProducts() {
  return mockProducts.filter((p) => {
    const inv = mockInventory.find((i) => i.productId === p.id)
    return inv && inv.currentStock - inv.reservedStock > 0
  })
}

function getProductStock(productId) {
  const inv = mockInventory.find((i) => i.productId === productId)
  if (!inv) return { available: 0, status: 'out_of_stock' }
  const available = inv.currentStock - inv.reservedStock
  if (available <= 0) return { available: 0, status: 'out_of_stock' }
  if (available <= 5) return { available, status: 'low_stock' }
  return { available, status: 'in_stock' }
}

const contextualResponses = {
  dinner: {
    message: "For a dinner event, I recommend elegant pieces that transition from golden hour to evening. Here are curated selections that match your minimalist aesthetic while making a sophisticated statement.",
    tags: ['Dresses', 'Outerwear'],
  },
  casual: {
    message: "For a relaxed everyday look, these pieces offer comfort without sacrificing style. They layer beautifully and work across seasons.",
    tags: ['Tops', 'Bottoms'],
  },
  work: {
    message: "Professional pieces that command attention. These selections balance authority with your personal style DNA.",
    tags: ['Outerwear', 'Bottoms'],
  },
  summer: {
    message: "Lightweight, breathable pieces perfect for warm weather. Natural fabrics and relaxed silhouettes for effortless summer style.",
    tags: ['Dresses', 'Footwear'],
  },
  default: {
    message: "Based on your style profile, I've curated these pieces that complement your aesthetic. Each recommendation considers your body type, preferred silhouettes, and color palette.",
    tags: null,
  },
}

export async function sendStylingPrompt(payload) {
  const available = getAvailableProducts()
  const prompt = (payload?.prompt || '').toLowerCase()

  let category = 'default'
  if (prompt.includes('dinner') || prompt.includes('evening') || prompt.includes('formal')) category = 'dinner'
  else if (prompt.includes('casual') || prompt.includes('everyday') || prompt.includes('relaxed')) category = 'casual'
  else if (prompt.includes('work') || prompt.includes('office') || prompt.includes('professional')) category = 'work'
  else if (prompt.includes('summer') || prompt.includes('beach') || prompt.includes('vacation')) category = 'summer'

  const ctx = contextualResponses[category]

  let recommended
  if (ctx.tags) {
    const tagged = available.filter((p) => ctx.tags.includes(p.category))
    recommended = tagged.length >= 2
      ? tagged.sort((a, b) => b.aiMatchScore - a.aiMatchScore).slice(0, 2)
      : available.sort((a, b) => b.aiMatchScore - a.aiMatchScore).slice(0, 2)
  } else {
    recommended = available.sort((a, b) => b.aiMatchScore - a.aiMatchScore).slice(0, 2)
  }

  return {
    message: ctx.message,
    products: recommended.map((p) => ({
      ...p,
      stockInfo: getProductStock(p.id),
    })),
    reasoning: {
      factors: [
        'Style DNA match',
        'Color harmony',
        'Silhouette compatibility',
        'Seasonal relevance',
        'Inventory availability',
      ],
      confidence: 88 + Math.floor(Math.random() * 10),
    },
  }
}

export async function getConsultationHistory() {
  return [
    { id: '1', date: '2026-06-01', prompt: 'Find me a dinner outfit', response: 'I found 3 perfect pieces...' },
    { id: '2', date: '2026-05-28', prompt: 'Summer wardrobe refresh', response: 'Let me curate a collection...' },
  ]
}
