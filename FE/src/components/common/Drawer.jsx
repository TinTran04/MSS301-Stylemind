import { X } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import clsx from 'clsx'

export default function Drawer({ isOpen, onClose, title, children, side = 'right' }) {
  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50">
          <motion.div 
            className="absolute inset-0 bg-primary/40 backdrop-blur-sm"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />
          <motion.div
            initial={{ x: side === 'right' ? '100%' : '-100%' }}
            animate={{ x: 0 }}
            exit={{ x: side === 'right' ? '100%' : '-100%' }}
            transition={{ type: 'spring', damping: 25, stiffness: 200 }}
            className={clsx(
              'absolute top-0 h-full w-full max-w-md bg-surface-container-lowest shadow-xl overflow-y-auto',
              side === 'right' ? 'right-0' : 'left-0'
            )}
          >
            <div className="sticky top-0 z-10 flex items-center justify-between p-6 border-b border-outline-variant/20 bg-surface-container-lowest">
              <h2 className="font-title-lg text-primary">{title}</h2>
              <button onClick={onClose} className="p-2 rounded-full hover:bg-surface-container-high transition-colors">
                <X size={18} />
              </button>
            </div>
            <div className="p-6">{children}</div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}
