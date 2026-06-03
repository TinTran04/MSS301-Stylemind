import clsx from 'clsx'

export default function Card({ className, children, hover = false, ...props }) {
  return (
    <div
      className={clsx(
        'bg-surface-container-lowest rounded-xl',
        hover && 'product-card-shadow hover:soft-shadow-hover hover:-translate-y-1 transition-all duration-300',
        className
      )}
      {...props}
    >
      {children}
    </div>
  )
}
