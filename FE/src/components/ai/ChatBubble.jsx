import clsx from 'clsx'
import { motion } from 'framer-motion'
import { Sparkles } from 'lucide-react'
import { formatTimestamp } from '../../features/ai-stylist/aiStylist.utils'

export default function ChatBubble({ message, isAI = true }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
      className={clsx('flex gap-3', isAI ? 'justify-start' : 'justify-end')}
    >
      {isAI && (
        <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center shrink-0">
          <Sparkles size={14} className="text-on-primary" />
        </div>
      )}
      <div className={clsx('max-w-[70%]')}>
        <div
          className={clsx(
            'px-4 py-3 rounded-2xl text-sm leading-relaxed',
            isAI ? 'bg-surface-container-low text-on-surface' : 'bg-primary-container text-on-primary-container'
          )}
        >
          {message.content}
        </div>
        <p className={clsx('text-[10px] text-on-surface-variant mt-1', isAI ? 'text-left' : 'text-right')}>
          {formatTimestamp(message.timestamp)}
        </p>
      </div>
      {!isAI && (
        <div className="w-8 h-8 rounded-full bg-secondary-container flex items-center justify-center shrink-0">
          <span className="text-xs font-semibold text-on-secondary-container">You</span>
        </div>
      )}
    </motion.div>
  )
}
