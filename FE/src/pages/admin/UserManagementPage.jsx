import { useEffect, useState, useCallback } from 'react'
import { Search, Shield, ShieldOff, UserCheck, UserX, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import useUserStore from '../../features/users/user.store'

const ROLE_STYLES = {
  ADMIN: 'bg-ai-lavender text-ai-indigo',
  CUSTOMER: 'bg-surface-container-high text-on-surface-variant',
}

const PAGE_SIZE = 20

export default function UserManagementPage() {
  const { content, totalElements, totalPages, currentPage, loading, error, loadUsers, changeRole, toggleEnabled, clearError } =
    useUserStore()

  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [selected, setSelected] = useState(null)
  const [actionLoading, setActionLoading] = useState(false)

  // Debounce search 400ms
  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(search), 400)
    return () => clearTimeout(t)
  }, [search])

  const fetchUsers = useCallback(
    (page = 0) => loadUsers({ page, size: PAGE_SIZE, search: debouncedSearch }),
    [debouncedSearch, loadUsers],
  )

  useEffect(() => {
    fetchUsers(0)
  }, [debouncedSearch]) // re-fetch when search changes, reset to page 0

  // Sync selected with latest store data
  useEffect(() => {
    if (selected) {
      const fresh = content.find((u) => u.id === selected.id)
      if (fresh) setSelected(fresh)
    }
  }, [content])

  async function handleChangeRole(userId, newRole) {
    setActionLoading(true)
    const updated = await changeRole(userId, newRole)
    if (updated) setSelected(updated)
    setActionLoading(false)
  }

  async function handleToggleEnabled(userId, currentEnabled) {
    setActionLoading(true)
    const updated = await toggleEnabled(userId, !currentEnabled)
    if (updated) setSelected(updated)
    setActionLoading(false)
  }

  const initials = (name) =>
    (name || '?')
      .split(' ')
      .map((w) => w[0])
      .join('')
      .slice(0, 2)
      .toUpperCase()

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-headline-md text-primary">User Management</h1>
          <p className="text-sm text-on-surface-variant mt-1">
            {loading ? 'Đang tải…' : `${totalElements} người dùng`}
          </p>
        </div>
        <button
          onClick={() => fetchUsers(currentPage)}
          disabled={loading}
          className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-high transition-colors disabled:opacity-40"
        >
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          Làm mới
        </button>
      </div>

      {/* Error banner */}
      {error && (
        <div className="bg-error/10 border border-error/20 rounded-lg px-4 py-3 text-sm text-error flex items-center justify-between">
          <span>{error}</span>
          <button onClick={clearError} className="font-medium underline">Đóng</button>
        </div>
      )}

      <div className="flex gap-6">
        {/* Table */}
        <div className="flex-1 bg-surface-container-lowest rounded-xl ambient-shadow overflow-hidden">
          {/* Search bar */}
          <div className="p-4 border-b border-outline-variant/20">
            <div className="relative max-w-sm">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm theo email hoặc tên…"
                className="w-full pl-9 pr-4 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none"
              />
            </div>
          </div>

          {/* Table body */}
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-surface-container-low/50">
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Người dùng</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Role</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Provider</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Trạng thái</th>
                  <th className="text-left font-label-sm uppercase text-on-surface-variant text-xs px-4 py-3">Ngày tạo</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/5">
                {loading && content.length === 0 && (
                  <tr>
                    <td colSpan={5} className="text-center py-12 text-sm text-on-surface-variant">
                      Đang tải…
                    </td>
                  </tr>
                )}
                {!loading && content.length === 0 && (
                  <tr>
                    <td colSpan={5} className="text-center py-12 text-sm text-on-surface-variant">
                      Không tìm thấy người dùng nào.
                    </td>
                  </tr>
                )}
                {content.map((u) => (
                  <tr
                    key={u.id}
                    onClick={() => setSelected(u)}
                    className={`cursor-pointer hover:bg-surface-container-high/30 transition-colors ${
                      selected?.id === u.id ? 'bg-surface-container-low' : ''
                    } ${!u.enabled ? 'opacity-50' : ''}`}
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center text-xs font-semibold text-on-primary-container shrink-0">
                          {initials(u.fullName || u.email)}
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-primary truncate">{u.fullName || '—'}</p>
                          <p className="text-xs text-on-surface-variant truncate">{u.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${ROLE_STYLES[u.role] || ROLE_STYLES.CUSTOMER}`}>
                        {u.role}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-on-surface-variant uppercase">{u.provider}</td>
                    <td className="px-4 py-3">
                      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${u.enabled ? 'bg-tertiary-fixed/30 text-tertiary' : 'bg-error/15 text-error'}`}>
                        {u.enabled ? 'Active' : 'Banned'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-xs text-on-surface-variant">
                      {u.createdAt ? new Date(u.createdAt).toLocaleDateString('vi-VN') : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t border-outline-variant/20 text-sm text-on-surface-variant">
              <span>
                Trang {currentPage + 1} / {totalPages} · {totalElements} users
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => fetchUsers(currentPage - 1)}
                  disabled={currentPage === 0 || loading}
                  className="p-1.5 rounded-lg hover:bg-surface-container-high disabled:opacity-30 transition-colors"
                >
                  <ChevronLeft size={16} />
                </button>
                <button
                  onClick={() => fetchUsers(currentPage + 1)}
                  disabled={currentPage >= totalPages - 1 || loading}
                  className="p-1.5 rounded-lg hover:bg-surface-container-high disabled:opacity-30 transition-colors"
                >
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Detail panel */}
        {selected && (
          <div className="w-72 shrink-0">
            <div className="bg-surface-container-lowest rounded-xl p-5 ambient-shadow sticky top-24 space-y-5">
              {/* Avatar + name */}
              <div className="flex flex-col items-center text-center">
                <div className="w-14 h-14 rounded-full bg-primary-container flex items-center justify-center text-lg font-semibold text-on-primary-container mb-2">
                  {initials(selected.fullName || selected.email)}
                </div>
                <h3 className="font-title-lg text-primary leading-tight">{selected.fullName || '—'}</h3>
                <p className="text-xs text-on-surface-variant mt-0.5 break-all">{selected.email}</p>
                <span className={`text-xs font-medium px-2 py-0.5 rounded-full mt-2 ${ROLE_STYLES[selected.role] || ROLE_STYLES.CUSTOMER}`}>
                  {selected.role}
                </span>
              </div>

              {/* Info */}
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-on-surface-variant">Provider</span>
                  <span className="font-medium text-primary uppercase">{selected.provider}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-on-surface-variant">Trạng thái</span>
                  <span className={`font-medium ${selected.enabled ? 'text-tertiary' : 'text-error'}`}>
                    {selected.enabled ? 'Active' : 'Banned'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-on-surface-variant">Tạo lúc</span>
                  <span className="text-primary text-xs">
                    {selected.createdAt ? new Date(selected.createdAt).toLocaleDateString('vi-VN') : '—'}
                  </span>
                </div>
              </div>

              {/* Actions */}
              <div className="space-y-2 pt-1 border-t border-outline-variant/20">
                {/* Toggle role */}
                <button
                  onClick={() => handleChangeRole(selected.id, selected.role === 'ADMIN' ? 'CUSTOMER' : 'ADMIN')}
                  disabled={actionLoading}
                  className="w-full flex items-center justify-center gap-2 py-2 rounded-lg text-xs font-medium bg-surface-container hover:bg-surface-container-high transition-colors disabled:opacity-40"
                >
                  {selected.role === 'ADMIN' ? (
                    <><ShieldOff size={14} /> Hạ xuống CUSTOMER</>
                  ) : (
                    <><Shield size={14} /> Nâng lên ADMIN</>
                  )}
                </button>

                {/* Toggle ban */}
                <button
                  onClick={() => handleToggleEnabled(selected.id, selected.enabled)}
                  disabled={actionLoading}
                  className={`w-full flex items-center justify-center gap-2 py-2 rounded-lg text-xs font-medium transition-colors disabled:opacity-40 ${
                    selected.enabled
                      ? 'bg-error/10 text-error hover:bg-error/20'
                      : 'bg-tertiary-fixed/20 text-tertiary hover:bg-tertiary-fixed/40'
                  }`}
                >
                  {selected.enabled ? (
                    <><UserX size={14} /> Khóa tài khoản</>
                  ) : (
                    <><UserCheck size={14} /> Mở khóa tài khoản</>
                  )}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
