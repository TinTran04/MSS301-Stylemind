import { useState, useEffect, useCallback } from 'react'
import { Search, Plus, Edit, Trash2, Loader2, RefreshCw, ChevronLeft, ChevronRight, Tag, ImagePlus, X, CircleAlert } from 'lucide-react'
import Drawer from '../../components/common/Drawer'
import Modal from '../../components/common/Modal'
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
import {
  CREATE_PRODUCT_STEPS,
  buildInitialProductPayload,
  canPublishProduct,
  getNextCreateStep,
} from '../../features/products/admin-product-flow'
import {
  getAdminProductErrorMessage,
  validateProductFields,
  validateVariantFields,
} from '../../features/products/admin-product-errors'

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
const CREATE_STEP_ITEMS = [
  { id: CREATE_PRODUCT_STEPS.PRODUCT_INFO, label: 'Product Info' },
  { id: CREATE_PRODUCT_STEPS.VARIANTS, label: 'Variants' },
  { id: CREATE_PRODUCT_STEPS.IMAGES, label: 'Images / Finish' },
]

function FriendlyErrorAlert({ error, onAction }) {
  if (!error) return null
  return (
    <div role="alert" className="flex items-start gap-3 rounded-lg border border-error/25 bg-error/10 px-3 py-3 text-error">
      <CircleAlert size={17} className="mt-0.5 shrink-0" />
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium">{error.title}</p>
        <p className="mt-1 text-xs leading-5">{error.message}</p>
        {error.actionLabel && onAction && (
          <button type="button" onClick={onAction} className="mt-2 text-xs font-medium underline underline-offset-2">
            {error.actionLabel}
          </button>
        )}
      </div>
    </div>
  )
}

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
  const [productActionError, setProductActionError] = useState(null)
  const [variantActionError, setVariantActionError] = useState(null)
  const [categoryActionError, setCategoryActionError] = useState(null)
  const [productFieldErrors, setProductFieldErrors] = useState({})
  const [variantFieldErrors, setVariantFieldErrors] = useState({})
  const [confirmDialog, setConfirmDialog] = useState(null)

  const [drawerOpen, setDrawerOpen] = useState(false)
  const [drawerMode, setDrawerMode] = useState('create')
  const [createStep, setCreateStep] = useState(CREATE_PRODUCT_STEPS.PRODUCT_INFO)
  const [editingProduct, setEditingProduct] = useState(null)
  const [form, setForm] = useState(EMPTY_PRODUCT_FORM)
  const [flowMessage, setFlowMessage] = useState('')
  const [isPublishing, setIsPublishing] = useState(false)

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

  // Variant operations (add/update/delete) render their friendly error inline
  // next to the Variants section instead of the shared drawer-top alert, so
  // admins see it right where the affected variant lives.
  const presentProductError = (err, context = {}) => {
    const friendly = getAdminProductErrorMessage(err, context)
    const isVariantScoped = context.action === 'saveVariant' || context.action === 'deleteVariant'
    if (isVariantScoped) {
      setVariantActionError(friendly)
    } else {
      setProductActionError(friendly)
      if (friendly.targetStep && drawerMode === 'create') {
        setCreateStep(friendly.targetStep)
      }
    }
    if (context.action === 'saveProduct') {
      setProductFieldErrors(friendly.fieldErrors || {})
    }
    if (context.action === 'saveVariant') {
      setVariantFieldErrors(friendly.fieldErrors || {})
    }
    if (context.action === 'uploadImage') {
      setImageError(friendly.message)
    }
    // Variant errors already show inline in the Variants section; a global
    // toast would duplicate the message and imply it happened outside the drawer.
    if (!isVariantScoped) {
      showToast(`${friendly.title}. ${friendly.message}`)
    }
    return friendly
  }

  const clearProductErrors = () => {
    setProductActionError(null)
    setVariantActionError(null)
    setProductFieldErrors({})
    setVariantFieldErrors({})
  }

  const requestConfirm = ({ title, message, confirmLabel, onConfirm }) => {
    setConfirmDialog({ title, message, confirmLabel, onConfirm })
  }

  const closeConfirmDialog = () => setConfirmDialog(null)

  const handleConfirmAccept = async () => {
    const action = confirmDialog?.onConfirm
    setConfirmDialog(null)
    if (action) await action()
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
      setCategoryActionError(null)
    } catch (err) {
      setCategories([])
      setCategoryActionError(getAdminProductErrorMessage(err, { action: 'loadCategories' }))
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
        const friendly = getAdminProductErrorMessage(err, { action: 'loadProducts' })
        setError(`${friendly.title}. ${friendly.message}`)
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
  const isGuidedCreate = drawerMode === 'create'
  const hasPersistedVariants = canPublishProduct(editingProduct)

  const closeProductDrawer = () => {
    setDrawerOpen(false)
    setEditingProduct(null)
    setFlowMessage('')
    setCreateStep(CREATE_PRODUCT_STEPS.PRODUCT_INFO)
    setVariantForm(EMPTY_VARIANT_FORM)
    setEditingVariantId(null)
    setImageFile(null)
    setImagePreviewUrl('')
    setImageError('')
    clearProductErrors()
  }

  const requestCloseProductDrawer = () => {
    if (isGuidedCreate && editingProduct && !hasPersistedVariants) {
      requestConfirm({
        title: 'Đóng khi chưa có biến thể?',
        message: 'Sản phẩm này chưa có biến thể và sẽ ở trạng thái INACTIVE. Bạn có chắc chắn muốn đóng lại không?',
        confirmLabel: 'Đóng',
        onConfirm: closeProductDrawer,
      })
      return
    }
    closeProductDrawer()
  }

  const openAddDrawer = () => {
    if (categories.length === 0) return
    setDrawerMode('create')
    setCreateStep(CREATE_PRODUCT_STEPS.PRODUCT_INFO)
    setEditingProduct(null)
    setFlowMessage('')
    setForm({
      ...EMPTY_PRODUCT_FORM,
      categoryId: categories[0]?.id ? String(categories[0].id) : '',
      status: 'INACTIVE',
    })
    setVariantForm(EMPTY_VARIANT_FORM)
    setEditingVariantId(null)
    setImageFile(null)
    setImagePreviewUrl('')
    setImageError('')
    setImageIsPrimary(true)
    clearProductErrors()
    setDrawerOpen(true)
  }

  const openEditDrawer = (product) => {
    setDrawerMode('edit')
    setCreateStep(CREATE_PRODUCT_STEPS.PRODUCT_INFO)
    setFlowMessage('')
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
    clearProductErrors()
    setDrawerOpen(true)
  }

  const handleDelete = (productId) => {
    requestConfirm({
      title: 'Xóa sản phẩm?',
      message: 'Bạn có chắc chắn muốn xóa sản phẩm này không? Thao tác này không thể hoàn tác.',
      confirmLabel: 'Xóa sản phẩm',
      onConfirm: async () => {
        try {
          await deleteProduct(productId)
          showToast('Đã xóa sản phẩm')
          fetchProducts(currentPage)
        } catch (err) {
          presentProductError(err, { action: 'deleteProduct' })
        }
      },
    })
  }

  const handleToggleStatus = async (product) => {
    const nextStatus = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      setProductActionError(null)
      await updateProductStatus(product.id, nextStatus)
      showToast('Đã cập nhật trạng thái')
      fetchProducts(currentPage)
    } catch (err) {
      if (err.errorCode === 'PRODUCT_REQUIRES_VARIANT') {
        openEditDrawer(product)
      }
      presentProductError(err, { action: 'updateStatus' })
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (isGuidedCreate && createStep !== CREATE_PRODUCT_STEPS.PRODUCT_INFO) return
    const fieldErrors = validateProductFields(form)
    if (Object.keys(fieldErrors).length > 0) {
      presentProductError(
        { errorCode: 'VALIDATION_ERROR', status: 400 },
        { action: 'saveProduct', fieldErrors },
      )
      return
    }
    setActionLoading(true)
    setProductActionError(null)
    setProductFieldErrors({})
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
      if (drawerMode === 'edit' && editingProduct) {
        const updated = await updateProduct(editingProduct.id, payload)
        setEditingProduct((current) => ({
          ...updated,
          images: updated.images || current.images || [],
          variants: updated.variants || current.variants || [],
        }))
        showToast('Đã cập nhật sản phẩm')
      } else {
        const created = await createProduct(buildInitialProductPayload(payload))
        setEditingProduct({
          ...created,
          images: created.images || [],
          variants: created.variants || [],
        })
        setForm((current) => ({ ...current, status: 'INACTIVE' }))
        setCreateStep(getNextCreateStep(CREATE_PRODUCT_STEPS.PRODUCT_INFO))
        setFlowMessage('Product has been created as INACTIVE. Add at least one variant before publishing it.')
        showToast('Đã tạo sản phẩm ở trạng thái INACTIVE')
      }
      fetchProducts(currentPage)
    } catch (err) {
      presentProductError(err, { action: 'saveProduct' })
    } finally {
      setActionLoading(false)
    }
  }

  const handleContinueToImages = () => {
    if (!hasPersistedVariants) return
    setCreateStep(getNextCreateStep(CREATE_PRODUCT_STEPS.VARIANTS))
  }

  const handleFinishInactive = async () => {
    await fetchProducts(currentPage)
    showToast('Đã lưu sản phẩm ở trạng thái INACTIVE')
    closeProductDrawer()
  }

  const handlePublishProduct = async () => {
    if (!editingProduct || !hasPersistedVariants) {
      presentProductError(
        { errorCode: 'PRODUCT_REQUIRES_VARIANT', status: 409 },
        { action: 'updateStatus' },
      )
      return
    }
    setIsPublishing(true)
    setProductActionError(null)
    try {
      await updateProductStatus(editingProduct.id, 'ACTIVE')
      await fetchProducts(currentPage)
      showToast('Đã xuất bản sản phẩm')
      closeProductDrawer()
    } catch (err) {
      presentProductError(err, { action: 'updateStatus' })
    } finally {
      setIsPublishing(false)
    }
  }

  // ─── Variants ──────────────────────────────────────────────────────────────
  const startEditVariant = (variant) => {
    setVariantActionError(null)
    setVariantFieldErrors({})
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
    setVariantFieldErrors({})
    setVariantActionError(null)
  }

  const handleVariantSubmit = async (e) => {
    e.preventDefault()
    if (!editingProduct) return
    const fieldErrors = validateVariantFields(variantForm)
    if (Object.keys(fieldErrors).length > 0) {
      presentProductError(
        { errorCode: 'VALIDATION_ERROR', status: 400 },
        { action: 'saveVariant', fieldErrors },
      )
      return
    }
    setActionLoading(true)
    setVariantActionError(null)
    setVariantFieldErrors({})
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
      if (isGuidedCreate) {
        setFlowMessage('At least one variant has been added. Continue to Images when ready.')
      }
      resetVariantForm()
      showToast('Đã lưu biến thể')
      await fetchProducts(currentPage)
    } catch (err) {
      presentProductError(err, { action: 'saveVariant' })
    } finally {
      setActionLoading(false)
    }
  }

  const handleDeleteVariant = (variantId) => {
    if (!editingProduct) return
    requestConfirm({
      title: 'Xóa biến thể?',
      message: 'Bạn có chắc chắn muốn xóa biến thể này không? Thao tác này không thể hoàn tác.',
      confirmLabel: 'Xóa biến thể',
      onConfirm: async () => {
        try {
          setVariantActionError(null)
          await deleteVariant(editingProduct.id, variantId)
          // Only remove the variant from the UI after the API confirms the
          // delete succeeded — if it fails (e.g. LAST_ACTIVE_VARIANT), the
          // variant must stay visible so the inline error below still applies to it.
          const remainingVariants = (editingProduct.variants || []).filter((v) => v.id !== variantId)
          setEditingProduct((prev) => ({ ...prev, variants: remainingVariants }))
          if (isGuidedCreate && remainingVariants.length === 0) {
            setFlowMessage('Product has been created as INACTIVE. Add at least one variant before publishing it.')
          }
          showToast('Đã xóa biến thể')
          await fetchProducts(currentPage)
        } catch (err) {
          presentProductError(err, { action: 'deleteVariant' })
        }
      },
    })
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
    setProductActionError(null)
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
      presentProductError(err, { action: 'uploadImage' })
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
      presentProductError(err, { action: 'deleteImage' })
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
    setCategoryActionError(null)
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
      const friendly = getAdminProductErrorMessage(err, { action: editingCategoryId ? 'updateCategory' : 'createCategory' })
      setCategoryActionError(friendly)
      showToast(`${friendly.title}. ${friendly.message}`)
    } finally {
      setActionLoading(false)
    }
  }

  const handleDeleteCategory = (categoryId) => {
    requestConfirm({
      title: 'Xóa danh mục?',
      message: 'Bạn có chắc chắn muốn xóa danh mục này không? Thao tác này không thể hoàn tác.',
      confirmLabel: 'Xóa danh mục',
      onConfirm: async () => {
        try {
          setCategoryActionError(null)
          await deleteCategory(categoryId)
          showToast('Đã xóa danh mục')
          fetchCategories()
        } catch (err) {
          const friendly = getAdminProductErrorMessage(err, { action: 'deleteCategory' })
          setCategoryActionError(friendly)
          showToast(`${friendly.title}. ${friendly.message}`)
        }
      },
    })
  }

  const handleProductErrorAction = async () => {
    if (productActionError?.targetStep) {
      setCreateStep(productActionError.targetStep)
    }
  }

  const handleVariantErrorAction = async () => {
    if (variantActionError?.errorCode === 'LAST_ACTIVE_VARIANT' && editingProduct) {
      setActionLoading(true)
      try {
        const updated = await updateProductStatus(editingProduct.id, 'INACTIVE')
        setEditingProduct((current) => ({ ...current, ...updated }))
        setForm((current) => ({ ...current, status: 'INACTIVE' }))
        setVariantActionError(null)
        showToast('Đã chuyển sản phẩm sang INACTIVE. Giờ bạn có thể xóa biến thể cuối cùng.')
        await fetchProducts(currentPage)
      } catch (err) {
        presentProductError(err, { action: 'updateStatus' })
      } finally {
        setActionLoading(false)
      }
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
          <button
            onClick={openAddDrawer}
            disabled={categories.length === 0}
            title={categories.length === 0 ? 'Categories must be available before creating a product.' : ''}
            className="bg-primary text-on-primary px-4 py-2 rounded-lg text-sm font-medium flex items-center gap-2 hover:opacity-90 disabled:opacity-40"
          >
            <Plus size={14} /> Thêm sản phẩm
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-error/10 border border-error/20 rounded-lg px-4 py-3 text-sm text-error">{error}</div>
      )}
      <FriendlyErrorAlert error={categoryActionError} onAction={categoryActionError?.actionLabel ? fetchCategories : null} />

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
      <Drawer isOpen={drawerOpen} onClose={requestCloseProductDrawer} title={drawerMode === 'edit' ? 'Edit Product' : 'Add Product'}>
        <form noValidate onSubmit={handleSubmit} className="space-y-6">
          {isGuidedCreate && (
            <div className="grid grid-cols-3 border-b border-outline-variant/20 pb-4">
              {CREATE_STEP_ITEMS.map((step, index) => {
                const activeIndex = CREATE_STEP_ITEMS.findIndex((item) => item.id === createStep)
                const isActive = step.id === createStep
                const isComplete = index < activeIndex
                return (
                  <div key={step.id} className="flex items-center gap-2 min-w-0">
                    <span className={`w-6 h-6 shrink-0 rounded-full flex items-center justify-center text-xs font-medium ${
                      isActive || isComplete
                        ? 'bg-primary text-on-primary'
                        : 'bg-surface-container-high text-on-surface-variant'
                    }`}>
                      {index + 1}
                    </span>
                    <span className={`text-xs truncate ${isActive ? 'text-primary font-medium' : 'text-on-surface-variant'}`}>
                      {step.label}
                    </span>
                  </div>
                )
              })}
            </div>
          )}

          {flowMessage && (
            <div className="bg-tertiary-fixed/20 border border-tertiary/20 rounded-lg px-3 py-2 text-sm text-on-surface">
              {flowMessage}
            </div>
          )}
          <FriendlyErrorAlert
            error={productActionError}
            onAction={productActionError?.actionLabel ? handleProductErrorAction : null}
          />

          {(!isGuidedCreate || createStep === CREATE_PRODUCT_STEPS.PRODUCT_INFO) && (
            <div className="space-y-6">
              <div>
                <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Product Name</label>
                <input
                  required
                  aria-invalid={Boolean(productFieldErrors.name)}
                  value={form.name}
                  onChange={(e) => {
                    setForm({ ...form, name: e.target.value })
                    setProductFieldErrors((current) => ({ ...current, name: undefined }))
                  }}
                  className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none"
                />
                {productFieldErrors.name && <p className="mt-1 text-xs text-error">{productFieldErrors.name}</p>}
              </div>
              <div>
                <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Description</label>
                <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={3} className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none" />
              </div>
              <div>
                <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Base Price</label>
                <input
                  required
                  type="number"
                  step="0.01"
                  min="0.01"
                  aria-invalid={Boolean(productFieldErrors.basePrice)}
                  value={form.basePrice}
                  onChange={(e) => {
                    setForm({ ...form, basePrice: e.target.value })
                    setProductFieldErrors((current) => ({ ...current, basePrice: undefined }))
                  }}
                  className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none"
                />
                {productFieldErrors.basePrice && <p className="mt-1 text-xs text-error">{productFieldErrors.basePrice}</p>}
              </div>
              <div>
                <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Category</label>
                <select
                  required
                  aria-invalid={Boolean(productFieldErrors.categoryId)}
                  value={form.categoryId}
                  onChange={(e) => {
                    setForm({ ...form, categoryId: e.target.value })
                    setProductFieldErrors((current) => ({ ...current, categoryId: undefined }))
                  }}
                  className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none"
                >
                  <option value="" disabled>Select category</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
                {productFieldErrors.categoryId && <p className="mt-1 text-xs text-error">{productFieldErrors.categoryId}</p>}
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
                {isGuidedCreate ? (
                  <span className="inline-flex text-xs font-medium px-2 py-1 rounded-full bg-surface-container-high text-on-surface-variant">
                    INACTIVE
                  </span>
                ) : (
                  <select
                    aria-invalid={Boolean(productFieldErrors.status)}
                    value={form.status}
                    onChange={(e) => {
                      setForm({ ...form, status: e.target.value })
                      setProductFieldErrors((current) => ({ ...current, status: undefined }))
                    }}
                    className="w-full bg-surface-container rounded-lg px-3 py-2 text-sm border-0 outline-none"
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="INACTIVE">INACTIVE</option>
                    <option value="DISCONTINUED">DISCONTINUED</option>
                  </select>
                )}
                {productFieldErrors.status && <p className="mt-1 text-xs text-error">{productFieldErrors.status}</p>}
              </div>

              {isGuidedCreate && (
                <div className="flex gap-3 pt-2">
                  <button type="submit" disabled={actionLoading} className="flex-1 bg-primary text-on-primary px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-50">
                    {actionLoading ? <Loader2 size={14} className="animate-spin mx-auto" /> : 'Create & Continue'}
                  </button>
                  <button type="button" onClick={requestCloseProductDrawer} className="px-4 py-2 rounded-lg text-sm font-medium bg-surface-container text-on-surface hover:bg-surface-container-high">
                    Close
                  </button>
                </div>
              )}
            </div>
          )}

          {editingProduct && (!isGuidedCreate || createStep === CREATE_PRODUCT_STEPS.VARIANTS) && (
            <div className={`${isGuidedCreate ? '' : 'pt-4 border-t border-outline-variant/20'} space-y-3`}>
              <h3 className="font-title-lg text-primary">Variants</h3>
              <p className="text-xs text-on-surface-variant">SKU must be unique across all products.</p>
              {(editingProduct.variants || []).length === 0 && (
                <p className="text-xs text-error">Add at least one variant before continuing or publishing.</p>
              )}
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
              <FriendlyErrorAlert
                error={variantActionError}
                onAction={variantActionError?.actionLabel ? handleVariantErrorAction : null}
              />
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <input
                    placeholder="SKU"
                    aria-invalid={Boolean(variantFieldErrors.sku)}
                    value={variantForm.sku}
                    onChange={(e) => {
                      setVariantForm({ ...variantForm, sku: e.target.value })
                      setVariantFieldErrors((current) => ({ ...current, sku: undefined }))
                    }}
                    className="w-full bg-surface-container rounded-lg px-3 py-2 text-xs border-0 outline-none"
                  />
                  {variantFieldErrors.sku && <p className="mt-1 text-xs text-error">{variantFieldErrors.sku}</p>}
                </div>
                <div>
                  <input
                    placeholder="Size"
                    aria-invalid={Boolean(variantFieldErrors.size)}
                    value={variantForm.size}
                    onChange={(e) => {
                      setVariantForm({ ...variantForm, size: e.target.value })
                      setVariantFieldErrors((current) => ({ ...current, size: undefined }))
                    }}
                    className="w-full bg-surface-container rounded-lg px-3 py-2 text-xs border-0 outline-none"
                  />
                  {variantFieldErrors.size && <p className="mt-1 text-xs text-error">{variantFieldErrors.size}</p>}
                </div>
                <div>
                  <input
                    placeholder="Color"
                    aria-invalid={Boolean(variantFieldErrors.color)}
                    value={variantForm.color}
                    onChange={(e) => {
                      setVariantForm({ ...variantForm, color: e.target.value })
                      setVariantFieldErrors((current) => ({ ...current, color: undefined }))
                    }}
                    className="w-full bg-surface-container rounded-lg px-3 py-2 text-xs border-0 outline-none"
                  />
                  {variantFieldErrors.color && <p className="mt-1 text-xs text-error">{variantFieldErrors.color}</p>}
                </div>
                <input placeholder="Material (optional)" value={variantForm.material} onChange={(e) => setVariantForm({ ...variantForm, material: e.target.value })} className="bg-surface-container rounded-lg px-3 py-2 text-xs border-0 outline-none" />
                <div className="col-span-2">
                  <input
                    min="0.01"
                    placeholder="Price override (optional)"
                    type="number"
                    step="0.01"
                    aria-invalid={Boolean(variantFieldErrors.priceOverride)}
                    value={variantForm.priceOverride}
                    onChange={(e) => {
                      setVariantForm({ ...variantForm, priceOverride: e.target.value })
                      setVariantFieldErrors((current) => ({ ...current, priceOverride: undefined }))
                    }}
                    className="w-full bg-surface-container rounded-lg px-3 py-2 text-xs border-0 outline-none"
                  />
                  {variantFieldErrors.priceOverride && <p className="mt-1 text-xs text-error">{variantFieldErrors.priceOverride}</p>}
                </div>
              </div>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={handleVariantSubmit}
                  disabled={actionLoading}
                  className="flex-1 bg-surface-container-high text-primary px-3 py-2 rounded-lg text-xs font-medium hover:opacity-90 disabled:opacity-40"
                >
                  {editingVariantId ? 'Save Variant' : 'Add Variant'}
                </button>
                {editingVariantId && (
                  <button type="button" onClick={resetVariantForm} className="px-3 py-2 rounded-lg text-xs font-medium bg-surface-container">Cancel</button>
                )}
              </div>

              {isGuidedCreate && (
                <div className="flex gap-3 pt-3">
                  <button
                    type="button"
                    onClick={handleContinueToImages}
                    disabled={!hasPersistedVariants}
                    className="flex-1 bg-primary text-on-primary px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-40"
                  >
                    Continue to Images
                  </button>
                  <button type="button" onClick={requestCloseProductDrawer} className="px-4 py-2 rounded-lg text-sm font-medium bg-surface-container text-on-surface hover:bg-surface-container-high">
                    Close
                  </button>
                </div>
              )}
            </div>
          )}

          {editingProduct && (!isGuidedCreate || createStep === CREATE_PRODUCT_STEPS.IMAGES) && (
            <div className={`${isGuidedCreate ? '' : 'pt-4 border-t border-outline-variant/20'} space-y-3`}>
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
                {(editingProduct.images || []).map((img) => (
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
              <button
                type="button"
                onClick={handleUploadImage}
                disabled={imageUploading || !imageFile}
                className="w-full flex items-center justify-center gap-2 bg-surface-container-high text-primary px-3 py-2 rounded-lg text-xs font-medium hover:opacity-90 disabled:opacity-40"
              >
                {imageUploading ? <Loader2 size={14} className="animate-spin" /> : <ImagePlus size={14} />}
                {imageUploading ? 'Uploading…' : 'Upload Image'}
              </button>

              {isGuidedCreate && (
                <div className="flex gap-3 pt-3">
                  <button
                    type="button"
                    onClick={handleFinishInactive}
                    disabled={!hasPersistedVariants || imageUploading}
                    className="flex-1 px-4 py-2 rounded-lg text-sm font-medium bg-surface-container text-on-surface hover:bg-surface-container-high disabled:opacity-40"
                  >
                    Finish Inactive
                  </button>
                  <button
                    type="button"
                    onClick={handlePublishProduct}
                    disabled={!hasPersistedVariants || imageUploading || isPublishing}
                    className="flex-1 bg-primary text-on-primary px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-40"
                  >
                    {isPublishing ? <Loader2 size={14} className="animate-spin mx-auto" /> : 'Publish Active'}
                  </button>
                </div>
              )}
            </div>
          )}

          {!isGuidedCreate && (
            <div className="flex gap-3 pt-2">
              <button type="submit" disabled={actionLoading || imageUploading} className="flex-1 bg-primary text-on-primary px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90 disabled:opacity-50">
                {actionLoading || imageUploading ? <Loader2 size={14} className="animate-spin mx-auto" /> : 'Update Product'}
              </button>
              <button type="button" onClick={requestCloseProductDrawer} className="px-4 py-2 rounded-lg text-sm font-medium bg-surface-container text-on-surface hover:bg-surface-container-high">
                Close
              </button>
            </div>
          )}
        </form>
      </Drawer>

      {/* Category management drawer */}
      <Drawer isOpen={categoryDrawerOpen} onClose={() => setCategoryDrawerOpen(false)} title="Manage Categories">
        <FriendlyErrorAlert error={categoryActionError} onAction={categoryActionError?.actionLabel ? fetchCategories : null} />
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

      <Modal isOpen={Boolean(confirmDialog)} onClose={closeConfirmDialog} title={confirmDialog?.title}>
        <p className="text-sm text-on-surface-variant">{confirmDialog?.message}</p>
        <div className="flex gap-3 pt-4">
          <button
            type="button"
            onClick={closeConfirmDialog}
            className="flex-1 px-4 py-2 rounded-lg text-sm font-medium bg-surface-container text-on-surface hover:bg-surface-container-high"
          >
            Hủy
          </button>
          <button
            type="button"
            onClick={handleConfirmAccept}
            className="flex-1 bg-error text-white px-4 py-2 rounded-lg text-sm font-medium hover:opacity-90"
          >
            {confirmDialog?.confirmLabel || 'Xóa'}
          </button>
        </div>
      </Modal>
    </div>
  )
}
