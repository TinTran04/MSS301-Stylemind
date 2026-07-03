import { create } from 'zustand'
import { listUsers, createUser, changeUserRole, changeUserEnabled, deleteUser } from './user.api'

const useUserStore = create((set, get) => ({
  content: [],
  totalElements: 0,
  totalPages: 1,
  currentPage: 0,
  loading: false,
  error: null,

  loadUsers: async ({ page = 0, size = 20, search = '', role = '', enabled = null } = {}) => {
    set({ loading: true, error: null })
    try {
      const data = await listUsers({ page, size, search, role, enabled })
      set({
        content: data.content,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
        currentPage: data.page,
        loading: false,
      })
    } catch (err) {
      set({ error: err.message || 'Không thể tải danh sách người dùng.', loading: false })
    }
  },

  createAccount: async ({ email, role }) => {
    try {
      const created = await createUser({ email, role })
      set({ content: [created, ...get().content], totalElements: get().totalElements + 1 })
      return created
    } catch (err) {
      set({ error: err.message || 'Không thể tạo tài khoản.' })
      return null
    }
  },

  changeRole: async (userId, role) => {
    try {
      const updated = await changeUserRole(userId, role)
      set({
        content: get().content.map((u) => (u.id === userId ? updated : u)),
      })
      return updated
    } catch (err) {
      set({ error: friendlyError(err, 'Không thể cập nhật role.') })
      return null
    }
  },

  toggleEnabled: async (userId, enabled) => {
    // Optimistic update
    set({ content: get().content.map((u) => (u.id === userId ? { ...u, enabled } : u)) })
    try {
      const updated = await changeUserEnabled(userId, enabled)
      set({ content: get().content.map((u) => (u.id === userId ? updated : u)) })
      return updated
    } catch (err) {
      // Rollback
      set({
        content: get().content.map((u) => (u.id === userId ? { ...u, enabled: !enabled } : u)),
        error: friendlyError(err, 'Không thể cập nhật trạng thái.'),
      })
      return null
    }
  },

  removeAccount: async (userId) => {
    try {
      await deleteUser(userId)
      set({
        content: get().content.filter((u) => u.id !== userId),
        totalElements: Math.max(0, get().totalElements - 1),
      })
      return true
    } catch (err) {
      set({ error: friendlyError(err, 'Không thể xóa tài khoản.') })
      return false
    }
  },

  clearError: () => set({ error: null }),
}))

function friendlyError(err, fallback) {
  if (err?.status === 409) {
    return err.message || 'Thao tác không được phép trên tài khoản này.'
  }
  return err?.message || fallback
}

export default useUserStore
