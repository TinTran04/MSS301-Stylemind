import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, Mail } from 'lucide-react'
import { forgotPassword } from '../../features/auth/auth.api'
import { setPasswordResetContext } from '../../features/auth/passwordResetSession'
import PasswordRecoveryShell from './PasswordRecoveryShell'

const GENERIC_MESSAGE = 'Nếu email này tồn tại, một mã đặt lại mật khẩu đã được gửi.'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [submitted, setSubmitted] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setLoading(true)

    try {
      const normalizedEmail = email.trim().toLowerCase()
      await forgotPassword(normalizedEmail)
      setPasswordResetContext({ email: normalizedEmail })
      setSubmitted(true)
    } catch {
      setError('Không thể bắt đầu khôi phục mật khẩu.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <PasswordRecoveryShell
      eyebrow="Khôi phục tài khoản"
      title="Quên mật khẩu?"
      description="Nhập email gắn với tài khoản StyleMind của bạn."
    >
      {submitted ? (
        <div role="status" className="space-y-6">
          <div className="border-l-2 border-tertiary-container pl-4">
            <p className="font-medium text-primary">Hãy kiểm tra hộp thư của bạn</p>
            <p className="text-sm text-on-surface-variant mt-1">{GENERIC_MESSAGE}</p>
          </div>
          <Link
            to="/verify-reset-otp"
            className="flex w-full items-center justify-center bg-primary text-on-primary rounded-lg py-3 text-sm font-medium uppercase"
          >
            Nhập mã xác minh
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="recovery-email" className="block font-label-sm uppercase text-on-surface-variant mb-2">
              Email
            </label>
            <div className="relative">
              <Mail size={16} className="absolute left-0 top-1/2 -translate-y-1/2 text-on-surface-variant" />
              <input
                id="recovery-email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
                autoComplete="email"
                className="w-full bg-transparent border-0 border-b border-outline-variant py-2 pl-7 text-sm text-on-surface focus:border-tertiary-container focus:outline-none"
                placeholder="tenban@email.com"
              />
            </div>
          </div>

          {error && <p role="alert" className="text-sm text-error">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-primary text-on-primary rounded-lg py-3 text-sm font-medium uppercase disabled:opacity-50"
          >
            {loading ? 'Đang gửi mã...' : 'Gửi mã đặt lại'}
          </button>
        </form>
      )}

      <Link to="/login" className="inline-flex items-center gap-2 text-sm text-on-surface-variant mt-8 hover:text-primary">
        <ArrowLeft size={16} />
        Quay lại đăng nhập
      </Link>
    </PasswordRecoveryShell>
  )
}
