// Shared Vietnamese error normalization for Admin Dashboard pages that don't
// already have their own friendly-error helper (see admin-product-errors.js
// for the Product Management-specific one). Callers pass known business
// errorCodes with their exact required copy; this only fills in the
// universal cases (auth/network) and a generic fallback.
export function getAdminErrorMessage(error, { knownCodes = {}, fallbackTitle, fallbackMessage } = {}) {
  const code = error?.errorCode
  const status = error?.status

  if (code && knownCodes[code]) {
    return { ...knownCodes[code], errorCode: code }
  }
  if (status === 401) {
    return { title: 'Phiên đăng nhập đã hết hạn', message: 'Vui lòng đăng nhập lại để tiếp tục.', errorCode: code }
  }
  if (status === 403) {
    return { title: 'Không có quyền thao tác', message: 'Bạn không có quyền thực hiện thao tác quản trị này.', errorCode: code }
  }
  if (status === undefined || status === 0 || status >= 500) {
    return {
      title: 'Dịch vụ tạm thời không khả dụng',
      message: 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra kết nối mạng hoặc thử lại sau.',
      errorCode: code,
    }
  }
  return {
    title: fallbackTitle || 'Đã xảy ra lỗi',
    message: fallbackMessage || 'Không thể hoàn tất thao tác. Vui lòng thử lại sau.',
    errorCode: code,
  }
}
