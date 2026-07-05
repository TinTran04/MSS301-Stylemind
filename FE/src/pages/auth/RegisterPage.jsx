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

// Turn a backend error into a friendly English message, keyed by the stable
// errorCode so wording stays consistent regardless of the server locale.
function messageForError(err, fallback) {
  switch (err?.errorCode) {
    case 'EMAIL_ALREADY_EXISTS':
      return 'An account with this email already exists.'
    case 'REGISTER_OTP_INVALID':
      return 'That code is incorrect or has expired.'
    case 'REGISTER_OTP_BLOCKED':
      return 'Too many attempts. Please request a new code.'
    case 'REGISTER_OTP_COOLDOWN':
      return 'Please wait a moment before requesting another code.'
    case 'NOTIFICATION_FAILED':
      return "We couldn't send the verification email. Please try again."
    default:
      if (err && err.status === undefined) {
        return 'Network error. Check your connection and try again.'
      }
      return err?.message || fallback
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

  // Countdown that gates the "Resend code" button.
  useEffect(() => {
    if (cooldown <= 0) return undefined
    const timer = setInterval(() => setCooldown((s) => (s <= 1 ? 0 : s - 1)), 1000)
    return () => clearInterval(timer)
  }, [cooldown])

  const validateForm = () => {
    if (!formData.email.trim()) return 'Email is required.'
    if (!EMAIL_PATTERN.test(formData.email.trim())) return 'Enter a valid email address.'
    if (!formData.password) return 'Password is required.'
    if (formData.password.length < 6) return 'Password must be at least 6 characters.'
    if (!formData.confirmPassword) return 'Please confirm your password.'
    if (formData.password !== formData.confirmPassword) return 'Passwords do not match.'
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
      setError(messageForError(err, 'Unable to start registration.'))
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
      setSuccess('Your account is ready. Redirecting you to sign in…')
      setTimeout(() => navigate('/login'), 1500)
    } catch (err) {
      setError(messageForError(err, 'The verification code is invalid or expired.'))
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
      setError(messageForError(err, 'Unable to resend the code.'))
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
              <h2 className="font-headline-md text-primary mt-8 lg:mt-0">Join the Atelier</h2>
              <p className="text-on-surface-variant mt-2">Begin your AI-powered style journey</p>

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
                      placeholder="your@email.com"
                    />
                  </div>
                </div>

                <div>
                  <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Password</label>
                  <div className="relative">
                    <Lock size={16} className="absolute left-0 top-1/2 -translate-y-1/2 text-on-surface-variant" />
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={formData.password}
                      onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                      required
                      minLength={6}
                      className="w-full bg-transparent border-0 border-b border-outline-variant py-2 pl-7 pr-10 text-sm text-on-surface focus:border-tertiary-container focus:outline-none transition-colors"
                      placeholder="Create a password"
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
                  <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">Confirm Password</label>
                  <div className="relative">
                    <Lock size={16} className="absolute left-0 top-1/2 -translate-y-1/2 text-on-surface-variant" />
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={formData.confirmPassword}
                      onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
                      required
                      minLength={6}
                      className="w-full bg-transparent border-0 border-b border-outline-variant py-2 pl-7 pr-10 text-sm text-on-surface focus:border-tertiary-container focus:outline-none transition-colors"
                      placeholder="Re-enter your password"
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
                  {loading ? 'Sending Code...' : 'Create Account'}
                </button>

                <div className="relative my-6">
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-outline-variant/20" />
                  </div>
                  <div className="relative flex justify-center text-xs">
                    <span className="bg-surface-container-lowest px-4 text-on-surface-variant uppercase tracking-wider">OR</span>
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
                  Sign up with Google
                </button>
              </form>

              <p className="text-center text-sm text-on-surface-variant mt-8">
                Already have an account?{' '}
                <Link to="/login" className="text-primary font-medium hover:underline">
                  Sign in
                </Link>
              </p>
            </>
          ) : (
            <>
              <h2 className="font-headline-md text-primary mt-8 lg:mt-0">Verify your email</h2>
              <p className="text-on-surface-variant mt-2">
                Enter the six-digit code we sent to{' '}
                <span className="text-primary font-medium">{formData.email.trim()}</span>. It expires shortly.
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
                      Verification code
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
                    {loading ? 'Verifying...' : 'Verify & Create Account'}
                  </button>

                  <div className="flex items-center justify-between text-sm">
                    <button
                      type="button"
                      onClick={handleBackToEdit}
                      className="inline-flex items-center gap-2 text-on-surface-variant hover:text-primary transition-colors"
                    >
                      <ArrowLeft size={16} />
                      Edit details
                    </button>
                    <button
                      type="button"
                      onClick={handleResend}
                      disabled={cooldown > 0 || loading}
                      className="text-primary font-medium hover:underline disabled:opacity-50 disabled:cursor-not-allowed disabled:no-underline"
                    >
                      {cooldown > 0 ? `Resend code in ${cooldown}s` : 'Resend code'}
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
          alt="Fashion editorial"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-primary/80 via-primary/20 to-transparent" />
        <div className="absolute bottom-16 left-16 right-16">
          <blockquote className="font-headline-lg text-on-primary/90 italic leading-relaxed">
            "Fashion is the armour to survive the reality of everyday life."
          </blockquote>
          <p className="text-on-primary/60 mt-4 font-label-sm uppercase tracking-wider">- Bill Cunningham</p>
        </div>
      </div>
    </div>
  )
}
