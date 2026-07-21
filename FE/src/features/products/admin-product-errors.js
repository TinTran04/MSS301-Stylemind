import { CREATE_PRODUCT_STEPS } from './admin-product-flow.js'

// Maps backend product-admin errors to friendly, actionable UI messages WITHOUT
// changing the backend contract. The backend keeps its exact codes/messages
// (e.g. PRODUCT_REQUIRES_VARIANT, LAST_ACTIVE_VARIANT, SKU_EXISTS); this only
// changes how they are presented to the admin. The technical `errorCode` is
// returned too so the UI can show it as a small secondary detail.
//
// apiClient normalizes axios/API errors to { message, errorCode, status }.
// Network failures arrive with status === undefined.

// The real project code is SKU_EXISTS; the others are accepted defensively in
// case the contract is extended, per the task's duplicate-SKU guidance.
const DUPLICATE_SKU_CODES = new Set([
  'SKU_EXISTS',
  'DUPLICATE_SKU',
  'SKU_ALREADY_EXISTS',
  'VARIANT_SKU_EXISTS',
])

export function validateProductFields(product) {
  const errors = {}
  if (!product?.name?.trim()) errors.name = 'Tên sản phẩm là bắt buộc.'
  if (!Number.isFinite(Number(product?.basePrice)) || Number(product.basePrice) <= 0) {
    errors.basePrice = 'Giá sản phẩm phải lớn hơn 0.'
  }
  if (!Array.isArray(product?.categoryIds) || product.categoryIds.length === 0) {
    errors.categoryIds = 'Vui lòng chọn ít nhất một danh mục.'
  }
  if (!['ACTIVE', 'INACTIVE', 'DISCONTINUED'].includes(product?.status)) {
    errors.status = 'Vui lòng chọn trạng thái sản phẩm hợp lệ.'
  }
  return errors
}

export function validateVariantFields(variant) {
  const errors = {}
  if (!variant?.sku?.trim()) errors.sku = 'SKU là bắt buộc.'
  if (!variant?.size?.trim()) errors.size = 'Kích cỡ là bắt buộc.'
  if (!variant?.color?.trim()) errors.color = 'Màu sắc là bắt buộc.'
  if (variant?.priceOverride !== '' && variant?.priceOverride != null
      && (!Number.isFinite(Number(variant.priceOverride)) || Number(variant.priceOverride) <= 0)) {
    errors.priceOverride = 'Giá riêng cho biến thể phải lớn hơn 0 nếu được nhập.'
  }
  if (variant?.stockQuantity === '' || variant?.stockQuantity == null
      || !Number.isFinite(Number(variant.stockQuantity)) || Number(variant.stockQuantity) < 0) {
    errors.stockQuantity = 'Số lượng tồn kho phải lớn hơn hoặc bằng 0.'
  }
  return errors
}

export function getAdminProductSuccessMessage(action) {
  if (action === 'updateProduct') {
    return {
      title: 'Cập nhật sản phẩm thành công',
      message: 'Thông tin sản phẩm đã được lưu.',
    }
  }

  return null
}

/**
 * @param {{message?:string, errorCode?:string, status?:number}} error
 * @param {{action?:string, fieldErrors?:object}} [context]
 * @returns {{title:string, message:string, actionLabel?:string, targetStep?:string, fieldErrors?:object, errorCode?:string}}
 */
