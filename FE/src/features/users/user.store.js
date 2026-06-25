import { create } from 'zustand'
import { listUsers, changeUserRole, changeUserEnabled } from './user.api'

const useUserStore = create((set, get) => ({
  content: [],
  totalElements: 0,
  totalPages: 1,
  currentPage: 0,
  loading: false,
  error: null,

  loadUsers: async ({ page = 0, size = 20, search = '' } = {}) => {
    set({ loading: true, error: null })
    try {
      const data = await listUsers({ page, size, search })
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

  changeRole: async (userId, role) => {
    try {
      const updated = await changeUserRole(userId, role)
      set({
        content: get().content.map((u) => (u.id === userId ? updated : u)),
      })
      return updated
    } catch (err) {
      set({ error: err.message || 'Không thể cập nhật role.' })
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
        error: err.message || 'Không thể cập nhật trạng thái.',
      })
      return null
    }
  },

  clearError: () => set({ error: null }),
}))

export default useUserStore
