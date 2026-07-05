import { useState, useEffect, useCallback } from 'react'
import { Search, Plus, Edit, Trash2, Loader2, RefreshCw, ChevronLeft, ChevronRight, Tag, ImagePlus, X } from 'lucide-react'
import Drawer from '../../components/common/Drawer'
import ProductImage from '../../components/customer/ProductImage'
import {
  getAdminProducts,
  createProduct,
  updateProduct,
  updateProductStatus,
  deleteProduct,
  addVariant,
  updateVariant,
  deleteVariant,
  uploadImage,
  deleteImage,
} from '../../features/products/admin-product.api'
import { createCategory, updateCategory, deleteCategory, getAdminCategories } from '../../features/products/admin-category.api'

const STATUS_STYLES = {
  ACTIVE: 'bg-tertiary-fixed/30 text-tertiary',
  INACTIVE: 'bg-surface-container-high text-on-surface-variant',
  DISCONTINUED: 'bg-error/15 text-error',
}

const EMPTY_PRODUCT_FORM = {
  name: '',
  description: '',
  basePrice: '',
  categoryId: '',
  aestheticStyle: '',
  targetDemographic: '',
  seasonalProperty: '',
  status: 'ACTIVE',
}

const EMPTY_VARIANT_FORM = { sku: '', size: '', color: '', material: '', priceOverride: '' }
const EMPTY_CATEGORY_FORM = { name: '', slug: '', parentId: '' }

const PAGE_SIZE = 20

