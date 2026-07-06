import Modal from '../common/Modal'

// Shared project-styled confirmation dialog for Admin Dashboard destructive/impactful
// actions. Replaces window.confirm across admin pages.
export default function AdminConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Xác nhận',
  cancelLabel = 'Hủy',
  destructive = false,
  loading = false,
  onConfirm,
  onCancel,
}) {
  return (
    <Modal isOpen={open} onClose={onCancel} title={title}>
      <p className="text-sm text-on-surface-variant mb-6">{message}</p>
      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={onCancel}
          disabled={loading}
          className="px-4 py-2 rounded-lg text-sm font-medium text-on-surface-variant hover:bg-surface-container-high transition-colors disabled:opacity-40"
        >
          {cancelLabel}
        </button>
        <button
          type="button"
          onClick={onConfirm}
          disabled={loading}
          className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-40 ${
            destructive ? 'bg-error text-white hover:opacity-90' : 'bg-primary text-on-primary hover:opacity-90'
          }`}
        >
          {loading ? 'Đang xử lý…' : confirmLabel}
        </button>
      </div>
    </Modal>
  )
}
