import { TrendingUp, TrendingDown } from 'lucide-react'
import { motion } from 'framer-motion'
import clsx from 'clsx'

export default function MetricCard({ title, value, change, subtitle, icon: Icon, status }) {
  const isPositive = change >= 0

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow"
    >
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          {Icon && (
            <div className="w-10 h-10 rounded-full bg-secondary-container flex items-center justify-center">
              <Icon size={18} className="text-on-secondary-container" />
            </div>
          )}
          <div>
            <p className="font-label-sm uppercase text-on-surface-variant">{title}</p>
          </div>
        </div>
        {status && (
          <span className={clsx(
            'text-xs font-medium px-2 py-0.5 rounded-full',
            status === 'good' && 'bg-green-status/10 text-green-status',
            status === 'warning' && 'bg-tertiary-fixed/30 text-tertiary',
            status === 'error' && 'bg-error-container text-error'
          )}>
            {status}
          </span>
        )}
      </div>
      <p className="font-headline-md text-primary">{value}</p>
      {change !== undefined && (
        <div className={clsx('flex items-center gap-1 mt-1 text-xs font-medium', isPositive ? 'text-green-status' : 'text-error')}>
          {isPositive ? <TrendingUp size={12} /> : <TrendingDown size={12} />}
          {isPositive ? '+' : ''}{change}%
          {subtitle && <span className="text-on-surface-variant font-normal ml-1">{subtitle}</span>}
        </div>
      )}
    </motion.div>
  )
}
