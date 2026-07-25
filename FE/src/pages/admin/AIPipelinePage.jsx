import { useState, useEffect, useCallback } from 'react'
import { Brain, RefreshCw, AlertTriangle, CheckCircle, Clock, Activity, Plus, Database } from 'lucide-react'
import StatusBadge from '../../components/admin/StatusBadge'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog'
import { getIndexJobs, createIndexJob } from '../../features/ai-stylist/adminAiIndexJobs.api'
import { getAdminErrorMessage } from '../../features/admin/admin-error-messages'
import { formatRelativeTime, formatDateTime } from '../../utils/formatDate'

const TARGET_TYPES = ['PRODUCT', 'INVENTORY', 'RULE']
const OPERATION_TYPES = ['CREATE', 'UPDATE', 'DELETE']
const STATUS_FILTER_OPTIONS = [
  { value: '', label: 'Mọi trạng thái' },
  { value: 'PENDING', label: 'Đang chờ' },
  { value: 'PROCESSING', label: 'Đang xử lý' },
  { value: 'COMPLETED', label: 'Hoàn tất' },
  { value: 'FAILED', label: 'Thất bại' },
]
const TARGET_TYPE_LABELS = {
  PRODUCT: 'Sản phẩm',
  INVENTORY: 'Tồn kho',
  RULE: 'Quy tắc',
}
const OPERATION_TYPE_LABELS = {
  CREATE: 'Tạo',
  UPDATE: 'Cập nhật',
  DELETE: 'Xóa',
}

