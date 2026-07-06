import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getAdminProductErrorMessage,
  validateProductFields,
  validateVariantFields,
} from './admin-product-errors.js'
import { CREATE_PRODUCT_STEPS } from './admin-product-flow.js'

test('PRODUCT_REQUIRES_VARIANT guides admin to add variants', () => {
  const result = getAdminProductErrorMessage({ errorCode: 'PRODUCT_REQUIRES_VARIANT', status: 409 })
  assert.equal(result.title, 'Chưa thể công khai sản phẩm')
  assert.match(result.message, /Vui lòng thêm ít nhất một biến thể/)
  assert.equal(result.actionLabel, 'Đi tới phần Biến thể')
  assert.equal(result.targetStep, CREATE_PRODUCT_STEPS.VARIANTS)
  assert.equal(result.errorCode, 'PRODUCT_REQUIRES_VARIANT')
})

test('LAST_ACTIVE_VARIANT guides admin to deactivate first', () => {
  const result = getAdminProductErrorMessage({ errorCode: 'LAST_ACTIVE_VARIANT', status: 409 })
  assert.equal(result.title, 'Không thể xóa biến thể cuối cùng')
  assert.match(result.message, /chuyển sản phẩm sang INACTIVE trước khi xóa biến thể này/)
  assert.equal(result.actionLabel, 'Chuyển sản phẩm sang INACTIVE trước')
  assert.equal(result.targetStep, CREATE_PRODUCT_STEPS.VARIANTS)
})

test('duplicate SKU (real code SKU_EXISTS) maps to field-level SKU guidance', () => {
  const result = getAdminProductErrorMessage({ errorCode: 'SKU_EXISTS', status: 400 })
  assert.equal(result.title, 'SKU đã tồn tại')
  assert.match(result.message, /Mỗi biến thể phải có một SKU duy nhất/)
  assert.equal(result.targetStep, CREATE_PRODUCT_STEPS.VARIANTS)
  assert.ok(result.fieldErrors && result.fieldErrors.sku)
})

test('defensive duplicate SKU aliases are also handled', () => {
  for (const code of ['DUPLICATE_SKU', 'SKU_ALREADY_EXISTS', 'VARIANT_SKU_EXISTS']) {
    assert.equal(getAdminProductErrorMessage({ errorCode: code, status: 400 }).title, 'SKU đã tồn tại')
  }
})

test('product validation maps to Product Info guidance and field errors', () => {
  const result = getAdminProductErrorMessage(
    { errorCode: 'VALIDATION_ERROR', status: 400 },
    { action: 'saveProduct', fieldErrors: { name: 'Tên sản phẩm là bắt buộc.' } },
  )
  assert.equal(result.title, 'Vui lòng kiểm tra thông tin sản phẩm')
  assert.equal(result.targetStep, CREATE_PRODUCT_STEPS.PRODUCT_INFO)
  assert.equal(result.fieldErrors.name, 'Tên sản phẩm là bắt buộc.')
})

test('variant validation maps to Variants guidance and preserves field errors', () => {
  const result = getAdminProductErrorMessage(
    { errorCode: 'VALIDATION_ERROR', status: 400 },
    { action: 'saveVariant', fieldErrors: { color: 'Màu sắc là bắt buộc.' } },
  )
  assert.equal(result.title, 'Vui lòng kiểm tra thông tin biến thể')
  assert.equal(result.targetStep, CREATE_PRODUCT_STEPS.VARIANTS)
  assert.equal(result.fieldErrors.color, 'Màu sắc là bắt buộc.')
})

test('category loading failure gives retry guidance', () => {
  const result = getAdminProductErrorMessage(
    { message: 'Network Error' },
    { action: 'loadCategories' },
  )
  assert.equal(result.title, 'Không thể tải danh mục')
  assert.match(result.message, /làm mới trang hoặc thử lại sau/)
  assert.equal(result.actionLabel, 'Thử lại')
})

test('generic create-category failure gives friendly Vietnamese guidance', () => {
  const result = getAdminProductErrorMessage({ status: 400 }, { action: 'createCategory' })
  assert.equal(result.title, 'Không thể tạo danh mục')
  assert.match(result.message, /kiểm tra thông tin và thử lại/)
})

test('generic update-category failure gives friendly Vietnamese guidance', () => {
  const result = getAdminProductErrorMessage({ status: 500 }, { action: 'updateCategory' })
  assert.equal(result.title, 'Không thể cập nhật danh mục')
  assert.match(result.message, /Vui lòng thử lại sau/)
})

