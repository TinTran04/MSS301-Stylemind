import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { verifyResetOtp } from '../../features/auth/auth.api'
import {
  getPasswordResetContext,
  setPasswordResetContext,
} from '../../features/auth/passwordResetSession'
import PasswordRecoveryShell from './PasswordRecoveryShell'

export default function VerifyResetOtpPage() {
  const [resetContext] = useState(getPasswordResetContext)
  const [otp, setOtp] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  if (!resetContext?.email) {
    return (
      <PasswordRecoveryShell
        eyebrow="Verification"
        title="Start a new recovery request"
        description="No active password recovery request was found in this tab."
      >
        <Link
          to="/forgot-password"
          className="flex w-full items-center justify-center bg-primary text-on-primary rounded-lg py-3 text-sm font-medium uppercase"
        >
          Request a reset code
        </Link>
      </PasswordRecoveryShell>
    )
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setLoading(true)

    try {
      const response = await verifyResetOtp(resetContext.email, otp)
      setPasswordResetContext({
        email: resetContext.email,
        resetToken: response.resetToken,
      })
      navigate('/reset-password')
    } catch (requestError) {
      setError(requestError.message || 'The verification code is invalid or expired.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <PasswordRecoveryShell
      eyebrow="Verification"
      title="Enter your reset code"
      description="Use the six-digit code sent to your email. It expires shortly."
    >
      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label htmlFor="reset-otp" className="block font-label-sm uppercase text-on-surface-variant mb-2">
            Verification code
          </label>
          <input
            id="reset-otp"
            inputMode="numeric"
            autoComplete="one-time-code"
            pattern="[0-9]{6}"
            maxLength={6}
            value={otp}
            onChange={(event) => setOtp(event.target.value.replace(/\D/g, '').slice(0, 6))}
            required
            className="w-full bg-transparent border-0 border-b border-outline-variant py-3 text-center text-2xl text-primary tracking-[0.35em] focus:border-tertiary-container focus:outline-none"
            placeholder="000000"
          />
        </div>

        {error && <p role="alert" className="text-sm text-error">{error}</p>}

        <button
          type="submit"
          disabled={loading || otp.length !== 6}
          className="w-full bg-primary text-on-primary rounded-lg py-3 text-sm font-medium uppercase disabled:opacity-50"
        >
          {loading ? 'Verifying...' : 'Verify code'}
        </button>
      </form>

      <Link to="/forgot-password" className="inline-flex items-center gap-2 text-sm text-on-surface-variant mt-8 hover:text-primary">
        <ArrowLeft size={16} />
        Request another code
      </Link>
    </PasswordRecoveryShell>
  )
}