function IndexJobsPanel() {
  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState({ targetType: 'PRODUCT', targetId: '', operationType: 'UPDATE' })
  const [toast, setToast] = useState('')
  const [formError, setFormError] = useState(null)
  const [confirmOpen, setConfirmOpen] = useState(false)

  const fetchJobs = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const data = await getIndexJobs(statusFilter ? { status: statusFilter } : {})
      setJobs(data.content || data || [])
    } catch (err) {
      setError(err.message || 'Không thể tải danh sách công việc chỉ mục AI.')
    } finally {
      setLoading(false)
    }
  }, [statusFilter])

  useEffect(() => {
    fetchJobs()
  }, [fetchJobs])

  const handleCreateSubmit = (e) => {
    e.preventDefault()
    if (!form.targetId.trim()) return
    setFormError(null)
    setConfirmOpen(true)
  }

  const handleCreateConfirm = async () => {
    setConfirmOpen(false)
    setCreating(true)
    try {
      await createIndexJob(form)
      setToast('Đã tạo tác vụ chỉ mục AI')
      setTimeout(() => setToast(''), 3000)
      setForm({ targetType: 'PRODUCT', targetId: '', operationType: 'UPDATE' })
      setShowForm(false)
      fetchJobs()
    } catch (err) {
      const friendly = getAdminErrorMessage(err, {
        fallbackTitle: 'Không thể tạo tác vụ chỉ mục AI',
        fallbackMessage: 'Hệ thống chưa thể tạo tác vụ chỉ mục AI. Vui lòng thử lại sau.',
      })
      setFormError(friendly)
    } finally {
      setCreating(false)
    }
  }

  return (
    <div className="bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
      <div className="p-4 border-b border-outline-variant/20 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Database size={16} className="text-primary" />
          <h2 className="font-title-lg text-primary">Công việc chỉ mục AI</h2>
        </div>
        <div className="flex items-center gap-2">
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="bg-surface-container-low border border-outline-variant/20 rounded-lg px-2 py-1.5 text-xs text-on-surface focus:outline-none"
          >
            {STATUS_FILTER_OPTIONS.map((option) => (
              <option key={option.value || 'all'} value={option.value}>{option.label}</option>
            ))}
          </select>
          <button
            onClick={() => setShowForm((v) => !v)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-primary text-on-primary hover:opacity-90"
          >
            <Plus size={12} /> Tạo tác vụ mới
          </button>
        </div>
      </div>

      {toast && <div className="px-4 py-2 text-xs text-primary bg-primary/10">{toast}</div>}

      {showForm && (
        <form onSubmit={handleCreateSubmit} className="p-4 border-b border-outline-variant/20 flex flex-wrap items-end gap-3">
          {formError && (
            <div className="w-full rounded-lg border border-error/20 bg-error-container/40 px-3 py-2 text-xs text-error">
              <p className="font-medium">{formError.title}</p>
              <p className="mt-0.5">{formError.message}</p>
            </div>
          )}
          <div>
            <label className="block text-[10px] uppercase text-on-surface-variant mb-1">Loại mục tiêu</label>
            <select
              value={form.targetType}
              onChange={(e) => setForm((f) => ({ ...f, targetType: e.target.value }))}
              className="bg-surface-container-low border border-outline-variant/20 rounded-lg px-2 py-1.5 text-xs"
            >
              {TARGET_TYPES.map((t) => <option key={t} value={t}>{TARGET_TYPE_LABELS[t] || t}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-[10px] uppercase text-on-surface-variant mb-1">Mã mục tiêu</label>
            <input
              value={form.targetId}
              onChange={(e) => setForm((f) => ({ ...f, targetId: e.target.value }))}
              placeholder="VD: VDU-AO-002"
              className="bg-surface-container-low border border-outline-variant/20 rounded-lg px-2 py-1.5 text-xs"
            />
          </div>
          <div>
            <label className="block text-[10px] uppercase text-on-surface-variant mb-1">Thao tác</label>
            <select
              value={form.operationType}
              onChange={(e) => setForm((f) => ({ ...f, operationType: e.target.value }))}
              className="bg-surface-container-low border border-outline-variant/20 rounded-lg px-2 py-1.5 text-xs"
            >
              {OPERATION_TYPES.map((t) => <option key={t} value={t}>{OPERATION_TYPE_LABELS[t] || t}</option>)}
            </select>
          </div>
          <button
            type="submit"
            disabled={creating || !form.targetId.trim()}
            className="px-3 py-1.5 rounded-lg text-xs font-medium bg-primary text-on-primary hover:opacity-90 disabled:opacity-50"
          >
            {creating ? 'Đang tạo...' : 'Tạo'}
          </button>
        </form>
      )}

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="bg-surface-container-low/50">
              <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Mục tiêu</th>
              <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Thao tác</th>
              <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Trạng thái</th>
              <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Số lần thử lại</th>
              <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Đã tạo</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant/5">
            {loading ? (
              <tr><td colSpan={5} className="text-center py-8 text-sm text-on-surface-variant">Đang tải công việc chỉ mục AI...</td></tr>
            ) : error ? (
              <tr><td colSpan={5} className="text-center py-8 text-sm text-error">{error}</td></tr>
            ) : jobs.length === 0 ? (
              <tr><td colSpan={5} className="text-center py-8 text-sm text-on-surface-variant">Không tìm thấy công việc chỉ mục AI nào.</td></tr>
            ) : jobs.map((job) => (
              <tr key={job.id} className="hover:bg-surface-container-high/30">
                <td className="px-4 py-3 text-sm text-primary font-medium">{TARGET_TYPE_LABELS[job.targetType] || job.targetType} · {job.targetId}</td>
                <td className="px-4 py-3 text-xs text-on-surface-variant">{OPERATION_TYPE_LABELS[job.operationType] || job.operationType}</td>
                <td className="px-4 py-3"><StatusBadge status={job.status?.toLowerCase()} /></td>
                <td className="px-4 py-3 text-xs text-on-surface-variant">{job.retryCount ?? 0}</td>
                <td className="px-4 py-3 text-xs text-on-surface-variant">{formatDateTime(job.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <AdminConfirmDialog
        open={confirmOpen}
        title="Tạo công việc chỉ mục AI?"
        message="Hệ thống sẽ bắt đầu quá trình chỉ mục dữ liệu AI. Quá trình này có thể mất một lúc."
        confirmLabel="Tạo tác vụ"
        loading={creating}
        onConfirm={handleCreateConfirm}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  )
}

export default function AIPipelinePage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-headline-md text-primary">Quy trình AI</h1>
          <p className="text-sm text-on-surface-variant mt-1">
            Quản lý các công việc chỉ mục (Index Jobs) và đồng bộ Vector DB cho AI Stylist
          </p>
        </div>
      </div>

      {/* Primary Real API Feature: Index Jobs Management */}
      <IndexJobsPanel />

      {/* Placeholder section for detailed event telemetry */}
      <div className="bg-surface-container-lowest rounded-2xl p-8 border border-outline-variant/30 text-center space-y-4 ambient-shadow">
        <div className="w-14 h-14 rounded-2xl bg-surface-container-high flex items-center justify-center mx-auto text-primary">
          <Activity size={28} />
        </div>
        <div className="max-w-md mx-auto space-y-2">
          <h2 className="font-title-lg text-primary font-semibold">
            Tính năng này sẽ được hoàn thiện trong tương lai
          </h2>
          <p className="text-sm text-on-surface-variant leading-relaxed">
            Nhật ký sự kiện thời gian thực (Real-time Event Telemetry &amp; Vector Index Latency Metrics) sẽ được kết nối khi dịch vụ AI Event Streaming hoàn tất tích hợp.
          </p>
        </div>
      </div>
    </div>
  )
}
