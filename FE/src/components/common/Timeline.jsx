import { Check } from 'lucide-react'
import clsx from 'clsx'

export default function Timeline({ steps, currentStep }) {
  const currentIndex = steps.findIndex((s) => s.status === currentStep)

  return (
    <div className="flex items-center justify-between relative">
      <div className="absolute top-1/2 left-0 right-0 h-px bg-outline-variant/30 -translate-y-1/2" />
      {steps.map((step, idx) => {
        const isCompleted = idx < currentIndex
        const isCurrent = idx === currentIndex

        return (
          <div key={step.status} className="relative flex flex-col items-center z-10">
            <div
              className={clsx(
                'w-8 h-8 rounded-full flex items-center justify-center border-2 transition-all',
                isCompleted && 'bg-primary text-on-primary border-primary',
                isCurrent && 'border-primary bg-surface-container-lowest relative',
                !isCompleted && !isCurrent && 'border-outline-variant bg-surface-container-lowest'
              )}
            >
              {isCompleted && <Check size={14} />}
              {isCurrent && (
                <span className="absolute inset-0 rounded-full border-2 border-primary animate-ping opacity-20" />
              )}
            </div>
            <span className="mt-2 text-xs font-medium text-on-surface-variant whitespace-nowrap">
              {step.label}
            </span>
          </div>
        )
      })}
    </div>
  )
}
