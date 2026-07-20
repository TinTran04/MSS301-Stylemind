import { useState, useEffect, useCallback } from 'react'
import { Brain, RefreshCw, AlertTriangle, CheckCircle, Clock, Activity, Plus, Database } from 'lucide-react'
import StatusBadge from '../../components/admin/StatusBadge'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog'
import { getAIPipelineEvents } from '../../features/analytics/analytics.api'
import { getIndexJobs, createIndexJob } from '../../features/ai-stylist/adminAiIndexJobs.api'
import { getAdminErrorMessage } from '../../features/admin/admin-error-messages'
import { formatRelativeTime, formatDateTime } from '../../utils/formatDate'

const TARGET_TYPES = ['PRODUCT', 'INVENTORY', 'RULE']
const OPERATION_TYPES = ['CREATE', 'UPDATE', 'DELETE']
const STATUS_FILTER_OPTIONS = [
  { value: '', label: 'Mß╗ìi trß║íng th├íi' },
  { value: 'PENDING', label: '─Éang chß╗¥' },
  { value: 'PROCESSING', label: '─Éang xß╗¡ l├╜' },
  { value: 'COMPLETED', label: 'Ho├án tß║Ñt' },
  { value: 'FAILED', label: 'Thß║Ñt bß║íi' },
]
const TARGET_TYPE_LABELS = {
  PRODUCT: 'Sß║ún phß║⌐m',
  INVENTORY: 'Tß╗ôn kho',
  RULE: 'Quy tß║»c',
}
const OPERATION_TYPE_LABELS = {
  CREATE: 'Tß║ío',
  UPDATE: 'Cß║¡p nhß║¡t',
  DELETE: 'X├│a',
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
      setError(err.message || 'Kh├┤ng thß╗â tß║úi danh s├ích c├┤ng viß╗çc chß╗ë mß╗Ñc AI.')
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
      setToast('─É├ú tß║ío t├íc vß╗Ñ chß╗ë mß╗Ñc AI')
      setTimeout(() => setToast(''), 3000)
      setForm({ targetType: 'PRODUCT', targetId: '', operationType: 'UPDATE' })
      setShowForm(false)
      fetchJobs()
    } catch (err) {
      const friendly = getAdminErrorMessage(err, {
        fallbackTitle: 'Kh├┤ng thß╗â tß║ío t├íc vß╗Ñ chß╗ë mß╗Ñc AI',
        fallbackMessage: 'Hß╗ç thß╗æng ch╞░a thß╗â tß║ío t├íc vß╗Ñ chß╗ë mß╗Ñc AI. Vui l├▓ng thß╗¡ lß║íi sau.',
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
          <h2 className="font-title-lg text-primary">C├┤ng viß╗çc chß╗ë mß╗Ñc AI</h2>
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
            <Plus size={12} /> Tß║ío t├íc vß╗Ñ mß╗¢i
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
            <label className="block text-[10px] uppercase text-on-surface-variant mb-1">Loß║íi mß╗Ñc ti├¬u</label>
            <select
              value={form.targetType}
              onChange={(e) => setForm((f) => ({ ...f, targetType: e.target.value }))}
              className="bg-surface-container-low border border-outline-variant/20 rounded-lg px-2 py-1.5 text-xs"
            >
              {TARGET_TYPES.map((t) => <option key={t} value={t}>{TARGET_TYPE_LABELS[t] || t}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-[10px] uppercase text-on-surface-variant mb-1">M├ú mß╗Ñc ti├¬u</label>
            <input
              value={form.targetId}
              onChange={(e) => setForm((f) => ({ ...f, targetId: e.target.value }))}
              placeholder="VD: VDU-AO-002"
              className="bg-surface-container-low border border-outline-variant/20 rounded-lg px-2 py-1.5 text-xs"
            />
          </div>
          <div>
            <label className="block text-[10px] uppercase text-on-surface-variant mb-1">Thao t├íc</label>
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
            {creating ? '─Éang tß║ío...' : 'Tß║ío'}
          </button>
        </form>
      )}

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="bg-surface-container-low/50">
              <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Mß╗Ñc ti├¬u</th>
              <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Thao t├íc</th>
              <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Trß║íng th├íi</th>
              <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Sß╗æ lß║ºn thß╗¡ lß║íi</th>
              <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">─É├ú tß║ío</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-outline-variant/5">
            {loading ? (
              <tr><td colSpan={5} className="text-center py-8 text-sm text-on-surface-variant">─Éang tß║úi c├┤ng viß╗çc chß╗ë mß╗Ñc AI...</td></tr>
            ) : error ? (
              <tr><td colSpan={5} className="text-center py-8 text-sm text-error">{error}</td></tr>
            ) : jobs.length === 0 ? (
              <tr><td colSpan={5} className="text-center py-8 text-sm text-on-surface-variant">Kh├┤ng t├¼m thß║Ñy c├┤ng viß╗çc chß╗ë mß╗Ñc AI n├áo.</td></tr>
            ) : jobs.map((job) => (
              <tr key={job.id} className="hover:bg-surface-container-high/30">
                <td className="px-4 py-3 text-sm text-primary font-medium">{TARGET_TYPE_LABELS[job.targetType] || job.targetType} ┬╖ {job.targetId}</td>
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
        title="Tß║ío c├┤ng viß╗çc chß╗ë mß╗Ñc AI?"
        message="Hß╗ç thß╗æng sß║╜ bß║»t ─æß║ºu qu├í tr├¼nh chß╗ë mß╗Ñc dß╗» liß╗çu AI. Qu├í tr├¼nh n├áy c├│ thß╗â mß║Ñt mß╗Öt l├║c."
        confirmLabel="Tß║ío t├íc vß╗Ñ"
        loading={creating}
        onConfirm={handleCreateConfirm}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  )
}

export default function AIPipelinePage() {
  const [events, setEvents] = useState([])
  const [selectedEvent, setSelectedEvent] = useState(null)
  const [retryingId, setRetryingId] = useState(null)

  useEffect(() => {
    getAIPipelineEvents().then(setEvents)
  }, [])

  const retryEvent = (eventId) => {
    setRetryingId(eventId)
    setEvents(prev => prev.map(e => e.id === eventId ? { ...e, status: 'processing' } : e))
    setSelectedEvent(prev => prev && prev.id === eventId ? { ...prev, status: 'processing' } : prev)
    setTimeout(() => {
      setEvents(prev => prev.map(e => e.id === eventId ? { ...e, status: 'synced' } : e))
      setSelectedEvent(prev => prev && prev.id === eventId ? { ...prev, status: 'synced' } : prev)
      setRetryingId(null)
    }, 2000)
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-headline-md text-primary">Quy tr├¼nh AI</h1>
          <p className="text-sm text-on-surface-variant mt-1">Theo d├╡i t├¼nh trß║íng dß╗ïch vß╗Ñ AI v├á luß╗ông sß╗▒ kiß╗çn</p>
        </div>
        <button className="bg-surface-container text-on-surface px-4 py-2 rounded-lg text-sm font-medium flex items-center gap-2 hover:bg-surface-container-high">
          <RefreshCw size={14} /> L├ám mß╗¢i
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-surface-container-lowest rounded-xl p-5 ambient-shadow">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2"><Activity size={16} className="text-primary" /><span className="text-sm font-medium text-primary">Chß╗ë mß╗Ñc vector</span></div>
            <StatusBadge status="synced" />
          </div>
          <p className="text-2xl font-semibold text-primary">99.98%</p>
          <p className="text-xs text-on-surface-variant mt-1">─Éiß╗âm sß╗⌐c khß╗Åe</p>
        </div>
        <div className="bg-surface-container-lowest rounded-xl p-5 ambient-shadow">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2"><Brain size={16} className="text-primary" /><span className="text-sm font-medium text-primary">─Éß╗ô thß╗ï tri thß╗⌐c</span></div>
            <StatusBadge status="synced" />
          </div>
          <p className="text-2xl font-semibold text-primary">100%</p>
          <p className="text-xs text-on-surface-variant mt-1">─Éiß╗âm to├án vß║╣n</p>
        </div>
        <div className="bg-surface-container-lowest rounded-xl p-5 ambient-shadow">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2"><Clock size={16} className="text-primary" /><span className="text-sm font-medium text-primary">─Éß╗Ö trß╗à quy tr├¼nh</span></div>
            <StatusBadge status="failed" />
          </div>
          <p className="text-2xl font-semibold text-error">420ms</p>
          <p className="text-xs text-error mt-1">V╞░ß╗út ng╞░ß╗íng (200ms)</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-8 bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
          <div className="p-4 border-b border-outline-variant/20">
            <h2 className="font-title-lg text-primary">Nhß║¡t k├╜ sß╗▒ kiß╗çn</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-surface-container-low/50">
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Thß╗¥i gian</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Sß╗▒ kiß╗çn</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Dß╗ïch vß╗Ñ</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Trß║íng th├íi</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/5">
                {events.map((event) => (
                  <tr key={event.id} className={`hover:bg-surface-container-high/30 cursor-pointer transition-colors ${selectedEvent?.id === event.id ? 'bg-surface-container-low' : ''}`} onClick={() => setSelectedEvent(event)}>
                    <td className="px-4 py-3 text-xs text-on-surface-variant">{formatRelativeTime(event.timestamp)}</td>
                    <td className="px-4 py-3 text-sm font-medium text-primary">{event.name}</td>
                    <td className="px-4 py-3 text-sm text-on-surface-variant">{event.service}</td>
                    <td className="px-4 py-3"><StatusBadge status={event.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="lg:col-span-4">
          <div className="bg-surface-container-lowest rounded-xl p-5 ambient-shadow sticky top-24">
            {selectedEvent ? (
              <div className="space-y-4">
                <div className="flex items-center gap-2">
                  {selectedEvent.status === 'failed' ? <AlertTriangle size={18} className="text-error" /> : <CheckCircle size={18} className="text-green-status" />}
                  <h3 className="font-title-lg text-primary">{selectedEvent.name}</h3>
                </div>
                <StatusBadge status={selectedEvent.status} />
                <div className="space-y-2 text-sm">
                  <div className="flex justify-between"><span className="text-on-surface-variant">Dß╗ïch vß╗Ñ</span><span className="text-primary">{selectedEvent.service}</span></div>
                  <div className="flex justify-between"><span className="text-on-surface-variant">Thß╗¥i gian</span><span className="text-primary">{formatDateTime(selectedEvent.timestamp)}</span></div>
                </div>
                {selectedEvent.status === 'failed' && (
                  <div className="bg-error-container/50 rounded-lg p-4">
                    <h4 className="text-xs font-medium text-error mb-2">Chi tiß║┐t lß╗ùi</h4>
                    <p className="text-xs text-on-surface-variant">Hß║┐t thß╗¥i gian kß║┐t nß╗æi: dß╗ïch vß╗Ñ kh├┤ng khß║ú dß╗Ñng. Sß║╜ thß╗¡ lß║íi sau 30 gi├óy.</p>
                  </div>
                )}
                {selectedEvent.status === 'processing' && (
                  <div className="bg-primary/10 rounded-lg p-4">
                    <h4 className="text-xs font-medium text-primary mb-2">─Éang thß╗¡ lß║íi</h4>
                    <div className="flex items-center gap-2">
                      <div className="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                      <p className="text-xs text-on-surface-variant">─Éang ─æß╗ông bß╗Ö lß║íi sß╗▒ kiß╗çn...</p>
                    </div>
                  </div>
                )}
                <div className="bg-surface-container-low rounded-lg p-4">
                  <h4 className="text-xs font-medium text-on-surface-variant mb-2">Xem tr╞░ß╗¢c payload</h4>
                  <pre className="text-[10px] text-on-surface-variant overflow-x-auto">{'{' + `\n  "event": "${selectedEvent.name}",\n  "service": "${selectedEvent.service}",\n  "status": "${selectedEvent.status}"` + '\n}'}</pre>
                </div>
                <div className="flex gap-2">
                  {selectedEvent.status === 'failed' && (
                    <button onClick={() => retryEvent(selectedEvent.id)} disabled={retryingId === selectedEvent.id} className="flex-1 bg-primary text-on-primary rounded-lg py-2 text-xs font-medium hover:opacity-90 disabled:opacity-50">
                      {retryingId === selectedEvent.id ? '─Éang thß╗¡ lß║íi...' : 'Thß╗¡ lß║íi'}
                    </button>
                  )}
                  <button className="flex-1 bg-surface-container text-on-surface rounded-lg py-2 text-xs font-medium hover:bg-surface-container-high">Xem nhß║¡t k├╜</button>
                </div>
              </div>
            ) : (
              <div className="text-center py-8 text-on-surface-variant text-sm">Chß╗ìn mß╗Öt sß╗▒ kiß╗çn ─æß╗â xem chi tiß║┐t</div>
            )}
          </div>
        </div>
      </div>

      <div className="bg-surface-container-lowest rounded-xl p-6 ambient-shadow">
        <h2 className="font-title-lg text-primary mb-4">T├¼nh trß║íng quy tr├¼nh</h2>
        <div className="flex items-center justify-between">
          {['Tiß║┐p nhß║¡n', 'Ph├ón t├¡ch', 'Chß╗ë mß╗Ñc', 'Nh├║ng', 'L╞░u trß╗»'].map((step, idx) => (
            <div key={step} className="flex items-center">
              <div className="flex flex-col items-center">
                <div className={`w-10 h-10 rounded-full flex items-center justify-center text-xs font-bold ${
                  idx < 4 ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant'
                }`}>{idx + 1}</div>
                <span className="text-xs text-on-surface-variant mt-2">{step}</span>
              </div>
              {idx < 4 && <div className={`w-16 h-0.5 mx-2 ${idx < 3 ? 'bg-primary' : 'bg-outline-variant'}`} />}
            </div>
          ))}
        </div>
      </div>

      <IndexJobsPanel />
    </div>
  )
}