export default function ProductManagementPage() {
  const [products, setProducts] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [currentPage, setCurrentPage] = useState(0)
  const [categories, setCategories] = useState([])
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionLoading, setActionLoading] = useState(false)
  const [toast, setToast] = useState(null)

  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editingProduct, setEditingProduct] = useState(null)
  const [form, setForm] = useState(EMPTY_PRODUCT_FORM)

  const [variantForm, setVariantForm] = useState(EMPTY_VARIANT_FORM)
  const [editingVariantId, setEditingVariantId] = useState(null)

  const [imageFile, setImageFile] = useState(null)
  const [imageIsPrimary, setImageIsPrimary] = useState(false)
  const [imagePreviewUrl, setImagePreviewUrl] = useState('')
  const [imageError, setImageError] = useState('')
  const [imageUploading, setImageUploading] = useState(false)

  const [categoryDrawerOpen, setCategoryDrawerOpen] = useState(false)
  const [categoryForm, setCategoryForm] = useState(EMPTY_CATEGORY_FORM)
  const [editingCategoryId, setEditingCategoryId] = useState(null)

  const showToast = (message) => {
    setToast(message)
    setTimeout(() => setToast(null), 3000)
  }

  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(search), 400)
    return () => clearTimeout(t)
  }, [search])

  useEffect(() => {
    return () => {
      if (imagePreviewUrl) URL.revokeObjectURL(imagePreviewUrl)
    }
  }, [imagePreviewUrl])

  const fetchCategories = useCallback(async () => {
    try {
      setCategories(await getAdminCategories())
    } catch {
      setCategories([])
    }
  }, [])

  const fetchProducts = useCallback(
    async (page = 0) => {
      setLoading(true)
      setError('')
      try {
        const data = await getAdminProducts({
          page,
          size: PAGE_SIZE,
          search: debouncedSearch,
          category: categoryFilter,
          status: statusFilter,
        })
        setProducts(data.content || [])
        setTotalElements(data.totalElements || 0)
        setTotalPages(data.totalPages || 1)
        setCurrentPage(data.page || 0)
      } catch (err) {
        setError(err.message || 'Không thể tải danh sách sản phẩm.')
      } finally {
        setLoading(false)
      }
    },
    [debouncedSearch, categoryFilter, statusFilter],
  )

  useEffect(() => {
    fetchCategories()
  }, [fetchCategories])

  useEffect(() => {
    fetchProducts(0)
  }, [fetchProducts])

  const categoryName = (categoryId) => categories.find((c) => String(c.id) === String(categoryId))?.name || 'Chưa phân loại'

  const openAddDrawer = () => {
    setEditingProduct(null)
    setForm({ ...EMPTY_PRODUCT_FORM, categoryId: categories[0]?.id ? String(categories[0].id) : '' })
    setVariantForm(EMPTY_VARIANT_FORM)
    setEditingVariantId(null)
    setImageFile(null)
    setImagePreviewUrl('')
    setImageError('')
    setImageIsPrimary(true)
    setDrawerOpen(true)
  }

  const openEditDrawer = (product) => {
    setEditingProduct(product)
    setForm({
      name: product.name,
      description: product.description || '',
      basePrice: String(product.basePrice ?? ''),
      categoryId: product.categoryId ? String(product.categoryId) : '',
      aestheticStyle: product.aestheticStyle || '',
      targetDemographic: product.targetDemographic || '',
      seasonalProperty: product.seasonalProperty || '',
      status: product.status || 'ACTIVE',
    })
    setVariantForm(EMPTY_VARIANT_FORM)
    setEditingVariantId(null)
    setImageFile(null)
    setImagePreviewUrl('')
    setImageError('')
    setImageIsPrimary(false)
    setDrawerOpen(true)
  }

  const handleDelete = async (productId) => {
    if (!window.confirm('Bạn có chắc muốn xóa sản phẩm này?')) return
    try {
      await deleteProduct(productId)
      showToast('Đã xóa sản phẩm')
      fetchProducts(currentPage)
    } catch (err) {
      showToast('Xóa thất bại: ' + err.message)
    }
  }

  const handleToggleStatus = async (product) => {
    const nextStatus = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      await updateProductStatus(product.id, nextStatus)
      showToast('Đã cập nhật trạng thái')
      fetchProducts(currentPage)
    } catch (err) {
      showToast('Cập nhật trạng thái thất bại: ' + err.message)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setActionLoading(true)
    try {
      const payload = {
        name: form.name,
        description: form.description,
        basePrice: Number(form.basePrice),
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        aestheticStyle: form.aestheticStyle || null,
        targetDemographic: form.targetDemographic || null,
        seasonalProperty: form.seasonalProperty || null,
        status: form.status,
      }
      if (editingProduct) {
        const updated = await updateProduct(editingProduct.id, payload)
        setEditingProduct((current) => ({
          ...updated,
          images: updated.images || current.images || [],
          variants: updated.variants || current.variants || [],
        }))
        showToast('Đã cập nhật sản phẩm')
      } else {
        const created = await createProduct(payload)
        let createdImages = created.images || []
        if (imageFile) {
          setImageUploading(true)
          try {
            const image = await uploadImage(created.id, imageFile, true)
            createdImages = [image]
            setImageFile(null)
            setImagePreviewUrl('')
            setImageIsPrimary(false)
            showToast('Đã tạo sản phẩm và tải ảnh lên')
          } catch (imageUploadError) {
            setImageError(imageUploadError.message || 'Không thể tải ảnh lên.')
            showToast('Đã tạo sản phẩm, nhưng tải ảnh thất bại')
          } finally {
            setImageUploading(false)
          }
        } else {
          showToast('Đã tạo sản phẩm. Bạn có thể thêm ảnh và biến thể ngay bây giờ.')
        }
        setEditingProduct({ ...created, images: createdImages, variants: created.variants || [] })
      }
      fetchProducts(currentPage)
    } catch (err) {
      showToast('Thao tác thất bại: ' + err.message)
    } finally {
      setActionLoading(false)
    }
  }

  // ─── Variants ──────────────────────────────────────────────────────────────
  const startEditVariant = (variant) => {
    setEditingVariantId(variant.id)
    setVariantForm({
      sku: variant.sku,
      size: variant.size,
      color: variant.color,
      material: variant.material || '',
      priceOverride: variant.priceOverride != null ? String(variant.priceOverride) : '',
    })
  }

  const resetVariantForm = () => {
    setEditingVariantId(null)
    setVariantForm(EMPTY_VARIANT_FORM)
  }

  const handleVariantSubmit = async (e) => {
    e.preventDefault()
    if (!editingProduct) return
    setActionLoading(true)
    try {
      const payload = {
        sku: variantForm.sku,
        size: variantForm.size,
        color: variantForm.color,
        material: variantForm.material || null,
        priceOverride: variantForm.priceOverride ? Number(variantForm.priceOverride) : null,
      }
      let updated
      if (editingVariantId) {
        updated = await updateVariant(editingProduct.id, editingVariantId, payload)
      } else {
        updated = await addVariant(editingProduct.id, payload)
      }
      setEditingProduct((prev) => ({
        ...prev,
        variants: editingVariantId
          ? prev.variants.map((v) => (v.id === editingVariantId ? updated : v))
          : [...(prev.variants || []), updated],
      }))
      resetVariantForm()
      showToast('Đã lưu biến thể')
    } catch (err) {
      showToast('Lưu biến thể thất bại: ' + err.message)
    } finally {
      setActionLoading(false)
    }
  }

  const handleDeleteVariant = async (variantId) => {
    if (!editingProduct) return
    if (!window.confirm('Xóa biến thể này?')) return
    try {
      await deleteVariant(editingProduct.id, variantId)
      setEditingProduct((prev) => ({ ...prev, variants: prev.variants.filter((v) => v.id !== variantId) }))
      showToast('Đã xóa biến thể')
    } catch (err) {
      showToast('Xóa biến thể thất bại: ' + err.message)
    }
  }

  // ─── Images ────────────────────────────────────────────────────────────────
  const handleImageSelection = (file) => {
    setImageError('')
    if (!file) {
      setImageFile(null)
      setImagePreviewUrl('')
      return
    }

    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif', 'image/avif']
    if (!allowedTypes.includes(file.type)) {
      setImageError('Chỉ chấp nhận JPEG, PNG, WebP, GIF hoặc AVIF.')
      return
    }
    if (file.size > 10 * 1024 * 1024) {
      setImageError('Ảnh không được vượt quá 10 MB.')
      return
    }

    setImageFile(file)
    setImagePreviewUrl(URL.createObjectURL(file))
  }

  const handleUploadImage = async (e) => {
    e.preventDefault()
    if (!editingProduct || !imageFile) return
    setImageUploading(true)
    setImageError('')
    try {
      const oldPrimary = imageIsPrimary
        ? (editingProduct.images || []).find((image) => image.isPrimary)
        : null
      const image = await uploadImage(editingProduct.id, imageFile, imageIsPrimary)
      setEditingProduct((prev) => ({
        ...prev,
        images: [
          ...(imageIsPrimary
            ? (prev.images || []).map((item) => ({ ...item, isPrimary: false }))
            : (prev.images || [])),
          image,
        ],
      }))
      if (oldPrimary?.id) {
        try {
          await deleteImage(editingProduct.id, oldPrimary.id)
          setEditingProduct((prev) => ({
            ...prev,
            images: (prev.images || []).filter((item) => item.id !== oldPrimary.id),
          }))
        } catch {
          showToast('Ảnh mới đã lưu; không thể dọn ảnh cũ.')
        }
      }
      setImageFile(null)
      setImagePreviewUrl('')
      setImageIsPrimary(false)
      showToast('Đã tải ảnh lên')
    } catch (err) {
      setImageError(err.message || 'Không thể tải ảnh lên.')
      showToast('Tải ảnh lên thất bại: ' + err.message)
    } finally {
      setImageUploading(false)
    }
  }

  const handleDeleteImage = async (imageId) => {
    if (!editingProduct) return
    try {
      await deleteImage(editingProduct.id, imageId)
      setEditingProduct((prev) => ({ ...prev, images: prev.images.filter((img) => img.id !== imageId) }))
      showToast('Đã xóa ảnh')
    } catch (err) {
      showToast('Xóa ảnh thất bại: ' + err.message)
    }
  }

  // ─── Categories ────────────────────────────────────────────────────────────
  const openCategoryDrawer = () => {
    setEditingCategoryId(null)
    setCategoryForm(EMPTY_CATEGORY_FORM)
    setCategoryDrawerOpen(true)
  }

  const startEditCategory = (category) => {
    setEditingCategoryId(category.id)
    setCategoryForm({
      name: category.name,
      slug: category.slug,
      parentId: category.parentId ? String(category.parentId) : '',
    })
  }

  const handleCategorySubmit = async (e) => {
    e.preventDefault()
    setActionLoading(true)
    try {
      const payload = {
        name: categoryForm.name,
        slug: categoryForm.slug,
        parentId: categoryForm.parentId ? Number(categoryForm.parentId) : null,
      }
      if (editingCategoryId) {
        await updateCategory(editingCategoryId, payload)
        showToast('Đã cập nhật danh mục')
      } else {
        await createCategory(payload)
        showToast('Đã tạo danh mục')
      }
      setEditingCategoryId(null)
      setCategoryForm(EMPTY_CATEGORY_FORM)
      fetchCategories()
    } catch (err) {
      showToast('Thao tác danh mục thất bại: ' + err.message)
    } finally {
      setActionLoading(false)
    }
  }

  const handleDeleteCategory = async (categoryId) => {
    if (!window.confirm('Xóa danh mục này?')) return
    try {
      await deleteCategory(categoryId)
      showToast('Đã xóa danh mục')
      fetchCategories()
    } catch (err) {
      showToast('Xóa danh mục thất bại: ' + err.message)
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
          <p className="text-sm text-on-surface-variant mt-1">
            {loading ? 'Đang tải…' : `${totalElements} sản phẩm`}
          </p>
        </div>
        <div className="flex gap-2">
          <button onClick={() => fetchProducts(currentPage)} disabled={loading} className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-high transition-colors disabled:opacity-40">
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} /> Làm mới
          </button>
          <button onClick={openCategoryDrawer} className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium bg-surface-container hover:bg-surface-container-high transition-colors">
            <Tag size={14} /> Danh mục
          </button>
          <button onClick={openAddDrawer} className="bg-primary text-on-primary px-4 py-2 rounded-lg text-sm font-medium flex items-center gap-2 hover:opacity-90">
            <Plus size={14} /> Thêm sản phẩm
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-error/10 border border-error/20 rounded-lg px-4 py-3 text-sm text-error">{error}</div>
      )}

      <div className="bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
        <div className="p-4 border-b border-outline-variant/20 flex flex-wrap gap-3">
          <div className="relative max-w-sm flex-1 min-w-[200px]">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm theo tên sản phẩm…"
              className="w-full pl-9 pr-4 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none"
            />
          </div>
          <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)} className="px-3 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none">
            <option value="">Mọi danh mục</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="px-3 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none">
            <option value="">Mọi trạng thái</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="INACTIVE">INACTIVE</option>
            <option value="DISCONTINUED">DISCONTINUED</option>
          </select>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-surface-container-low/50">
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Product</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Category</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Base Price</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Variants</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Status</th>
                <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/5">
              {loading && products.length === 0 ? (
                <tr><td colSpan={6} className="text-center py-12 text-sm text-on-surface-variant">Đang tải…</td></tr>
              ) : products.length === 0 ? (
                <tr><td colSpan={6} className="text-center py-12 text-sm text-on-surface-variant">Không tìm thấy sản phẩm nào.</td></tr>
              ) : products.map((product) => (
                <tr key={product.id} className="hover:bg-surface-container-high/30 cursor-pointer" onClick={() => openEditDrawer(product)}>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-3">
                      <ProductImage
                        src={(product.images || []).find((image) => image.isPrimary)?.imageUrl || product.images?.[0]?.imageUrl}
                        alt={product.name}
                        className="w-10 h-10 rounded-lg object-cover"
                      />
                      <p className="text-sm font-medium text-primary">{product.name}</p>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-xs text-on-surface-variant">{product.categoryName || categoryName(product.categoryId)}</td>
                  <td className="px-4 py-3 text-sm text-primary">{Number(product.basePrice).toLocaleString('vi-VN')}</td>
                  <td className="px-4 py-3 text-xs text-on-surface-variant">{product.variants?.length || 0}</td>
                  <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                    <button
                      onClick={() => handleToggleStatus(product)}
                      className={`text-xs font-medium px-2 py-0.5 rounded-full ${STATUS_STYLES[product.status] || STATUS_STYLES.INACTIVE}`}
                      title="Bấm để chuyển đổi trạng thái ACTIVE/INACTIVE"
                    >
                      {product.status}
                    </button>
                  </td>
                  <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                    <div className="flex gap-1">
                      <button onClick={() => openEditDrawer(product)} className="p-1.5 rounded hover:bg-surface-container-high"><Edit size={14} className="text-on-surface-variant" /></button>
                      <button onClick={() => handleDelete(product.id)} className="p-1.5 rounded hover:bg-surface-container-high"><Trash2 size={14} className="text-error" /></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-outline-variant/20 text-sm text-on-surface-variant">
            <span>Trang {currentPage + 1} / {totalPages} · {totalElements} products</span>
            <div className="flex gap-2">
              <button onClick={() => fetchProducts(currentPage - 1)} disabled={currentPage === 0 || loading} className="p-1.5 rounded-lg hover:bg-surface-container-high disabled:opacity-30 transition-colors">
                <ChevronLeft size={16} />
              </button>
              <button onClick={() => fetchProducts(currentPage + 1)} disabled={currentPage >= totalPages - 1 || loading} className="p-1.5 rounded-lg hover:bg-surface-container-high disabled:opacity-30 transition-colors">
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Product create/edit drawer */}
      <Drawer isOpen={drawerOpen} onClose={() => setDrawerOpen(false)} title={editingProduct ? 'Edit Product' : 'Add Product'}>
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Product Name</label>
            <input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
          </div>
          <div>
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Description</label>
            <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={3} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
          </div>
          <div>
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Base Price</label>
            <input required type="number" step="0.01" min="0.01" value={form.basePrice} onChange={(e) => setForm({ ...form, basePrice: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
          </div>
          <div>
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Category</label>
            <select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none">
              <option value="">Uncategorized</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Aesthetic Style</label>
            <input value={form.aestheticStyle} onChange={(e) => setForm({ ...form, aestheticStyle: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Target Demographic</label>
              <input value={form.targetDemographic} onChange={(e) => setForm({ ...form, targetDemographic: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
            </div>
            <div>
              <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Seasonal Property</label>
              <input value={form.seasonalProperty} onChange={(e) => setForm({ ...form, seasonalProperty: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
            </div>
          </div>
          <div>
            <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Status</label>
            <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none">
              <option value="ACTIVE">ACTIVE</option>
              <option value="INACTIVE">INACTIVE</option>
              <option value="DISCONTINUED">DISCONTINUED</option>
            </select>
          </div>
          <div className="flex gap-3 pt-2">
            <button type="submit" disabled={actionLoading || imageUploading} className="flex-1 bg-primary text-on-primary px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-50">
              {actionLoading || imageUploading ? <Loader2 size={14} className="animate-spin mx-auto" /> : (editingProduct ? 'Update Product' : 'Add Product')}
            </button>
            <button type="button" onClick={() => setDrawerOpen(false)} className="px-4 py-2 rounded-lg text-sm font-medium bg-surface-container text-on-surface hover:bg-surface-container-high">
              Close
            </button>
          </div>

          {/* Variants — only available once the product exists */}
          {editingProduct && (
            <div className="pt-4 border-t border-outline-variant/20 space-y-3">
              <h3 className="font-title-lg text-primary">Variants</h3>
              <p className="text-xs text-on-surface-variant">SKU must be unique across all products.</p>
              <div className="space-y-2">
                {(editingProduct.variants || []).map((v) => (
                  <div key={v.id} className="flex items-center justify-between bg-surface-container rounded-lg px-3 py-2 text-xs">
                    <span>{v.sku} · {v.size} · {v.color}{v.priceOverride ? ` · ${Number(v.priceOverride).toLocaleString('vi-VN')}` : ''}</span>
                    <div className="flex gap-1">
                      <button type="button" onClick={() => startEditVariant(v)} className="p-1 rounded hover:bg-surface-container-high"><Edit size={12} /></button>
                      <button type="button" onClick={() => handleDeleteVariant(v.id)} className="p-1 rounded hover:bg-surface-container-high"><Trash2 size={12} className="text-error" /></button>
                    </div>
                  </div>
                ))}
              </div>
              <div className="grid grid-cols-2 gap-2">
                <input placeholder="SKU" value={variantForm.sku} onChange={(e) => setVariantForm({ ...variantForm, sku: e.target.value })} className="bg-surface-container rounded-lg px-3 py-2 text-xs border-0 outline-none" />
                <input placeholder="Size" value={variantForm.size} onChange={(e) => setVariantForm({ ...variantForm, size: e.target.value })} className="bg-surface-container rounded-lg px-3 py-2 text-xs border-0 outline-none" />
                <input placeholder="Color" value={variantForm.color} onChange={(e) => setVariantForm({ ...variantForm, color: e.target.value })} className="bg-surface-container rounded-lg px-3 py-2 text-xs border-0 outline-none" />
                <input placeholder="Material (optional)" value={variantForm.material} onChange={(e) => setVariantForm({ ...variantForm, material: e.target.value })} className="bg-surface-container rounded-lg px-3 py-2 text-xs border-0 outline-none" />
                <input placeholder="Price override (optional)" type="number" step="0.01" value={variantForm.priceOverride} onChange={(e) => setVariantForm({ ...variantForm, priceOverride: e.target.value })} className="bg-surface-container rounded-lg px-3 py-2 text-xs border-0 outline-none col-span-2" />
              </div>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={handleVariantSubmit}
                  disabled={actionLoading || !variantForm.sku || !variantForm.size || !variantForm.color}
                  className="flex-1 bg-surface-container-high text-primary px-3 py-2 rounded-lg text-xs font-medium hover:opacity-90 disabled:opacity-40"
                >
                  {editingVariantId ? 'Save Variant' : 'Add Variant'}
                </button>
                {editingVariantId && (
                  <button type="button" onClick={resetVariantForm} className="px-3 py-2 rounded-lg text-xs font-medium bg-surface-container">Cancel</button>
                )}
              </div>
            </div>
          )}

          <div className="pt-4 border-t border-outline-variant/20 space-y-3">
              <div>
                <h3 className="font-title-lg text-primary">Images</h3>
                <p className="text-xs text-on-surface-variant mt-1">
                  JPEG, PNG, WebP, GIF or AVIF · maximum 10 MB
                </p>
              </div>
              <div className="grid grid-cols-3 gap-2">
                {imagePreviewUrl && (
                  <div className="relative">
                    <img src={imagePreviewUrl} alt="Selected product preview" className="w-full aspect-square object-cover rounded-lg" />
                    <span className="absolute bottom-1 left-1 text-[10px] bg-surface-container-lowest/90 text-primary px-1.5 py-0.5 rounded">Preview</span>
                  </div>
                )}
                {(editingProduct?.images || []).map((img) => (
                  <div key={img.id} className="relative">
                    <img src={img.imageUrl} alt="" className="w-full aspect-square object-cover rounded-lg" />
                    {img.isPrimary && <span className="absolute top-1 left-1 text-[10px] bg-primary text-on-primary px-1.5 py-0.5 rounded-full">Primary</span>}
                    <button type="button" onClick={() => handleDeleteImage(img.id)} className="absolute top-1 right-1 p-1 rounded-full bg-surface-container-lowest/90 hover:bg-error/20">
                      <X size={12} className="text-error" />
                    </button>
                  </div>
                ))}
              </div>
              <div className="flex items-center gap-2">
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp,image/gif,image/avif"
                  onChange={(e) => handleImageSelection(e.target.files?.[0] || null)}
                  className="text-xs flex-1 min-w-0"
                />
                <label className="flex items-center gap-1 text-xs text-on-surface-variant">
                  <input type="checkbox" checked={imageIsPrimary} onChange={(e) => setImageIsPrimary(e.target.checked)} /> Primary
                </label>
              </div>
              {imageError && <p role="alert" className="text-xs text-error">{imageError}</p>}
              {editingProduct ? (
                <button
                  type="button"
                  onClick={handleUploadImage}
                  disabled={imageUploading || !imageFile}
                  className="w-full flex items-center justify-center gap-2 bg-surface-container-high text-primary px-3 py-2 rounded-lg text-xs font-medium hover:opacity-90 disabled:opacity-40"
                >
                  {imageUploading ? <Loader2 size={14} className="animate-spin" /> : <ImagePlus size={14} />}
                  {imageUploading ? 'Uploading…' : 'Upload Image'}
                </button>
              ) : (
                <p className="text-xs text-on-surface-variant">
                  The selected image uploads after the product is created.
                </p>
              )}
          </div>
        </form>
      </Drawer>

      {/* Category management drawer */}
      <Drawer isOpen={categoryDrawerOpen} onClose={() => setCategoryDrawerOpen(false)} title="Manage Categories">
        <form onSubmit={handleCategorySubmit} className="space-y-3 pb-4 border-b border-outline-variant/20">
          <input required placeholder="Name" value={categoryForm.name} onChange={(e) => setCategoryForm({ ...categoryForm, name: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
          <input required placeholder="Slug" value={categoryForm.slug} onChange={(e) => setCategoryForm({ ...categoryForm, slug: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
          <select value={categoryForm.parentId} onChange={(e) => setCategoryForm({ ...categoryForm, parentId: e.target.value })} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none">
            <option value="">No parent</option>
            {categories.filter((c) => c.id !== editingCategoryId).map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
          <div className="flex gap-2">
            <button type="submit" disabled={actionLoading} className="flex-1 bg-primary text-on-primary px-3 py-2 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-50">
              {editingCategoryId ? 'Save Category' : 'Add Category'}
            </button>
            {editingCategoryId && (
              <button type="button" onClick={() => { setEditingCategoryId(null); setCategoryForm(EMPTY_CATEGORY_FORM) }} className="px-3 py-2 rounded-lg text-sm font-medium bg-surface-container">
                Cancel
              </button>
            )}
          </div>
        </form>
        <div className="pt-4 space-y-2">
          {categories.length === 0 && <p className="text-xs text-on-surface-variant">No categories yet.</p>}
          {categories.map((c) => (
            <div key={c.id} className="flex items-center justify-between bg-surface-container rounded-lg px-3 py-2 text-sm">
              <div>
                <p className="text-primary">{c.name}</p>
                <p className="text-xs text-on-surface-variant">{c.slug}</p>
              </div>
              <div className="flex gap-1">
                <button onClick={() => startEditCategory(c)} className="p-1.5 rounded hover:bg-surface-container-high"><Edit size={14} className="text-on-surface-variant" /></button>
                <button onClick={() => handleDeleteCategory(c.id)} className="p-1.5 rounded hover:bg-surface-container-high"><Trash2 size={14} className="text-error" /></button>
              </div>
            </div>
          ))}
        </div>
      </Drawer>
    </div>
  )
}
