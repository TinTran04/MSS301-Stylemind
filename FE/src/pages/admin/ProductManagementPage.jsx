import { useState, useEffect } from 'react'
import { Search, Plus, Eye, Edit, Trash2, Loader2, RefreshCw } from 'lucide-react'
import StatusBadge from '../../components/admin/StatusBadge'
import Drawer from '../../components/common/Drawer'
import { getAdminProducts, createProduct, updateProduct, deleteProduct } from '../../features/products/admin-product.api'
import { getCategories } from '../../features/products/product.api'

export default function ProductManagementPage() {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null)
  const [toast, setToast] = useState(null)
  const [form, setForm] = useState({ name: '', price: '', categoryId: '', material: '', sku: '' })

  const fetchProducts = async () => {
    setLoading(true)
    try {
      const data = await getAdminProducts()
      const cats = await getCategories()
      setProducts(data.content || data || [])
      setCategories(cats || [])
    } catch (err) {
      showToast('Error loading products: ' + err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchProducts()
  }, [])

  const filtered = products.filter((p) =>
    p.active !== false && (p.name?.toLowerCase().includes(search.toLowerCase()) || p.sku?.toLowerCase().includes(search.toLowerCase()))
  )

  const showToast = (message) => {
    setToast(message)
    setTimeout(() => setToast(null), 3000)
  }

  const openAddDrawer = () => {
    setEditingProduct(null)
    setForm({ name: '', price: '', categoryId: categories[0]?.id || '', material: '', sku: '' })
    setDrawerOpen(true)
  }

  const openEditDrawer = (e, product) => {
    e.stopPropagation()
    setEditingProduct(product)
    setForm({ 
      name: product.name, 
      price: String(product.price), 
      categoryId: product.category?.id || categories[0]?.id || '', 
      material: product.material || '',
      sku: product.sku || ''
    })
    setDrawerOpen(true)
  }

  const handleDelete = async (e, productId) => {
    e.stopPropagation()
    if (!window.confirm('Are you sure you want to delete this product?')) return
    try {
      await deleteProduct(productId)
      setProducts(prev => prev.filter(p => p.id !== productId))
      showToast('Product deleted')
    } catch (err) {
      showToast('Delete failed: ' + err.message)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setActionLoading(true)
    try {
      const payload = {
        name: form.name,
        price: Number(form.price),
        categoryId: Number(form.categoryId),
        material: form.material,
        sku: form.sku || `SKU-${Date.now()}`,
        active: true,
      }
      if (editingProduct) {
        await updateProduct(editingProduct.id, payload)
        showToast('Product updated')
      } else {
        await createProduct(payload)
        showToast('Product added')
      }
      setDrawerOpen(false)
      fetchProducts()
    } catch (err) {
      showToast('Operation failed: ' + err.message)
    } finally {
      setActionLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      {toast && (
        <div className="fixed top-4 right-4 z-50 bg-surface-container-highest text-on-surface px-4 py-2 rounded-lg shadow-lg text-sm font-medium border border-outline/20">
          {toast}
        </div>
      )}

      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-headline-md text-primary">Products</h1>
          <p className="text-sm text-on-surface-variant mt-1">{filtered.length} products found</p>
        </div>
        <div className="flex gap-2">
          <button onClick={fetchProducts} disabled={loading} className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-high transition-colors disabled:opacity-40">
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} /> Refresh
          </button>
          <button onClick={openAddDrawer} className="bg-primary text-on-primary px-4 py-2 rounded-lg text-sm font-medium flex items-center gap-2 hover:opacity-90">
            <Plus size={14} /> Add Product
          </button>
        </div>
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
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/5">
              {loading && products.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-center py-12 text-sm text-on-surface-variant">Loading products...</td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-center py-12 text-sm text-on-surface-variant">No products found.</td>
                </tr>
              ) : filtered.map((product) => {
                const totalStock = product.variants?.reduce((sum, v) => sum + (v.stockQuantity || 0), 0) || 0;
                return (
                  <tr key={product.id} className="hover:bg-surface-container-high/30 cursor-pointer" onClick={(e) => openEditDrawer(e, product)}>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <img src={product.images?.[0]?.url || 'https://via.placeholder.com/40'} alt={product.name} className="w-10 h-10 rounded-lg object-cover" />
                        <div>
                          <p className="text-sm font-medium text-primary">{product.name}</p>
                          <p className="text-xs text-on-surface-variant">{product.category?.name || 'Uncategorized'}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-xs text-on-surface-variant font-mono">{product.sku}</td>
                    <td className="px-4 py-3 text-sm text-primary">${product.price}</td>
                    <td className="px-4 py-3"><span className="text-xs text-on-surface-variant">{totalStock} units</span></td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1">
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
              <img src={editingProduct.images?.[0]?.url || 'https://via.placeholder.com/40'} alt={editingProduct.name} className="w-16 h-20 rounded-lg object-cover" />
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
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">SKU</label>
            <input required value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
          </div>
          <div>
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Price</label>
            <input required type="number" step="0.01" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
          </div>
          <div>
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Category</label>
            <select required value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none">
              <option value="" disabled>Select a category</option>
              {categories.map(c => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Material</label>
            <input value={form.material} onChange={(e) => setForm({ ...form, material: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
          </div>
          <div className="flex gap-3 pt-2">
            <button type="submit" disabled={actionLoading} className="flex-1 bg-primary text-on-primary px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-50">
              {actionLoading ? <Loader2 size={14} className="animate-spin mx-auto" /> : (editingProduct ? 'Update Product' : 'Add Product')}
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
