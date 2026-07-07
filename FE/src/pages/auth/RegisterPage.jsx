import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowLeft, CheckCircle2, Eye, EyeOff, Lock, Mail } from 'lucide-react'
import {
  registerUser,
  resendRegisterOtp,
  verifyRegisterOtp,
} from '../../features/auth/auth.api'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const RESEND_COOLDOWN_SECONDS = 60

function messageForError(err, fallback) {
  switch (err?.errorCode) {
    case 'EMAIL_ALREADY_EXISTS':
      return 'Email này đã được sử dụng.'
    case 'REGISTER_OTP_INVALID':
      return 'Mã xác minh không đúng hoặc đã hết hạn.'
    case 'REGISTER_OTP_BLOCKED':
      return 'Bạn đã thử quá nhiều lần. Vui lòng yêu cầu mã mới.'
    case 'REGISTER_OTP_COOLDOWN':
      return 'Vui lòng chờ một chút rồi hãy yêu cầu mã mới.'
    case 'NOTIFICATION_FAILED':
      return 'Không thể gửi email xác minh. Vui lòng thử lại.'
    default:
      if (err && err.status === undefined) {
        return 'Lỗi kết nối mạng. Vui lòng kiểm tra lại và thử sau.'
      }
      return fallback
  }
}

export default function RegisterPage() {
  const [step, setStep] = useState('form') // 'form' | 'otp'
  const [formData, setFormData] = useState({ email: '', password: '', confirmPassword: '' })
  const [otp, setOtp] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)
  const [cooldown, setCooldown] = useState(0)
  const navigate = useNavigate()

  useEffect(() => {
    if (cooldown <= 0) return undefined
    const timer = setInterval(() => setCooldown((s) => (s <= 1 ? 0 : s - 1)), 1000)
    return () => clearInterval(timer)
  }, [cooldown])

  const validateForm = () => {
    if (!formData.email.trim()) return 'Email là bắt buộc.'
    if (!EMAIL_PATTERN.test(formData.email.trim())) return 'Vui lòng nhập email hợp lệ.'
    if (!formData.password) return 'Mật khẩu là bắt buộc.'
    if (formData.password.length < 6) return 'Mật khẩu phải có ít nhất 6 ký tự.'
    if (!formData.confirmPassword) return 'Vui lòng xác nhận mật khẩu.'
    if (formData.password !== formData.confirmPassword) return 'Mật khẩu không khớp.'
    return ''
  }

  const handleStartRegistration = async (e) => {
    e.preventDefault()
    setError('')
    const validationError = validateForm()
    if (validationError) {
      setError(validationError)
      return
    }

    setLoading(true)
    try {
      await registerUser({ email: formData.email.trim(), password: formData.password })
      setStep('otp')
      setOtp('')
      setCooldown(RESEND_COOLDOWN_SECONDS)
    } catch (err) {
      setError(messageForError(err, 'Không thể bắt đầu đăng ký.'))
    } finally {
      setLoading(false)
    }
  }

  const handleVerifyOtp = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await verifyRegisterOtp(formData.email.trim(), otp)
      setSuccess('Tài khoản của bạn đã sẵn sàng. Đang chuyển bạn đến trang đăng nhập…')
      setTimeout(() => navigate('/login'), 1500)
    } catch (err) {
      setError(messageForError(err, 'Mã xác minh không hợp lệ hoặc đã hết hạn.'))
    } finally {
      setLoading(false)
    }
  }

  const handleResend = async () => {
    if (cooldown > 0 || loading) return
    setError('')
    setLoading(true)
    try {
      await resendRegisterOtp(formData.email.trim())
      setCooldown(RESEND_COOLDOWN_SECONDS)
    } catch (err) {
      setError(messageForError(err, 'Không thể gửi lại mã.'))
      if (err?.errorCode === 'REGISTER_OTP_COOLDOWN') setCooldown(RESEND_COOLDOWN_SECONDS)
    } finally {
      setLoading(false)
    }
  }

  const handleBackToEdit = () => {
    setStep('form')
    setError('')
    setOtp('')
  }

  return (
    <div className="flex min-h-screen">
      {/* Left: Register Form */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8 md:p-16">
        <div className="w-full max-w-md">
          <Link to="/" className="font-display-lg tracking-tighter text-primary no-underline lg:hidden">
            StyleMind
          </Link>

          {step === 'form' ? (
            <>
              <h2 className="font-headline-md text-primary mt-8 lg:mt-0">Tham gia StyleMind</h2>
              <p className="text-on-surface-variant mt-2">Bắt đầu hành trình phong cách được AI cá nhân hóa</p>

              <form onSubmit={handleStartRegistration} className="space-y-6 mt-8" noValidate>
                <div>
                  <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Email</label>
                  <div className="relative">
                    <Mail size={16} className="absolute left-0 top-1/2 -translate-y-1/2 text-on-surface-variant" />
                    <input
                      type="email"
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                      required
                      className="w-full bg-transparent border-0 border-b border-outline-variant py-2 pl-7 text-sm text-on-surface focus:border-tertiary-container focus:outline-none transition-colors"
                      placeholder="tenban@email.com"
                    />
                  </div>
                </div>

                <div>
                  <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Mật khẩu</label>
                  <div className="relative">
                    <Lock size={16} className="absolute left-0 top-1/2 -translate-y-1/2 text-on-surface-variant" />
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={formData.password}
                      onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                      required
                      minLength={6}
                      className="w-full bg-transparent border-0 border-b border-outline-variant py-2 pl-7 pr-10 text-sm text-on-surface focus:border-tertiary-container focus:outline-none transition-colors"
                      placeholder="Tạo mật khẩu"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-0 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-primary transition-colors"
                    >
                      {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </div>

                <div>
                  <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Xác nhận mật khẩu</label>
                  <div className="relative">
                    <Lock size={16} className="absolute left-0 top-1/2 -translate-y-1/2 text-on-surface-variant" />
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={formData.confirmPassword}
                      onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
                      required
                      minLength={6}
                      className="w-full bg-transparent border-0 border-b border-outline-variant py-2 pl-7 pr-10 text-sm text-on-surface focus:border-tertiary-container focus:outline-none transition-colors"
                      placeholder="Nhập lại mật khẩu"
                    />
                  </div>
                </div>

                {error && (
                  <div role="alert" className="rounded-lg border border-error/20 bg-error-container/40 px-4 py-3 text-sm text-error">
                    {error}
                  </div>
                )}

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full bg-primary text-on-primary rounded-lg py-3 text-sm font-medium hover:opacity-90 transition-opacity tracking-[0.1em] uppercase mt-8 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {loading ? 'Đang gửi mã...' : 'Tạo tài khoản'}
                </button>

                <div className="relative my-6">
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-outline-variant/20" />
                  </div>
                  <div className="relative flex justify-center text-xs">
                    <span className="bg-surface-container-lowest px-4 text-on-surface-variant uppercase tracking-wider">HOẶC</span>
                  </div>
                </div>

                <button
                  type="button"
                  className="w-full border border-outline-variant rounded-lg py-3 text-sm font-medium hover:bg-surface-container-high transition-colors flex items-center justify-center gap-3"
                >
                  <svg width="18" height="18" viewBox="0 0 24 24">
                    <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4"/>
                    <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                    <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                    <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
                  </svg>
                  Đăng ký với Google
                </button>
              </form>

              <p className="text-center text-sm text-on-surface-variant mt-8">
                Đã có tài khoản?{' '}
                <Link to="/login" className="text-primary font-medium hover:underline">
                  Đăng nhập
                </Link>
              </p>
            </>
          ) : (
            <>
              <h2 className="font-headline-md text-primary mt-8 lg:mt-0">Xác minh email</h2>
              <p className="text-on-surface-variant mt-2">
                Nhập mã 6 số chúng tôi đã gửi tới{' '}
                <span className="text-primary font-medium">{formData.email.trim()}</span>. Mã sẽ hết hạn sau ít phút.
              </p>

              {success ? (
                <div
                  role="status"
                  className="mt-8 flex items-center gap-3 rounded-lg border border-tertiary-container/40 bg-tertiary-container/20 px-4 py-4 text-sm text-primary"
                >
                  <CheckCircle2 size={18} />
                  {success}
                </div>
              ) : (
                <form onSubmit={handleVerifyOtp} className="space-y-6 mt-8">
                  <div>
                    <label htmlFor="register-otp" className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">
                      Mã xác minh
                    </label>
                    <input
                      id="register-otp"
                      inputMode="numeric"
                      autoComplete="one-time-code"
                      pattern="[0-9]{6}"
                      maxLength={6}
                      value={otp}
                      onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                      required
                      className="w-full bg-transparent border-0 border-b border-outline-variant py-3 text-center text-2xl text-primary tracking-[0.35em] focus:border-tertiary-container focus:outline-none"
                      placeholder="000000"
                    />
                  </div>

                  {error && (
                    <div role="alert" className="rounded-lg border border-error/20 bg-error-container/40 px-4 py-3 text-sm text-error">
                      {error}
                    </div>
                  )}

                  <button
                    type="submit"
                    disabled={loading || otp.length !== 6}
                    className="w-full bg-primary text-on-primary rounded-lg py-3 text-sm font-medium hover:opacity-90 transition-opacity tracking-[0.1em] uppercase mt-2 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {loading ? 'Đang xác minh...' : 'Xác minh và tạo tài khoản'}
                  </button>

                  <div className="flex items-center justify-between text-sm">
                    <button
                      type="button"
                      onClick={handleBackToEdit}
                      className="inline-flex items-center gap-2 text-on-surface-variant hover:text-primary transition-colors"
                    >
                      <ArrowLeft size={16} />
                      Sửa thông tin
                    </button>
                    <button
                      type="button"
                      onClick={handleResend}
                      disabled={cooldown > 0 || loading}
                      className="text-primary font-medium hover:underline disabled:opacity-50 disabled:cursor-not-allowed disabled:no-underline"
                    >
                      {cooldown > 0 ? `Gửi lại mã sau ${cooldown}s` : 'Gửi lại mã'}
                    </button>
                  </div>
                </form>
              )}
            </>
          )}
        </div>
      </div>

      {/* Right: Editorial Image */}
      <div className="hidden lg:flex lg:w-1/2 relative overflow-hidden">
            <img
          src="https://images.unsplash.com/photo-1509631179647-0177331693ae?w=1200&h=1600&fit=crop"
          alt="Ảnh biên tập thời trang"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-primary/80 via-primary/20 to-transparent" />
        <div className="absolute bottom-16 left-16 right-16">
          <blockquote className="font-headline-lg text-on-primary/90 italic leading-relaxed">
            "Phong cách là lớp áo giúp ta bước qua nhịp sống thường ngày."
          </blockquote>
          <p className="text-on-primary/60 mt-4 font-label-sm uppercase tracking-wider">- Bill Cunningham</p>
        </div>
      </div>
    </div>
  )
}