test('generic delete-category failure gives friendly Vietnamese guidance', () => {
  const result = getAdminProductErrorMessage({ status: 500 }, { action: 'deleteCategory' })
  assert.equal(result.title, 'Không thể xóa danh mục')
  assert.match(result.message, /Vui lòng thử lại sau/)
})

test('specific category conflict codes still take priority over generic create/update/delete fallback', () => {
  const inUse = getAdminProductErrorMessage({ errorCode: 'CATEGORY_IN_USE', status: 409 }, { action: 'deleteCategory' })
  assert.equal(inUse.title, 'Danh mục đang được sử dụng')

  const slugExists = getAdminProductErrorMessage({ errorCode: 'SLUG_EXISTS', status: 409 }, { action: 'createCategory' })
  assert.equal(slugExists.title, 'Slug danh mục đã tồn tại')
})

test('image upload failure is framed as partial success', () => {
  const result = getAdminProductErrorMessage({ status: 500 }, { action: 'uploadImage' })
  assert.equal(result.title, 'Sản phẩm đã được lưu, nhưng tải ảnh thất bại')
  assert.match(result.message, /kiểm tra định dạng, dung lượng ảnh/)
  assert.equal(result.targetStep, CREATE_PRODUCT_STEPS.IMAGES)
})

test('image storage not configured maps to a distinct, actionable message', () => {
  const result = getAdminProductErrorMessage({ errorCode: 'IMAGE_STORAGE_NOT_CONFIGURED', status: 503 }, { action: 'uploadImage' })
  assert.equal(result.title, 'Chưa cấu hình dịch vụ lưu ảnh')
  assert.match(result.message, /kiểm tra cấu hình Cloudinary/)
  assert.equal(result.targetStep, CREATE_PRODUCT_STEPS.IMAGES)
})

test('network error (no status) maps to service unavailable', () => {
  const result = getAdminProductErrorMessage({ message: 'Network Error', errorCode: 'ERR_NETWORK' })
  assert.equal(result.title, 'Dịch vụ tạm thời không khả dụng')
  assert.match(result.message, /Không thể kết nối tới máy chủ/)
})

test('401 maps to session expired', () => {
  assert.equal(getAdminProductErrorMessage({ status: 401 }).title, 'Phiên đăng nhập đã hết hạn')
})

test('403 maps to permission denied', () => {
  assert.equal(getAdminProductErrorMessage({ status: 403 }).title, 'Không có quyền truy cập')
})

test('auth takes precedence over uploadImage context', () => {
  const result = getAdminProductErrorMessage({ status: 401 }, { action: 'uploadImage' })
  assert.equal(result.title, 'Phiên đăng nhập đã hết hạn')
})

test('unknown error uses generic primary copy and retains technical detail separately', () => {
  const result = getAdminProductErrorMessage({ message: 'weird', errorCode: 'ODD', status: 400 })
  assert.equal(result.title, 'Đã xảy ra lỗi')
  assert.equal(result.message, 'Không thể hoàn tất thao tác. Vui lòng thử lại sau.')
  assert.equal(result.technicalMessage, 'weird')
  assert.equal(result.errorCode, 'ODD')
})

test('category conflicts map to actionable category guidance', () => {
  const result = getAdminProductErrorMessage({ errorCode: 'CATEGORY_IN_USE', status: 409 })
  assert.equal(result.title, 'Danh mục đang được sử dụng')
  assert.match(result.message, /chuyển các sản phẩm đó sang danh mục khác/)
})

test('product field validation identifies required and invalid values', () => {
  assert.deepEqual(validateProductFields({
    name: ' ',
    basePrice: '0',
    categoryId: '',
    status: 'UNKNOWN',
  }), {
    name: 'Tên sản phẩm là bắt buộc.',
    basePrice: 'Giá gốc phải lớn hơn 0.',
    categoryId: 'Danh mục là bắt buộc.',
    status: 'Vui lòng chọn trạng thái sản phẩm hợp lệ.',
  })
})

test('variant field validation identifies required and invalid values', () => {
  assert.deepEqual(validateVariantFields({
    sku: '',
    size: ' ',
    color: '',
    priceOverride: '-1',
  }), {
    sku: 'SKU là bắt buộc.',
    size: 'Kích thước là bắt buộc.',
    color: 'Màu sắc là bắt buộc.',
    priceOverride: 'Giá ghi đè phải lớn hơn 0.',
  })
})
