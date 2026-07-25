import { useEffect, useState } from 'react'
import { Edit3, Home, MapPin, Plus, Star, Trash2 } from 'lucide-react'
import Modal from '../../components/common/Modal'
import { useAuth } from '../../hooks/useAuth'
import AddressForm, { addressToForm, createEmptyAddressForm } from '../../features/profile/AddressForm'
import {
  createAddress,
  deleteAddress,
  getAddresses,
  getProfile,
  getProvinces,
  getWards,
  setDefaultAddress,
  updateAddress,
} from '../../features/profile/profile.api'
import { buildAddressPayload, formatSavedAddress } from '../../features/profile/address.utils'
import { getVietnamesePhoneValidationMessage } from '../../features/profile/phone.utils'

export default function ProfilePage() {
  const { user } = useAuth()
  const [profile, setProfile] = useState(null)
  const [addresses, setAddresses] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editingAddress, setEditingAddress] = useState(null)
  const [form, setForm] = useState(createEmptyAddressForm())
  const [provinces, setProvinces] = useState([])
  const [wards, setWards] = useState([])
  const [administrativeLoading, setAdministrativeLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [pendingDelete, setPendingDelete] = useState(null)
  const [actionId, setActionId] = useState('')

  const loadAddresses = async () => {
    const list = await getAddresses()
    setAddresses(Array.isArray(list) ? list : [])
  }

  const loadPage = async () => {
    setLoading(true)
    setError('')
    try {
      const [nextProfile] = await Promise.all([getProfile(), loadAddresses(), getProvinces().then((list) => setProvinces(Array.isArray(list) ? list : []))])
      setProfile(nextProfile || null)
    } catch (requestError) {
      setError(requestError?.message || 'Không thể tải thông tin tài khoản.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadPage() }, [])

  useEffect(() => {
    let active = true
    if (!form.provinceCode || !formOpen) {
      setWards([])
      return undefined
    }
    setAdministrativeLoading(true)
    getWards(form.provinceCode)
      .then((list) => { if (active) setWards(Array.isArray(list) ? list : []) })
      .catch((requestError) => { if (active) setError(requestError?.message || 'Không thể tải dữ liệu phường/xã.') })
      .finally(() => { if (active) setAdministrativeLoading(false) })
    return () => { active = false }
  }, [form.provinceCode, formOpen])

  const closeForm = () => {
    if (saving) return
    setFormOpen(false)
    setEditingAddress(null)
    setForm(createEmptyAddressForm())
  }

  const openCreate = () => {
    setSuccess('')
    setError('')
    setEditingAddress(null)
    setForm(createEmptyAddressForm())
    setFormOpen(true)
  }

  const openEdit = (address) => {
    setSuccess('')
    setError('')
    setEditingAddress(address)
    setForm(addressToForm(address))
    setFormOpen(true)
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    const phoneError = getVietnamesePhoneValidationMessage(form.phoneNumber)
    if (phoneError) {
      setError(phoneError)
      return
    }
    const payload = buildAddressPayload(form)
    if (!payload.recipientName || !payload.addressLine || !payload.provinceCode || !payload.wardCode) {
      setError('Vui lòng điền đầy đủ thông tin địa chỉ giao hàng.')
      return
    }
    setSaving(true)
    setError('')
    try {
      if (editingAddress) {
        await updateAddress(editingAddress.id, payload)
        setSuccess('Đã cập nhật địa chỉ giao hàng.')
      } else {
        await createAddress(payload)
        setSuccess('Đã thêm địa chỉ giao hàng.')
      }
      await loadAddresses()
      closeForm()
    } catch (requestError) {
      setError(requestError?.message || 'Không thể lưu địa chỉ. Vui lòng thử lại.')
    } finally {
      setSaving(false)
    }
  }

  const handleSetDefault = async (address) => {
    setActionId(address.id)
    setError('')
    try {
      await setDefaultAddress(address.id)
      await loadAddresses()
      setSuccess('Đã cập nhật địa chỉ mặc định.')
    } catch (requestError) {
      setError(requestError?.message || 'Không thể cập nhật địa chỉ mặc định.')
    } finally {
      setActionId('')
    }
  }

  const handleDelete = async () => {
    if (!pendingDelete) return
    setActionId(pendingDelete.id)
    setError('')
    try {
      await deleteAddress(pendingDelete.id)
      await loadAddresses()
      setSuccess('Đã xóa địa chỉ giao hàng.')
      setPendingDelete(null)
    } catch (requestError) {
      setError(requestError?.message || 'Không thể xóa địa chỉ. Vui lòng thử lại.')
    } finally {
      setActionId('')
    }
  }

  const displayName = profile?.displayName || user?.name || user?.email || 'Khách hàng'

  return (
    <main className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <header className="border-b border-outline-variant/20 pb-7">
        <p className="text-sm font-medium text-on-surface-variant">Tài khoản của tôi</p>
        <h1 className="mt-1 font-headline-md text-primary">Địa chỉ giao hàng</h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-on-surface-variant">Quản lý thông tin nhận hàng để thanh toán nhanh và chính xác hơn.</p>
      </header>

      <section className="mt-6 grid gap-4 border-b border-outline-variant/15 pb-6 sm:grid-cols-2" aria-label="Thông tin tài khoản">
        <div><p className="text-xs font-medium uppercase tracking-wide text-on-surface-variant">Tên hiển thị</p><p className="mt-1 text-sm font-medium text-primary">{displayName}</p></div>
        <div><p className="text-xs font-medium uppercase tracking-wide text-on-surface-variant">Email</p><p className="mt-1 break-all text-sm font-medium text-primary">{user?.email || 'Chưa có thông tin'}</p></div>
      </section>

      <section className="mt-8" aria-labelledby="saved-addresses-heading">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h2 id="saved-addresses-heading" className="font-title-lg text-primary">Địa chỉ đã lưu</h2>
            <p className="mt-1 text-sm text-on-surface-variant">Địa chỉ mặc định sẽ được chọn trước khi thanh toán.</p>
          </div>
          <button type="button" onClick={openCreate} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2.5 text-sm font-medium text-on-primary hover:opacity-90"><Plus size={16} /> Thêm địa chỉ</button>
        </div>

        {error && <p role="alert" className="mt-5 rounded-lg border border-error/20 bg-error-container/20 px-4 py-3 text-sm text-error">{error}</p>}
        {success && <p role="status" className="mt-5 rounded-lg border border-green-status/20 bg-green-status/10 px-4 py-3 text-sm text-primary">{success}</p>}

        {loading ? (
          <div className="mt-6 grid gap-4"><div className="h-40 animate-pulse rounded-lg bg-surface-container" /><div className="h-40 animate-pulse rounded-lg bg-surface-container" /></div>
        ) : addresses.length === 0 ? (
          <div className="mt-6 rounded-lg border border-dashed border-outline-variant/35 px-6 py-12 text-center"><Home className="mx-auto text-on-surface-variant" size={28} /><h3 className="mt-4 font-medium text-primary">Chưa có địa chỉ giao hàng</h3><p className="mt-1 text-sm text-on-surface-variant">Thêm địa chỉ đầu tiên để dùng khi thanh toán.</p></div>
        ) : (
          <div className="mt-6 grid gap-4 lg:grid-cols-2">
            {addresses.map((address) => (
              <article key={address.id} className="rounded-lg border border-outline-variant/20 bg-surface-container-lowest p-5">
                <div className="flex items-start justify-between gap-3"><div><h3 className="font-medium text-primary">{address.recipientName || 'Chưa có thông tin'}</h3><p className="mt-1 text-sm text-on-surface-variant">{address.phoneNumber || 'Chưa có thông tin'}</p></div>{address.isDefault && <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-primary px-2.5 py-1 text-xs font-medium text-on-primary"><Star size={12} fill="currentColor" /> Mặc định</span>}</div>
                <div className="mt-4 flex gap-2 text-sm text-on-surface-variant"><MapPin size={16} className="mt-0.5 shrink-0" /><p>{formatSavedAddress(address)}</p></div>
                {address.shippingNote && <p className="mt-3 border-l-2 border-outline-variant/30 pl-3 text-xs leading-5 text-on-surface-variant">{address.shippingNote}</p>}
                <div className="mt-5 flex flex-wrap gap-2 border-t border-outline-variant/15 pt-4">
                  {!address.isDefault && <button type="button" onClick={() => handleSetDefault(address)} disabled={actionId === address.id} className="rounded-lg border border-outline-variant/30 px-3 py-2 text-xs font-medium text-primary hover:bg-surface-container-high disabled:opacity-60">Đặt làm mặc định</button>}
                  <button type="button" onClick={() => openEdit(address)} className="inline-flex items-center gap-1 rounded-lg px-3 py-2 text-xs font-medium text-primary hover:bg-surface-container-high"><Edit3 size={14} /> Chỉnh sửa</button>
                  <button type="button" onClick={() => setPendingDelete(address)} className="ml-auto inline-flex items-center gap-1 rounded-lg px-3 py-2 text-xs font-medium text-error hover:bg-error-container/20"><Trash2 size={14} /> Xóa</button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <Modal isOpen={formOpen} onClose={closeForm} title={editingAddress ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ'} className="max-w-2xl max-h-[calc(100vh-2rem)] overflow-y-auto">
        <AddressForm form={form} provinces={provinces} wards={wards} loadingAdministrativeData={administrativeLoading} submitting={saving} submitLabel={editingAddress ? 'Lưu thay đổi' : 'Lưu địa chỉ'} showDefaultOption={!editingAddress} onChange={setForm} onSubmit={handleSubmit} onCancel={closeForm} />
      </Modal>
      <Modal isOpen={Boolean(pendingDelete)} onClose={() => { if (!actionId) setPendingDelete(null) }} title="Xóa địa chỉ?">
        <p className="text-sm leading-6 text-on-surface-variant">Địa chỉ này sẽ bị xóa khỏi tài khoản. Các đơn hàng cũ vẫn giữ nguyên địa chỉ đã lưu tại thời điểm đặt hàng.</p>
        <div className="mt-6 flex justify-end gap-3"><button type="button" onClick={() => setPendingDelete(null)} disabled={Boolean(actionId)} className="rounded-lg px-4 py-2 text-sm font-medium text-on-surface-variant">Hủy</button><button type="button" onClick={handleDelete} disabled={Boolean(actionId)} className="rounded-lg bg-error px-4 py-2 text-sm font-medium text-white disabled:opacity-60">{actionId ? 'Đang xóa...' : 'Xóa địa chỉ'}</button></div>
      </Modal>
    </main>
  )
}
