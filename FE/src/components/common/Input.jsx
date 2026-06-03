import clsx from 'clsx'
import { forwardRef } from 'react'

const Input = forwardRef(function Input({ label, error, className, type = 'text', ...props }, ref) {
  return (
    <div className="w-full">
      {label && (
        <label className="block font-label-sm uppercase tracking-wider text-on-surface-variant mb-2">
          {label}
        </label>
      )}
      <input
        ref={ref}
        type={type}
        className={clsx(
          'w-full bg-transparent border-0 border-b border-outline-variant py-2 text-sm text-on-surface',
          'focus:border-tertiary-container focus:outline-none transition-colors duration-300',
          error && 'border-error',
          className
        )}
        {...props}
      />
      {error && <p className="mt-1 text-xs text-error">{error}</p>}
    </div>
  )
})

export default Input
