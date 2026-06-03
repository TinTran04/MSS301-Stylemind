import { useState } from 'react'
import { Search, Plus, Eye, Edit, Trash2, X } from 'lucide-react'
import StatusBadge from '../../components/admin/StatusBadge'
import Drawer from '../../components/common/Drawer'
import { mockProducts } from '../../data/mockProducts'
import { mockInventory } from '../../data/mockInventory'

export default function ProductManagementPage() {
  const [products, setProducts] = useState(mockProducts.map(p => ({ ...p, isActive: p.isActive !== false })))
  const [search, setSearch] = useState('')
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null)
  const [toast, setToast] = useState(null)
  const [form, setForm] = useState({ name: '', price: '', category: '', material: '' })

  const filtered = products.filter((p) =>
    p.isActive && (p.name.toLowerCase().includes(search.toLowerCase()) || p.sku.toLowerCase().includes(search.toLowerCase()))
  )

  const showToast = (message) => {
    setToast(message)
    setTimeout(() => setToast(null), 2000)
  }

  const openAddDrawer = () => {
    setEditingProduct(null)
    setForm({ name: '', price: '', category: '', material: '' })
    setDrawerOpen(true)
  }

  const openEditDrawer = (e, product) => {
    e.stopPropagation()
    setEditingProduct(product)
    setForm({ name: product.name, price: String(product.price), category: product.category, material: product.material })
    setDrawerOpen(true)
  }

  const handleDelete = (e, productId) => {
    e.stopPropagation()
    setProducts(prev => prev.map(p => p.id === productId ? { ...p, isActive: false } : p))
    showToast('Product deleted')
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    if (editingProduct) {
      setProducts(prev => prev.map(p => p.id === editingProduct.id ? { ...p, name: form.name, price: Number(form.price), category: form.category, material: form.material } : p))
      showToast('Product updated')
    } else {
      const newProduct = {
        ...mockProducts[0],
        id: `prod_${Date.now()}`,
        name: form.name,
        price: Number(form.price),
        category: form.category,
        material: form.material,
        sku: `SKU-${Date.now()}`,
        isActive: true,
      }
      setProducts(prev => [...prev, newProduct])
      showToast('Product added')
    }
    setDrawerOpen(false)
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
          <h1 className="font-headline-md text-primary">Products</h1>
          <p className="text-sm text-on-surface-variant mt-1">{products.filter(p => p.isActive).length} products total</p>
        </div>
        <button onClick={openAddDrawer} className="bg-primary text-on-primary px-4 py-2 rounded-lg text-sm font-medium flex items-center gap-2 hover:opacity-90">
          <Plus size={14} /> Add Product
        </button>
      </div>

      <div className="bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
        <div className="p-4 border-b border-outline-variant/20">
          <div className="relative max-w-sm">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search products..."
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
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Price</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Stock</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">AI Sync</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/5">
              {filtered.map((product) => {
                const inv = mockInventory.find((i) => i.productId === product.id)
                const available = inv ? inv.currentStock - inv.reservedStock : 0
                return (
                  <tr key={product.id} className="hover:bg-surface-container-high/30 cursor-pointer" onClick={() => { setEditingProduct(product); setForm({ name: product.name, price: String(product.price), category: product.category, material: product.material }); setDrawerOpen(true) }}>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <img src={product.images[0]} alt={product.name} className="w-10 h-10 rounded-lg object-cover" />
                        <div>
                          <p className="text-sm font-medium text-primary">{product.name}</p>
                          <p className="text-xs text-on-surface-variant">{product.category}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-xs text-on-surface-variant font-mono">{product.sku}</td>
                    <td className="px-4 py-3 text-sm text-primary">${product.price}</td>
                    <td className="px-4 py-3"><StatusBadge status={inv?.status || 'in_stock'} /></td>
                    <td className="px-4 py-3"><StatusBadge status="synced" /></td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1">
                        <button className="p-1.5 rounded hover:bg-surface-container-high"><Eye size={14} className="text-on-surface-variant" /></button>
                        <button onClick={(e) => openEditDrawer(e, product)} className="p-1.5 rounded hover:bg-surface-container-high"><Edit size={14} className="text-on-surface-variant" /></button>
                        <button onClick={(e) => handleDelete(e, product.id)} className="p-1.5 rounded hover:bg-surface-container-high"><Trash2 size={14} className="text-error" /></button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>

      <Drawer isOpen={drawerOpen} onClose={() => setDrawerOpen(false)} title={editingProduct ? 'Edit Product' : 'Add Product'}>
        <form onSubmit={handleSubmit} className="space-y-6">
          {editingProduct && (
            <div className="flex items-center gap-3">
              <img src={editingProduct.images[0]} alt={editingProduct.name} className="w-16 h-20 rounded-lg object-cover" />
              <div>
                <h3 className="font-title-lg text-primary">{editingProduct.name}</h3>
                <p className="text-xs text-on-surface-variant">{editingProduct.sku}</p>
              </div>
            </div>
          )}
          <div>
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Product Name</label>
            <input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
          </div>
          <div>
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Price</label>
            <input required type="number" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
          </div>
          <div>
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Category</label>
            <input required value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
          </div>
          <div>
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Material</label>
            <input required value={form.material} onChange={(e) => setForm({ ...form, material: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="submit" className="flex-1 bg-primary text-on-primary px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90">
              {editingProduct ? 'Update Product' : 'Add Product'}
            </button>
            <button type="button" onClick={() => setDrawerOpen(false)} className="px-4 py-2 rounded-lg text-sm font-medium bg-surface-container text-on-surface hover:bg-surface-container-high">
              Cancel
            </button>
          </div>
        </form>
      </Drawer>
    </div>
  )
}
