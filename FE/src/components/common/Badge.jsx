import clsx from 'clsx'

const variants = {
  default: 'bg-surface-container-high text-on-surface-variant',
  primary: 'bg-primary text-on-primary',
  secondary: 'bg-secondary-container text-on-secondary-container',
  success: 'bg-green-status/10 text-green-status',
  warning: 'bg-tertiary-fixed/30 text-tertiary',
  error: 'bg-error-container text-error',
  ai: 'bg-ai-lavender text-ai-indigo animate-pulse-glow',
}

export default function Badge({ variant = 'default', className, children, ...props }) {
  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold',
        variants[variant],
        className
      )}
      {...props}
    >
      {children}
    </span>
  )
}
