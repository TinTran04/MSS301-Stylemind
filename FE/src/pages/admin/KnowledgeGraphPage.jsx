import { useState, useEffect, useCallback } from 'react'
import {
  Network,
  FileText,
  Plus,
  Loader2,
  CheckCircle,
  Trash2,
  ExternalLink,
  AlertTriangle,
} from 'lucide-react'
import StatusBadge from '../../components/admin/StatusBadge'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog'
import KnowledgeSourceReview from '../../components/admin/KnowledgeSourceReview'
import KnowledgeGraphExplorer from '../../components/admin/KnowledgeGraphExplorer'
import useAuthStore from '../../features/auth/auth.store'
import {
  ingestKnowledge,
  listKnowledgeSources,
  getKnowledgeSource,
  approveKnowledgeSource,
  deleteKnowledgeSource,
  getKnowledgeGraphOverview,
} from '../../features/ai-stylist/adminKnowledge.api'
import { formatDateTime } from '../../utils/formatDate'

const TABS = [
  { id: 'sources', label: 'Nguồn tri thức', icon: FileText },
  { id: 'graph', label: 'Đồ thị tri thức', icon: Network },
]

function IngestForm({ userId, onIngested }) {
  const [title, setTitle] = useState('')
  const [urlsText, setUrlsText] = useState('')
  const [rawText, setRawText] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const urls = urlsText.split('\n').map((u) => u.trim()).filter(Boolean)
  const texts = rawText.trim() ? [rawText.trim()] : []
  const canSubmit = !submitting && (urls.length > 0 || texts.length > 0)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!canSubmit) return
    setSubmitting(true)
    setError('')
    try {
      const result = await ingestKnowledge({ userId, title: title.trim(), texts, urls })
      setTitle('')
      setUrlsText('')
      setRawText('')
      onIngested(result)
    } catch (err) {
      setError(err.message || 'Không thể trích xuất tri thức. Vui lòng thử lại.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="bg-surface-container-lowest rounded-xl ambient-shadow p-5 space-y-3">
      <div className="flex items-center gap-2">
        <Plus size={15} className="text-primary" />
        <h2 className="font-title-lg text-primary">Nạp nguồn tri thức mới</h2>
      </div>
      <p className="text-xs text-on-surface-variant">
        Dán URL bài viết thời trang (mỗi dòng một URL) hoặc nội dung văn bản. Hệ thống sẽ trích xuất
        khái niệm, quan hệ và quy tắc để bạn duyệt trước khi ghi vào đồ thị tri thức.
      </p>

      {error && (
        <div role="alert" className="rounded-lg border border-error/20 bg-error-container/40 px-3 py-2 text-xs text-error flex items-center gap-2">
          <AlertTriangle size={13} className="shrink-0" /> {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        <div className="space-y-3">
          <div>
            <label className="block text-[10px] uppercase text-on-surface-variant mb-1">Tiêu đề (tùy chọn)</label>
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="VD: Cẩm nang phối đồ mùa hè"
              maxLength={255}
              className="w-full bg-surface-container-low border border-outline-variant/20 rounded-lg px-3 py-2 text-sm focus:outline-none"
            />
          </div>
          <div>
            <label className="block text-[10px] uppercase text-on-surface-variant mb-1">URL bài viết (mỗi dòng một URL)</label>
            <textarea
              value={urlsText}
              onChange={(e) => setUrlsText(e.target.value)}
              rows={3}
              placeholder={'https://blog.example.com/phoi-do-cong-so'}
              className="w-full bg-surface-container-low border border-outline-variant/20 rounded-lg px-3 py-2 text-sm font-mono focus:outline-none resize-y"
            />
          </div>
        </div>
        <div>
          <label className="block text-[10px] uppercase text-on-surface-variant mb-1">Hoặc dán nội dung văn bản</label>
          <textarea
            value={rawText}
            onChange={(e) => setRawText(e.target.value)}
            rows={7}
            placeholder="Dán nội dung kiến thức thời trang tại đây..."
            className="w-full bg-surface-container-low border border-outline-variant/20 rounded-lg px-3 py-2 text-sm focus:outline-none resize-y"
          />
        </div>
      </div>

      <div className="flex items-center gap-3">
        <button
          type="submit"
          disabled={!canSubmit}
          className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium bg-primary text-on-primary hover:opacity-90 disabled:opacity-50"
        >
          {submitting ? <Loader2 size={14} className="animate-spin" /> : <Plus size={14} />}
          {submitting ? 'Đang trích xuất...' : 'Trích xuất tri thức'}
        </button>
        {submitting && (
          <p className="text-xs text-on-surface-variant">
            Đang đọc nguồn và trích xuất bằng AI — có thể mất 1–3 phút.
          </p>
        )}
      </div>
    </form>
  )
}

function SourceDetailPanel({ detail, loading, onApprove, onDelete, approving }) {
  if (loading) {
    return (
      <div className="bg-surface-container-lowest rounded-xl ambient-shadow p-8 text-center text-sm text-on-surface-variant">
        Đang tải chi tiết nguồn tri thức...
      </div>
    )
  }
  if (!detail) return null

  const isPending = detail.status === 'pending'

  return (
    <div className="bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
      <div className="p-5 border-b border-outline-variant/20">
        <div className="flex items-start justify-between gap-4 flex-wrap">
          <div className="min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <h2 className="font-title-lg text-primary">{detail.title || 'Nguồn không tiêu đề'}</h2>
              <StatusBadge status={detail.status} />
            </div>
            <p className="text-xs text-on-surface-variant mt-1">
              Tạo lúc {formatDateTime(detail.created_at)}
              {detail.approved_at ? ` · Duyệt lúc ${formatDateTime(detail.approved_at)}` : ''}
            </p>
            {detail.sources?.length > 0 && (
              <div className="mt-2 space-y-0.5">
                {detail.sources.map((src) => (
                  <p key={src} className="text-xs text-on-surface-variant flex items-center gap-1.5 min-w-0">
                    <ExternalLink size={10} className="shrink-0" />
                    {src.startsWith('http') ? (
                      <a href={src} target="_blank" rel="noreferrer" className="truncate hover:text-primary underline-offset-2 hover:underline">
                        {src}
                      </a>
                    ) : (
                      <span className="truncate">Văn bản dán trực tiếp ({src})</span>
                    )}
                  </p>
                ))}
              </div>
            )}
          </div>
          <div className="flex items-center gap-2 shrink-0">
            {isPending && (
              <button
                onClick={onApprove}
                disabled={approving}
                className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs font-medium bg-primary text-on-primary hover:opacity-90 disabled:opacity-50"
              >
                {approving ? <Loader2 size={12} className="animate-spin" /> : <CheckCircle size={12} />}
                {approving ? 'Đang ghi vào đồ thị...' : 'Duyệt & ghi vào đồ thị'}
              </button>
            )}
            <button
              onClick={onDelete}
              className="flex items-center gap-1.5 px-3 py-2 rounded-lg text-xs font-medium bg-surface-container text-error hover:bg-error-container/40"
            >
              <Trash2 size={12} /> Xóa
            </button>
          </div>
        </div>
        {isPending && (
          <p className="mt-3 text-xs text-on-surface-variant bg-surface-container-low rounded-lg px-3 py-2">
            Hãy rà soát các khái niệm, quan hệ và quy tắc bên dưới. Nếu chính xác, bấm
            <span className="font-medium text-primary"> "Duyệt & ghi vào đồ thị"</span> — tri thức sẽ được
            ghi vào Neo4j và đánh chỉ mục vector. Nếu sai, hãy xóa và nạp lại nguồn khác.
          </p>
        )}
      </div>
      <div className="p-5">
        <KnowledgeSourceReview concepts={detail.concepts} edges={detail.edges} rules={detail.rules} />
      </div>
    </div>
  )
}

export default function KnowledgeGraphPage() {
  const user = useAuthStore((s) => s.user)
  const [tab, setTab] = useState('sources')

  // Sources state
  const [sourcesList, setSourcesList] = useState([])
  const [loadingSources, setLoadingSources] = useState(true)
  const [listError, setListError] = useState('')
  const [selectedId, setSelectedId] = useState(null)
  const [detail, setDetail] = useState(null)
  const [loadingDetail, setLoadingDetail] = useState(false)
  const [toast, setToast] = useState('')
  const [approving, setApproving] = useState(false)
  const [confirm, setConfirm] = useState(null) // { type: 'approve' | 'delete', sourceId }

  // Graph state (loaded lazily when the tab is first opened)
  const [graph, setGraph] = useState(null)
  const [graphLoading, setGraphLoading] = useState(false)
  const [graphError, setGraphError] = useState('')

  const showToast = (msg) => {
    setToast(msg)
    setTimeout(() => setToast(''), 5000)
  }

  const fetchSources = useCallback(async () => {
    if (!user?.id) return
    setLoadingSources(true)
    setListError('')
    try {
      setSourcesList(await listKnowledgeSources(user.id))
    } catch (err) {
      setListError(err.message || 'Không thể tải danh sách nguồn tri thức.')
    } finally {
      setLoadingSources(false)
    }
  }, [user?.id])

  useEffect(() => {
    fetchSources()
  }, [fetchSources])

  const loadGraph = useCallback(async () => {
    setGraphLoading(true)
    setGraphError('')
    try {
      setGraph(await getKnowledgeGraphOverview())
    } catch (err) {
      setGraphError(err.message || 'Không thể tải đồ thị tri thức.')
    } finally {
      setGraphLoading(false)
    }
  }, [])

  useEffect(() => {
    if (tab === 'graph' && !graph && !graphLoading) loadGraph()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab])

  const selectSource = useCallback(async (sourceId) => {
    setSelectedId(sourceId)
    setLoadingDetail(true)
    try {
      setDetail(await getKnowledgeSource(sourceId))
    } catch (err) {
      setDetail(null)
      showToast(err.message || 'Không thể tải chi tiết nguồn.')
    } finally {
      setLoadingDetail(false)
    }
  }, [])

  const handleIngested = async (result) => {
    showToast(`Đã trích xuất ${result.concepts.length} khái niệm, ${result.edges.length} quan hệ, ${result.rules.length} quy tắc. Hãy rà soát và duyệt bên dưới.`)
    await fetchSources()
    await selectSource(result.source_id)
  }

  const handleApprove = async () => {
    setConfirm(null)
    setApproving(true)
    try {
      const result = await approveKnowledgeSource(selectedId)
      showToast(
        `Đã ghi vào đồ thị: ${result.concepts_upserted} khái niệm, ${result.edges_upserted} quan hệ, ` +
        `${result.rules_upserted} quy tắc, ${result.concept_vectors_upserted} vector.`
      )
      setGraph(null) // graph tab reloads fresh data next time
      await fetchSources()
      await selectSource(selectedId)
    } catch (err) {
      showToast(err.message || 'Không thể duyệt nguồn tri thức.')
    } finally {
      setApproving(false)
    }
  }

  const handleDelete = async () => {
    const sourceId = confirm?.sourceId
    setConfirm(null)
    try {
      await deleteKnowledgeSource(sourceId)
      showToast('Đã xóa nguồn tri thức.')
      if (sourceId === selectedId) {
        setSelectedId(null)
        setDetail(null)
      }
      setGraph(null)
      await fetchSources()
    } catch (err) {
      showToast(err.message || 'Không thể xóa nguồn tri thức.')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="font-headline-md text-primary">Tri thức AI</h1>
          <p className="text-sm text-on-surface-variant mt-1">
            Nạp nguồn tri thức thời trang, rà soát kết quả trích xuất và quản lý đồ thị tri thức
          </p>
        </div>
        <div className="flex rounded-lg bg-surface-container p-1">
          {TABS.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              onClick={() => setTab(id)}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-md text-xs font-medium transition-colors ${
                tab === id ? 'bg-primary text-on-primary' : 'text-on-surface-variant hover:text-primary'
              }`}
            >
              <Icon size={13} /> {label}
            </button>
          ))}
        </div>
      </div>

      {toast && (
        <div className="rounded-lg bg-primary/10 border border-primary/10 px-4 py-2.5 text-xs text-primary">{toast}</div>
      )}

      {tab === 'sources' ? (
        <div className="space-y-6">
          <IngestForm userId={user?.id} onIngested={handleIngested} />

          <div className="bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
            <div className="p-4 border-b border-outline-variant/20 flex items-center gap-2">
              <FileText size={15} className="text-primary" />
              <h2 className="font-title-lg text-primary">Danh sách nguồn</h2>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="bg-surface-container-low/50">
                    <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Tiêu đề</th>
                    <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Trạng thái</th>
                    <th className="text-center font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Khái niệm</th>
                    <th className="text-center font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Quan hệ</th>
                    <th className="text-center font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Quy tắc</th>
                    <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Ngày tạo</th>
                    <th className="px-4 py-3" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/5">
                  {loadingSources ? (
                    <tr><td colSpan={7} className="text-center py-8 text-sm text-on-surface-variant">Đang tải danh sách nguồn...</td></tr>
                  ) : listError ? (
                    <tr><td colSpan={7} className="text-center py-8 text-sm text-error">{listError}</td></tr>
                  ) : sourcesList.length === 0 ? (
                    <tr><td colSpan={7} className="text-center py-8 text-sm text-on-surface-variant">Chưa có nguồn tri thức nào. Hãy nạp nguồn đầu tiên ở trên.</td></tr>
                  ) : sourcesList.map((source) => (
                    <tr
                      key={source.id}
                      onClick={() => selectSource(source.id)}
                      className={`cursor-pointer transition-colors hover:bg-surface-container-high/30 ${selectedId === source.id ? 'bg-surface-container-low' : ''}`}
                    >
                      <td className="px-4 py-3 text-sm font-medium text-primary max-w-[280px] truncate">
                        {source.title || 'Nguồn không tiêu đề'}
                      </td>
                      <td className="px-4 py-3"><StatusBadge status={source.status} /></td>
                      <td className="px-4 py-3 text-center text-sm text-on-surface">{source.concepts_count}</td>
                      <td className="px-4 py-3 text-center text-sm text-on-surface">{source.edges_count}</td>
                      <td className="px-4 py-3 text-center text-sm text-on-surface">{source.rules_count}</td>
                      <td className="px-4 py-3 text-xs text-on-surface-variant">{formatDateTime(source.created_at)}</td>
                      <td className="px-4 py-3 text-right">
                        <button
                          onClick={(e) => {
                            e.stopPropagation()
                            setConfirm({ type: 'delete', sourceId: source.id })
                          }}
                          title="Xóa nguồn"
                          className="p-1.5 rounded-md text-on-surface-variant hover:text-error hover:bg-error-container/30"
                        >
                          <Trash2 size={13} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <SourceDetailPanel
            detail={detail}
            loading={loadingDetail}
            approving={approving}
            onApprove={() => setConfirm({ type: 'approve', sourceId: selectedId })}
            onDelete={() => setConfirm({ type: 'delete', sourceId: selectedId })}
          />
        </div>
      ) : (
        <KnowledgeGraphExplorer graph={graph} loading={graphLoading} error={graphError} onReload={loadGraph} />
      )}

      <AdminConfirmDialog
        open={confirm?.type === 'approve'}
        title="Duyệt nguồn tri thức?"
        message="Toàn bộ khái niệm, quan hệ và quy tắc đã trích xuất sẽ được ghi vào đồ thị tri thức Neo4j và đánh chỉ mục vector. AI sẽ dùng tri thức này khi tư vấn."
        confirmLabel="Duyệt & ghi"
        loading={approving}
        onConfirm={handleApprove}
        onCancel={() => setConfirm(null)}
      />
      <AdminConfirmDialog
        open={confirm?.type === 'delete'}
        title="Xóa nguồn tri thức?"
        message="Nguồn này sẽ bị xóa. Nếu đã duyệt, tri thức do nguồn này đóng góp cũng sẽ bị gỡ khỏi đồ thị Neo4j và chỉ mục vector."
        confirmLabel="Xóa"
        destructive
        onConfirm={handleDelete}
        onCancel={() => setConfirm(null)}
      />
    </div>
  )
}
