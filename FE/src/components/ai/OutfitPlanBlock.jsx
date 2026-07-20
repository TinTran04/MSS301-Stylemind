import { useState } from 'react'
import { ShoppingBag, Lightbulb, Loader2 } from 'lucide-react'
import ProductBlock from './ProductBlock'
import useCartStore from '../../features/cart/cart.store'
import { getProductById } from '../../features/products/product.api'
import { toDisplayProduct } from '../../features/ai-stylist/aiStylist.utils'

// Renders the structured outfit plan attached to an AI message
// (metadata.outfit_plan from the Python chat service): one card per day with
// the recommended items, styling tip and an add-the-whole-set shortcut.
export default function OutfitPlanBlock({ plan, messageId }) {
  const addItem = useCartStore((s) => s.addItem)
  const [addingDay, setAddingDay] = useState(null)
  const [addedDays, setAddedDays] = useState([])

  const outfits = plan?.outfits || []
  if (outfits.length === 0) return null

  const handleAddFullOutfit = async (dayPlan) => {
    setAddingDay(dayPlan.day)
    try {
      for (const item of dayPlan.items) {
        if (!item.product_id) continue
        const fullProduct = await getProductById(item.product_id)
        if (fullProduct) {
          await addItem(fullProduct, 1, undefined, undefined, {
            isAiRecommended: true,
            sourceBundleId: `${messageId}-day-${dayPlan.day}`,
          })
        }
      }
      setAddedDays((prev) => [...prev, dayPlan.day])
    } finally {
      setAddingDay(null)
    }
  }

  return (
    <div className="space-y-4">
      {outfits.map((dayPlan) => (
        <div key={dayPlan.day} className="glass-panel rounded-2xl p-4">
          {outfits.length > 1 && (
            <div className="flex items-center gap-2 mb-1">
              <span className="px-2.5 py-0.5 rounded-full bg-primary text-on-primary text-xs font-semibold">
                Ng├áy {dayPlan.day}
              </span>
            </div>
          )}
          {dayPlan.context && (
            <p className="text-xs text-on-surface-variant mb-3">{dayPlan.context}</p>
          )}

          <div className="grid grid-cols-2 gap-3 max-w-[500px]">
            {dayPlan.items.map((item, idx) => (
              <ProductBlock
                key={item.product_id || idx}
                product={toDisplayProduct(item)}
                bundleId={`${messageId}-day-${dayPlan.day}`}
              />
            ))}
          </div>

          {dayPlan.styling_tip && (
            <div className="mt-3 flex items-start gap-2 rounded-xl bg-surface-container-low px-3 py-2.5">
              <Lightbulb size={14} className="text-tertiary shrink-0 mt-0.5" />
              <p className="text-xs text-on-surface-variant leading-relaxed">{dayPlan.styling_tip}</p>
            </div>
          )}

          {dayPlan.items.some((item) => item.product_id) && (
            <div className="mt-3">
              <button
                onClick={() => handleAddFullOutfit(dayPlan)}
                disabled={addingDay === dayPlan.day || addedDays.includes(dayPlan.day)}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-primary text-on-primary rounded-lg text-xs font-medium hover:opacity-90 transition-opacity disabled:opacity-50"
              >
                {addingDay === dayPlan.day
                  ? <Loader2 size={12} className="animate-spin" />
                  : <ShoppingBag size={12} />}
                {addingDay === dayPlan.day
                  ? '─Éang th├¬m...'
                  : addedDays.includes(dayPlan.day) ? '─É├ú th├¬m trß╗ìn set' : 'Th├¬m trß╗ìn set'}
              </button>
            </div>
          )}
        </div>
      ))}
    </div>
  )
}