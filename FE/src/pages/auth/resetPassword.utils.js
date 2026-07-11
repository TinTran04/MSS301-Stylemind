export const RESET_PASSWORD_INVALID_MESSAGE = 'Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.'
export const RESET_PASSWORD_SUCCESS_MESSAGE = 'Đặt lại mật khẩu thành công. Vui lòng đăng nhập.'
export const SET_PASSWORD_SUCCESS_MESSAGE = 'Đặt lại mật khẩu thành công. Vui lòng đăng nhập.'

export function resolveResetPasswordContext({ searchParams, sessionContext }) {
  const email = searchParams?.get?.('email')?.trim()
  const token = searchParams?.get?.('token')?.trim()

  if (email && token) {
    return {
      mode: 'setup',
      email: email.toLowerCase(),
      resetToken: token,
    }
  }

  if (sessionContext?.email && sessionContext?.resetToken) {
    return {
      mode: 'reset',
      email: String(sessionContext.email).trim().toLowerCase(),
      resetToken: String(sessionContext.resetToken).trim(),
    }
  }

  return null
}
