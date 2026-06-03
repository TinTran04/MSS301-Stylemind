import { Minus, Plus, Trash2 } from 'lucide-react'
import useCartStore from '../../features/cart/cart.store'

export default function CartItem({ item }) {
  const updateQuantity = useCartStore((s) => s.updateQuantity)
  const removeItem = useCartStore((s) => s.removeItem)

  return (
    <div className="flex gap-4 py-4 border-b border-outline-variant/10">
      <img
        src={item.images?.[0]}
        alt={item.name}
        className="w-20 h-24 object-cover rounded-lg"
      />
      <div className="flex-1 min-w-0">
        <h4 className="text-sm font-medium text-primary truncate">{item.name}</h4>
        <p className="text-xs text-on-surface-variant mt-0.5">
          Size: {item.size} &middot; {item.color}
        </p>
        <p className="text-sm font-semibold text-primary mt-2">${item.price}</p>
      </div>
      <div className="flex flex-col items-end justify-between">
        <button
          onClick={() => removeItem(item.cartItemId)}
          className="p-1 text-on-surface-variant hover:text-error transition-colors"
        >
          <Trash2 size={14} />
        </button>
        <div className="flex items-center gap-2 border border-outline-variant/20 rounded-lg">
          <button
            onClick={() => updateQuantity(item.cartItemId, item.quantity - 1)}
            className="p-1.5 hover:bg-surface-container-high rounded-l-lg transition-colors"
          >
            <Minus size={12} />
          </button>
          <span className="text-xs font-medium w-5 text-center">{item.quantity}</span>
          <button
            onClick={() => updateQuantity(item.cartItemId, item.quantity + 1)}
            className="p-1.5 hover:bg-surface-container-high rounded-r-lg transition-colors"
          >
            <Plus size={12} />
          </button>
        </div>
      </div>
    </div>
  )
}
