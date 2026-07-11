import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Eye, EyeOff, Lock } from 'lucide-react'
import { resetPassword, setupPassword } from '../../features/auth/auth.api'
import {
  clearPasswordResetContext,
  getPasswordResetContext,
} from '../../features/auth/passwordResetSession'
import PasswordRecoveryShell from './PasswordRecoveryShell'
import {
  RESET_PASSWORD_INVALID_MESSAGE,
  RESET_PASSWORD_SUCCESS_MESSAGE,
  SET_PASSWORD_SUCCESS_MESSAGE,
  resolveResetPasswordContext,
} from './resetPassword.utils'

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const [sessionContext] = useState(getPasswordResetContext)
  const navigate = useNavigate()
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [complete, setComplete] = useState(false)
  const resetContext = resolveResetPasswordContext({
    searchParams,
    sessionContext,
  })
  const isInviteSetup = resetContext?.mode === 'setup'

  if (!resetContext?.email || !resetContext?.resetToken) {
    return (
      <PasswordRecoveryShell
        eyebrow="Mật khẩu mới"
        title="Liên kết không hợp lệ"
        description={RESET_PASSWORD_INVALID_MESSAGE}
      >
        <Link
          to="/forgot-password"
          className="flex w-full items-center justify-center bg-primary text-on-primary rounded-lg py-3 text-sm font-medium uppercase"
        >
          Bắt đầu khôi phục tài khoản
        </Link>
      </PasswordRecoveryShell>
    )
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')

    if (newPassword !== confirmPassword) {
      setError('Mật khẩu không khớp.')
      return
    }

    setLoading(true)
    try {
      if (isInviteSetup) {
        await setupPassword(resetContext.email, resetContext.resetToken, newPassword)
      } else {
        await resetPassword(resetContext.email, resetContext.resetToken, newPassword)
      }
      clearPasswordResetContext()
      setComplete(true)
      navigate('/login', {
        replace: true,
        state: {
          flashMessage: isInviteSetup ? SET_PASSWORD_SUCCESS_MESSAGE : RESET_PASSWORD_SUCCESS_MESSAGE,
        },
      })
    } catch {
      setError(RESET_PASSWORD_INVALID_MESSAGE)
    } finally {
      setLoading(false)
    }
  }

  return (
    <PasswordRecoveryShell
      eyebrow="Mật khẩu mới"
      title={complete ? 'Mật khẩu đã được cập nhật' : isInviteSetup ? 'Thiết lập mật khẩu' : 'Chọn mật khẩu mới'}
      description={complete
        ? 'Mật khẩu mới của bạn đã sẵn sàng để sử dụng.'
        : isInviteSetup
          ? 'Hãy chọn mật khẩu mới để hoàn tất thiết lập tài khoản StyleMind.'
          : 'Hãy dùng ít nhất 6 ký tự và chọn một mật khẩu riêng cho StyleMind.'}
    >
      {complete ? (
        <div role="status" className="space-y-6">
          <p className="text-sm text-on-surface-variant">Đang chuyển bạn đến trang đăng nhập…</p>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-6">
          <PasswordField
            id="new-password"
            label="Mật khẩu mới"
            value={newPassword}
            onChange={setNewPassword}
            visible={showPassword}
            onToggle={() => setShowPassword((current) => !current)}
          />
          <PasswordField
            id="confirm-password"
            label="Xác nhận mật khẩu"
            value={confirmPassword}
            onChange={setConfirmPassword}
            visible={showPassword}
          />

          {error && <p role="alert" className="text-sm text-error">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-primary text-on-primary rounded-lg py-3 text-sm font-medium uppercase disabled:opacity-50"
          >
            {loading ? 'Đang cập nhật mật khẩu...' : 'Cập nhật mật khẩu'}
          </button>
        </form>
      )}
    </PasswordRecoveryShell>
  )
}

function PasswordField({ id, label, value, onChange, visible, onToggle }) {
  return (
    <div>
      <label htmlFor={id} className="block font-label-sm uppercase text-on-surface-variant mb-2">
        {label}
      </label>
      <div className="relative">
        <Lock size={16} className="absolute left-0 top-1/2 -translate-y-1/2 text-on-surface-variant" />
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          minLength={6}
          maxLength={100}
          required
          autoComplete="new-password"
          className="w-full bg-transparent border-0 border-b border-outline-variant py-2 pl-7 pr-10 text-sm text-on-surface focus:border-tertiary-container focus:outline-none"
        />
        {onToggle && (
          <button
            type="button"
            onClick={onToggle}
            aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
            className="absolute right-0 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-primary"
          >
            {visible ? <EyeOff size={16} /> : <Eye size={16} />}
          </button>
        )}
      </div>
    </div>
  )
}
