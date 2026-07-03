import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Eye, EyeOff, Lock } from 'lucide-react'
import { resetPassword } from '../../features/auth/auth.api'
import {
  clearPasswordResetContext,
  getPasswordResetContext,
} from '../../features/auth/passwordResetSession'
import PasswordRecoveryShell from './PasswordRecoveryShell'

export default function ResetPasswordPage() {
  const [resetContext] = useState(getPasswordResetContext)
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [complete, setComplete] = useState(false)

  if (!resetContext?.email || !resetContext?.resetToken) {
    return (
      <PasswordRecoveryShell
        eyebrow="New password"
        title="Verification required"
        description="Verify a current reset code before choosing a new password."
      >
        <Link
          to="/forgot-password"
          className="flex w-full items-center justify-center bg-primary text-on-primary rounded-lg py-3 text-sm font-medium uppercase"
        >
          Start account recovery
        </Link>
      </PasswordRecoveryShell>
    )
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    setLoading(true)
    try {
      await resetPassword(resetContext.email, resetContext.resetToken, newPassword)
      clearPasswordResetContext()
      setComplete(true)
    } catch (requestError) {
      setError(requestError.message || 'Unable to reset your password.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <PasswordRecoveryShell
      eyebrow="New password"
      title={complete ? 'Password updated' : 'Choose a new password'}
      description={complete
        ? 'Your new password is ready to use.'
        : 'Use at least six characters and keep it unique to StyleMind.'}
    >
      {complete ? (
        <div role="status" className="space-y-6">
          <p className="text-sm text-on-surface-variant">
            You can now sign in with your new password.
          </p>
          <Link
            to="/login"
            className="flex w-full items-center justify-center bg-primary text-on-primary rounded-lg py-3 text-sm font-medium uppercase"
          >
            Return to sign in
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-6">
          <PasswordField
            id="new-password"
            label="New password"
            value={newPassword}
            onChange={setNewPassword}
            visible={showPassword}
            onToggle={() => setShowPassword((current) => !current)}
          />
          <PasswordField
            id="confirm-password"
            label="Confirm password"
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
            {loading ? 'Updating password...' : 'Update password'}
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
            aria-label={visible ? 'Hide password' : 'Show password'}
            className="absolute right-0 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-primary"
          >
            {visible ? <EyeOff size={16} /> : <Eye size={16} />}
          </button>
        )}
      </div>
    </div>
  )
}