export function getAdminProductErrorMessage(error, context = {}) {
  const code = error?.errorCode
  const status = error?.status

  // 1) Known business contracts — exact backend codes, friendly presentation.
  if (code === 'PRODUCT_REQUIRES_VARIANT') {
    return {
      title: 'Chưa thể công khai sản phẩm',
      message: 'Vui lòng thêm ít nhất một biến thể trước khi công khai sản phẩm này.',
      actionLabel: 'Đi tới phần Biến thể',
      targetStep: CREATE_PRODUCT_STEPS.VARIANTS,
      errorCode: code,
    }
  }
  if (code === 'LAST_ACTIVE_VARIANT') {
    return {
      title: 'Không thể xóa biến thể cuối cùng',
      message: 'Đây là biến thể cuối cùng của một sản phẩm đang hoạt động. Vui lòng chuyển sản phẩm sang trạng thái Ngừng bán trước khi xóa biến thể này.',
      actionLabel: 'Chuyển sang Ngừng bán trước',
      targetStep: CREATE_PRODUCT_STEPS.VARIANTS,
      errorCode: code,
    }
  }
  if (code === 'DUPLICATE_VARIANT') {
    return {
      title: 'Biến thể đã tồn tại',
      message: 'Biến thể này đã tồn tại. Vui lòng kiểm tra lại kích cỡ, màu sắc và chất liệu.',
      targetStep: CREATE_PRODUCT_STEPS.VARIANTS,
      errorCode: code,
    }
  }
  if (DUPLICATE_SKU_CODES.has(code)) {
    return {
      title: 'SKU đã tồn tại',
      message: 'Vui lòng dùng SKU khác. Mỗi biến thể phải có một SKU duy nhất.',
      targetStep: CREATE_PRODUCT_STEPS.VARIANTS,
      fieldErrors: { sku: 'SKU này đã được sử dụng.' },
      errorCode: code,
    }
  }
  if (code === 'CATEGORY_IN_USE') {
    return {
      title: 'Danh mục đang được sử dụng',
      message: 'Vui lòng chuyển các sản phẩm đó sang danh mục khác trước khi xóa danh mục này.',
      errorCode: code,
    }
  }
  if (code === 'CATEGORY_HAS_CHILDREN') {
    return {
      title: 'Danh mục có danh mục con',
      message: 'Vui lòng di chuyển hoặc xóa các danh mục con trước khi xóa danh mục này.',
      errorCode: code,
    }
  }
  if (code === 'SLUG_EXISTS') {
    return {
      title: 'Đường dẫn danh mục đã tồn tại',
      message: 'Vui lòng dùng đường dẫn khác cho danh mục này.',
      fieldErrors: { slug: 'Đường dẫn danh mục này đã được sử dụng.' },
      errorCode: code,
    }
  }
  if (code === 'VALIDATION_ERROR' && context.action === 'saveVariant') {
    return {
      title: 'Vui lòng kiểm tra thông tin biến thể',
      message: 'SKU, kích cỡ và màu sắc là bắt buộc. Giá riêng cho biến thể phải lớn hơn 0 nếu được nhập.',
      targetStep: CREATE_PRODUCT_STEPS.VARIANTS,
      fieldErrors: context.fieldErrors || {},
      errorCode: code,
    }
  }
  if (code === 'VALIDATION_ERROR') {
    return {
      title: 'Vui lòng kiểm tra thông tin sản phẩm',
      message: 'Một số thông tin sản phẩm đang bị thiếu hoặc không hợp lệ.',
      targetStep: CREATE_PRODUCT_STEPS.PRODUCT_INFO,
      fieldErrors: context.fieldErrors || {},
      errorCode: code,
    }
  }
  if (code === 'IMAGE_STORAGE_NOT_CONFIGURED') {
    return {
      title: 'Chưa cấu hình dịch vụ lưu ảnh',
      message: 'Hệ thống chưa thể lưu ảnh sản phẩm. Vui lòng kiểm tra cấu hình Cloudinary.',
      targetStep: CREATE_PRODUCT_STEPS.IMAGES,
      errorCode: code,
    }
  }

  // 2) Auth — take precedence over generic/context handling.
  if (status === 401) {
    return {
      title: 'Phiên đăng nhập đã hết hạn',
      message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
      errorCode: code,
    }
  }
  if (status === 403) {
    return {
      title: 'Không có quyền truy cập',
      message: 'Bạn không có quyền thực hiện thao tác quản trị này.',
      errorCode: code,
    }
  }

  if (context.action === 'loadCategories') {
    return {
      title: 'Không thể tải danh mục',
      message: 'Hệ thống chưa thể tải danh mục sản phẩm. Vui lòng làm mới trang hoặc thử lại sau.',
      actionLabel: 'Thử lại',
      errorCode: code,
    }
  }
  // Category conflicts (CATEGORY_IN_USE/CATEGORY_HAS_CHILDREN/SLUG_EXISTS) are
  // already handled above by errorCode; these are the generic fallback when a
  // create/update/delete fails for another reason (validation, network, 5xx).
  if (context.action === 'createCategory') {
    return {
      title: 'Không thể tạo danh mục',
      message: 'Không thể tạo danh mục. Vui lòng kiểm tra thông tin và thử lại.',
      errorCode: code,
    }
  }
  if (context.action === 'updateCategory') {
    return {
      title: 'Không thể cập nhật danh mục',
      message: 'Không thể cập nhật danh mục. Vui lòng thử lại sau.',
      errorCode: code,
    }
  }
  if (context.action === 'deleteCategory') {
    return {
      title: 'Không thể xóa danh mục',
      message: 'Không thể xóa danh mục. Vui lòng thử lại sau.',
      errorCode: code,
    }
  }

  // 3) Image upload after the product already exists = partial success.
  //    The product must NOT be rolled back; admin can retry from the Images step.
  if (context.action === 'uploadImage') {
    return {
      title: 'Sản phẩm đã được lưu, nhưng tải ảnh thất bại',
      message: 'Không thể tải ảnh lên. Vui lòng kiểm tra định dạng, dung lượng ảnh hoặc thử lại sau.',
      targetStep: CREATE_PRODUCT_STEPS.IMAGES,
      errorCode: code,
    }
  }

  // 4) Network / service unavailable (no status, or 5xx).
  if (status === undefined || status === 0 || status >= 500) {
    return {
      title: 'Dịch vụ tạm thời không khả dụng',
      message: 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra kết nối mạng hoặc thử lại sau.',
      errorCode: code,
    }
  }

  // 5) Fallback — friendly primary; keep the raw server message as the detail.
  return {
    title: 'Đã xảy ra lỗi',
    message: 'Không thể hoàn tất thao tác. Vui lòng thử lại sau.',
    technicalMessage: error?.message,
    errorCode: code,
  }
}
