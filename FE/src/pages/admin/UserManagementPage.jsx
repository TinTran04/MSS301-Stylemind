import { useEffect, useState, useCallback } from 'react'
import { Search, Shield, ShieldOff, UserCheck, UserX, ChevronLeft, ChevronRight, RefreshCw, Plus, Trash2 } from 'lucide-react'
import useUserStore from '../../features/users/user.store'
import useAuthStore from '../../features/auth/auth.store'
import Drawer from '../../components/common/Drawer'
import AdminConfirmDialog from '../../components/admin/AdminConfirmDialog'

const ROLE_STYLES = {
  ADMIN: 'bg-ai-lavender text-ai-indigo',
  CUSTOMER: 'bg-surface-container-high text-on-surface-variant',
}

const PAGE_SIZE = 20

export default function UserManagementPage() {
  const {
    content,
    totalElements,
    totalPages,
    currentPage,
    loading,
    error,
    actionError,
    createError,
    loadUsers,
    createAccount,
    changeRole,
    toggleEnabled,
    removeAccount,
    clearError,
    clearActionError,
    clearCreateError,
  } = useUserStore()
  const currentUserId = useAuthStore((s) => s.user?.id)

  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [selected, setSelected] = useState(null)
  const [actionLoading, setActionLoading] = useState(false)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [createForm, setCreateForm] = useState({ email: '', role: 'CUSTOMER' })
  const [createLoading, setCreateLoading] = useState(false)
  const [confirmDialog, setConfirmDialog] = useState(null)

  const requestConfirm = (dialog) => setConfirmDialog(dialog)
  const closeConfirmDialog = () => setConfirmDialog(null)
  const handleConfirmAccept = async () => {
    if (!confirmDialog) return
    setActionLoading(true)
    try {
      await confirmDialog.onConfirm()
    } finally {
      setActionLoading(false)
      setConfirmDialog(null)
    }
  }

  // Debounce search 400ms
  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(search), 400)
    return () => clearTimeout(t)
  }, [search])

  const enabledParam = statusFilter === '' ? null : statusFilter === 'active'

  const fetchUsers = useCallback(
    (page = 0) => loadUsers({ page, size: PAGE_SIZE, search: debouncedSearch, role: roleFilter, enabled: enabledParam }),
    [debouncedSearch, roleFilter, enabledParam, loadUsers],
  )

  useEffect(() => {
    fetchUsers(0)
  }, [debouncedSearch, roleFilter, statusFilter]) // re-fetch when filters change, reset to page 0

  // Sync selected with latest store data
  useEffect(() => {
    if (selected) {
      const fresh = content.find((u) => u.id === selected.id)
      if (fresh) setSelected(fresh)
      else setSelected(null)
    }
  }, [content])

  async function handleChangeRole(userId, newRole) {
    setActionLoading(true)
    const updated = await changeRole(userId, newRole)
    if (updated) setSelected(updated)
    setActionLoading(false)
  }

  function handleToggleEnabled(userId, currentEnabled) {
    // Disabling is impactful (locks the account out) — confirm first.
    // Re-enabling is not destructive, so it applies immediately.
    if (currentEnabled) {
      requestConfirm({
        title: 'Vô hiệu hóa tài khoản?',
        message: 'Tài khoản này sẽ không thể đăng nhập sau khi bị vô hiệu hóa.',
        confirmLabel: 'Vô hiệu hóa',
        destructive: true,
        onConfirm: async () => {
          const updated = await toggleEnabled(userId, false)
          if (updated) setSelected(updated)
        },
      })
      return
    }
    setActionLoading(true)
    toggleEnabled(userId, true).then((updated) => {
      if (updated) setSelected(updated)
      setActionLoading(false)
    })
  }

  function handleDelete(userId) {
    requestConfirm({
      title: 'Xóa tài khoản?',
      message: 'Bạn có chắc chắn muốn xóa tài khoản này không? Thao tác này không thể hoàn tác.',
      confirmLabel: 'Xóa tài khoản',
      destructive: true,
      onConfirm: async () => {
        const ok = await removeAccount(userId)
        if (ok) setSelected(null)
      },
    })
  }

  function handleSelectUser(user) {
    clearActionError()
    setSelected(user)
  }

  async function handleCreateSubmit(e) {
    e.preventDefault()
    setCreateLoading(true)
    const created = await createAccount(createForm)
    setCreateLoading(false)
    if (created) {
      setDrawerOpen(false)
      setCreateForm({ email: '', role: 'CUSTOMER' })
    }
  }

  function handleOpenCreateDrawer() {
    clearCreateError()
    setDrawerOpen(true)
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
        <div className="flex items-center gap-2">
          <button
            onClick={() => fetchUsers(currentPage)}
            disabled={loading}
            className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-on-surface-variant hover:bg-surface-container-high transition-colors disabled:opacity-40"
          >
            <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
            Làm mới
          </button>
          <button
            onClick={handleOpenCreateDrawer}
            className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium bg-primary text-on-primary hover:opacity-90 transition-opacity"
          >
            <Plus size={14} />
            Tạo tài khoản
          </button>
        </div>
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
          {/* Search + filter bar */}
          <div className="p-4 border-b border-outline-variant/20 flex flex-wrap gap-3">
            <div className="relative max-w-sm flex-1 min-w-[200px]">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm theo email…"
                className="w-full pl-9 pr-4 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none"
              />
            </div>
            <select
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
              className="px-3 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none"
            >
              <option value="">Mọi role</option>
              <option value="ADMIN">ADMIN</option>
              <option value="CUSTOMER">CUSTOMER</option>
            </select>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-3 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none"
            >
              <option value="">Mọi trạng thái</option>
              <option value="active">Active</option>
              <option value="disabled">Banned</option>
            </select>
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
                    onClick={() => handleSelectUser(u)}
                    className={`cursor-pointer hover:bg-surface-container-high/30 transition-colors ${
                      selected?.id === u.id ? 'bg-surface-container-low' : ''
                    } ${!u.enabled ? 'opacity-50' : ''}`}
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center text-xs font-semibold text-on-primary-container shrink-0">
                          {initials(u.email)}
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-primary truncate">{u.email}</p>
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
                  {initials(selected.email)}
                </div>
                <h3 className="font-title-lg text-primary leading-tight break-all">{selected.email}</h3>
                <div className="flex items-center gap-2 mt-2">
                  <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${ROLE_STYLES[selected.role] || ROLE_STYLES.CUSTOMER}`}>
                    {selected.role}
                  </span>
                  {selected.id === currentUserId && (
                    <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-surface-container-high text-on-surface-variant">
                      Bạn
                    </span>
                  )}
                </div>
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

              {/* Row/action-level error: self-protection, last-admin, 403, etc.
                  Shown near the action panel instead of a page-wide banner. */}
              {actionError && (
                <div className="bg-error/10 border border-error/20 rounded-lg px-3 py-2 text-xs text-error flex items-start justify-between gap-2">
                  <span>{actionError}</span>
                  <button onClick={clearActionError} className="font-medium underline shrink-0">Đóng</button>
                </div>
              )}

              {/* Actions — hidden for the signed-in admin's own row.
                  This is convenience only; the backend rejects self/last-admin actions with 409 regardless. */}
              {selected.id !== currentUserId ? (
                <div className="space-y-2 pt-1 border-t border-outline-variant/20">
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

                  <button
                    onClick={() => handleDelete(selected.id)}
                    disabled={actionLoading}
                    className="w-full flex items-center justify-center gap-2 py-2 rounded-lg text-xs font-medium bg-error/10 text-error hover:bg-error/20 transition-colors disabled:opacity-40"
                  >
                    <Trash2 size={14} /> Xóa tài khoản
                  </button>
                </div>
              ) : (
                <p className="text-xs text-on-surface-variant text-center border-t border-outline-variant/20 pt-3">
                  Bạn không thể tự khóa, xóa hoặc đổi role của chính mình.
                </p>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Create account drawer */}
      <Drawer isOpen={drawerOpen} onClose={() => setDrawerOpen(false)} title="Tạo tài khoản">
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          {createError && (
            <div className="bg-error/10 border border-error/20 rounded-lg px-3 py-2 text-xs text-error flex items-start justify-between gap-2">
              <span>{createError}</span>
              <button type="button" onClick={clearCreateError} className="font-medium underline shrink-0">Đóng</button>
            </div>
          )}
          <div>
            <label className="block text-xs font-medium text-on-surface-variant mb-1">Email</label>
            <input
              type="email"
              required
              value={createForm.email}
              onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
              placeholder="user@example.com"
              className="w-full px-3 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-on-surface-variant mb-1">Role</label>
            <select
              value={createForm.role}
              onChange={(e) => setCreateForm({ ...createForm, role: e.target.value })}
              className="w-full px-3 py-2 bg-surface-container rounded-lg text-sm border-0 outline-none"
            >
              <option value="CUSTOMER">CUSTOMER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>
          <p className="text-xs text-on-surface-variant">
            Một email thiết lập mật khẩu sẽ được gửi tới tài khoản mới.
          </p>
          <button
            type="submit"
            disabled={createLoading}
            className="w-full py-2 rounded-lg text-sm font-medium bg-primary text-on-primary hover:opacity-90 transition-opacity disabled:opacity-40"
          >
            {createLoading ? 'Đang tạo…' : 'Tạo tài khoản'}
          </button>
        </form>
      </Drawer>

      <AdminConfirmDialog
        open={!!confirmDialog}
        title={confirmDialog?.title}
        message={confirmDialog?.message}
        confirmLabel={confirmDialog?.confirmLabel}
        destructive={confirmDialog?.destructive}
        loading={actionLoading}
        onConfirm={handleConfirmAccept}
        onCancel={closeConfirmDialog}
      />
    </div>
  )
}
