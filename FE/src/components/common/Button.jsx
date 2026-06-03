import clsx from 'clsx'

const variants = {
  primary: 'bg-primary text-on-primary hover:opacity-90',
  secondary: 'border border-primary text-primary hover:bg-primary hover:text-on-primary',
  ghost: 'text-on-surface-variant hover:bg-surface-container-high',
  danger: 'bg-error text-on-primary hover:opacity-90',
  tertiary: 'text-tertiary hover:bg-tertiary-fixed/20',
}

const sizes = {
  sm: 'px-3 py-1.5 text-xs',
  md: 'px-5 py-2.5 text-sm',
  lg: 'px-8 py-3 text-base',
}

export default function Button({ variant = 'primary', size = 'md', className, children, ...props }) {
  return (
    <button
      className={clsx(
        'rounded-lg font-medium transition-all duration-200 active:scale-[0.98] inline-flex items-center justify-center gap-2',
        variants[variant],
        sizes[size],
        className
      )}
      {...props}
    >
      {children}
    </button>
  )
}
