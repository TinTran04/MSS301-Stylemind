import { useState } from 'react'
import { Search, RefreshCw, AlertTriangle, TrendingUp } from 'lucide-react'
import StatusBadge from '../../components/admin/StatusBadge'
import { mockProducts } from '../../data/mockProducts'
import { mockInventory } from '../../data/mockInventory'

export default function InventoryManagementPage() {
  const [search, setSearch] = useState('')
  const [inventory, setInventory] = useState(mockInventory)
  const [toast, setToast] = useState(null)

  const showToast = (message) => {
    setToast(message)
    setTimeout(() => setToast(null), 2000)
  }

  const inventoryData = inventory.map((inv) => {
    const product = mockProducts.find((p) => p.id === inv.productId)
    return {
      ...inv,
      name: product?.name || 'Unknown',
      category: product?.category || '',
      image: product?.images?.[0] || '',
      available: inv.currentStock - inv.reservedStock,
    }
  }).filter((item) => item.name.toLowerCase().includes(search.toLowerCase()))

  const lowStockCount = inventoryData.filter((i) => i.status === 'low_stock').length
  const outOfStockCount = inventoryData.filter((i) => i.status === 'out_of_stock').length

  const getStatus = (stock) => {
    if (stock <= 0) return 'out_of_stock'
    if (stock <= 10) return 'low_stock'
    return 'in_stock'
  }

  const replenish = (e, productId) => {
    e.stopPropagation()
    setInventory(prev => prev.map(inv => {
      if (inv.productId === productId) {
        const newStock = inv.currentStock + 20
        return { ...inv, currentStock: newStock, status: getStatus(newStock) }
      }
      return inv
    }))
    showToast('Stock replenished by 20')
  }

  const markDamaged = (e, productId) => {
    e.stopPropagation()
    setInventory(prev => prev.map(inv => {
      if (inv.productId === productId) {
        const newStock = Math.max(0, inv.currentStock - 5)
        return { ...inv, currentStock: newStock, status: getStatus(newStock) }
      }
      return inv
    }))
    showToast('5 units marked as damaged')
  }

  return (
    <div className="space-y-6">
      {toast && (
        <div className="fixed top-4 right-4 z-50 bg-green-status text-white px-4 py-2 rounded-lg shadow-lg text-sm font-medium animate-pulse">
          {toast}
        </div>
      )}

      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-headline-md text-primary">Inventory</h1>
          <p className="text-sm text-on-surface-variant mt-1">Stock management and AI demand insights</p>
        </div>
        <button className="bg-primary text-on-primary px-4 py-2 rounded-lg text-sm font-medium flex items-center gap-2 hover:opacity-90">
          <RefreshCw size={14} /> Sync Inventory
        </button>
      </div>

      <div className="grid grid-cols-3 gap-4">
        <div className="bg-surface-container-lowest rounded-xl p-4 ambient-shadow">
          <p className="text-xs text-on-surface-variant uppercase">Total SKUs</p>
          <p className="text-2xl font-semibold text-primary mt-1">{inventory.length}</p>
        </div>
        <div className="bg-surface-container-lowest rounded-xl p-4 ambient-shadow">
          <div className="flex items-center gap-1.5"><AlertTriangle size={14} className="text-tertiary" /><p className="text-xs text-tertiary uppercase">Low Stock</p></div>
          <p className="text-2xl font-semibold text-tertiary mt-1">{lowStockCount}</p>
        </div>
        <div className="bg-surface-container-lowest rounded-xl p-4 ambient-shadow">
          <div className="flex items-center gap-1.5"><AlertTriangle size={14} className="text-error" /><p className="text-xs text-error uppercase">Out of Stock</p></div>
          <p className="text-2xl font-semibold text-error mt-1">{outOfStockCount}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
          <div className="p-4 border-b border-outline-variant/20">
            <div className="relative max-w-sm">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search inventory..."
                className="w-full pl-9 pr-4 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none"
              />
            </div>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-surface-container-low/50">
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Product</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">SKU</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Current</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Reserved</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Available</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Status</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/5">
                {inventoryData.map((item) => (
                  <tr key={item.productId} className="hover:bg-surface-container-high/30">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <img src={item.image} alt={item.name} className="w-8 h-8 rounded-lg object-cover" />
                        <span className="text-sm font-medium text-primary">{item.name}</span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-xs text-on-surface-variant font-mono">{item.sku}</td>
                    <td className="px-4 py-3 text-sm text-on-surface">{item.currentStock}</td>
                    <td className="px-4 py-3 text-sm text-on-surface-variant">{item.reservedStock}</td>
                    <td className="px-4 py-3 text-sm font-medium text-primary">{item.available}</td>
                    <td className="px-4 py-3"><StatusBadge status={item.status} /></td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1">
                        <button onClick={(e) => replenish(e, item.productId)} className="text-xs text-primary font-medium hover:underline px-2 py-1 rounded bg-primary/10">Replenish</button>
                        <button onClick={(e) => markDamaged(e, item.productId)} className="text-xs text-error font-medium hover:underline px-2 py-1 rounded bg-error/10">Damaged</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="space-y-4">
          <div className="bg-surface-container-lowest rounded-xl p-5 ambient-shadow">
            <h3 className="font-title-lg text-primary mb-4">Quick Replenish</h3>
            <div className="space-y-3">
              {inventoryData.filter((i) => i.status !== 'in_stock').map((item) => (
                <div key={item.productId} className="flex items-center justify-between p-2 bg-surface-container-low rounded-lg">
                  <span className="text-xs text-on-surface truncate">{item.name}</span>
                  <button onClick={(e) => replenish(e, item.productId)} className="text-xs text-primary font-medium hover:underline">Reorder</button>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-ai-lavender/20 rounded-xl p-5 border border-ai-lavender/30">
            <div className="flex items-center gap-2 mb-3">
              <TrendingUp size={16} className="text-tertiary" />
              <span className="font-label-sm uppercase text-on-surface-variant">AI Demand Prediction</span>
            </div>
            <p className="text-sm text-on-surface-variant leading-relaxed">
              AI models predict a 35% surge in demand for Silk Midi Dresses next week based on social media trends and seasonal patterns. Consider increasing stock levels.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
