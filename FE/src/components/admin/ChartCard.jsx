import clsx from 'clsx'

export default function ChartCard({ title, children, className, action }) {
  return (
    <div className={clsx('bg-surface-container-lowest rounded-xl p-6 ambient-shadow', className)}>
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-title-lg text-primary">{title}</h3>
        {action}
      </div>
      {children}
    </div>
  )
}
