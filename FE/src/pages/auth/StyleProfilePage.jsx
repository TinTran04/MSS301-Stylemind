import { useEffect, useState } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion'
import {
  Home,
  Loader2,
  Mail,
  MapPin,
  PencilLine,
  Plus,
  Star,
  Trash2,
} from 'lucide-react'
import clsx from 'clsx'
import { useAuth } from '../../hooks/useAuth'
import Modal from '../../components/common/Modal'
import {
  createAddress,
  deleteAddress,
  getAddresses,
  getProvinces,
  getWards,
  getProfile,
  updateAddress,
  updateProfile,
} from '../../features/profile/profile.api'
import { buildAddressPayload, formatSavedAddress } from '../../features/profile/address.utils'
import { getVietnamesePhoneValidationMessage } from '../../features/profile/phone.utils.js'
import { getInitials } from '../../features/auth/auth.utils'
import { mockStyleOptions } from '../../features/profile/profile.mock'
import { VIETNAM_PROVINCES, validateVietnamesePhone, validateVietnameseCity } from '../../utils/vietnamLocation'

const tabs = [
  { id: 'style', label: 'Hồ sơ phong cách' },
  { id: 'addresses', label: 'Địa chỉ giao hàng' },
]

const addressFormTemplate = {
  recipientName: '',
  phoneNumber: '',
  addressLine: '',
  provinceCode: '',
  wardCode: '',
  shippingNote: '',
  isDefault: false,
}

const styleOptions = mockStyleOptions.stylePreferences
const bodyTypes = mockStyleOptions.bodyTypes
const fits = mockStyleOptions.fitPreferences
const colors = mockStyleOptions.colorPalettes

function createEmptyAddressForm() {
  return { ...addressFormTemplate }
}

function getOptionLabel(options, value) {
  return options.find((option) => option.value === value || option.id === value)?.label || value
}

function formatAddressLine(address) {
  return formatSavedAddress(address)
}

function isEmailLike(value) {
  return typeof value === 'string' && value.includes('@')
}

function SectionCard({ eyebrow, title, description, action }) {
  return (
    <div className="rounded-[1.75rem] border border-outline-variant/20 bg-surface-container-lowest p-6 shadow-[0_20px_40px_-28px_rgba(15,23,42,0.16)]">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div className="space-y-2">
          {eyebrow && (
            <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-on-surface-variant">
              {eyebrow}
            </p>
          )}
          <h2 className="text-2xl font-semibold tracking-tight text-primary md:text-[2rem]">{title}</h2>
          {description && <p className="max-w-[60ch] text-sm leading-relaxed text-on-surface-variant">{description}</p>}
        </div>
        {action}
      </div>
    </div>
  )
}

function PillTab({ active, label, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={clsx(
        'relative min-w-0 flex-1 overflow-hidden rounded-full px-4 py-3 text-sm font-medium transition-colors',
        active ? 'text-on-primary' : 'text-on-surface-variant hover:text-primary'
      )}
    >
      {active && (
        <motion.span
          layoutId="profile-tab-pill"
          className="absolute inset-0 rounded-full bg-primary shadow-[0_10px_24px_-14px_rgba(15,23,42,0.35)]"
          transition={{ type: 'spring', stiffness: 420, damping: 36 }}
        />
      )}
      <span className="relative z-10">{label}</span>
    </button>
  )
}

function LoadingSkeleton() {
  return (
    <div className="min-h-screen bg-background">
      <header className="flex items-center justify-between px-6 py-5 md:px-8">
        <div className="h-6 w-24 rounded-full bg-surface-container-high animate-pulse" />
        <div className="h-5 w-28 rounded-full bg-surface-container-high animate-pulse" />
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="rounded-[1.75rem] border border-outline-variant/20 bg-surface-container-lowest p-6">
          <div className="grid gap-6 lg:grid-cols-[1fr_auto] lg:items-center">
            <div className="space-y-4">
              <div className="h-3 w-28 rounded-full bg-surface-container-high animate-pulse" />
              <div className="h-11 w-72 max-w-full rounded-2xl bg-surface-container-high animate-pulse" />
              <div className="h-4 w-52 rounded-full bg-surface-container-high animate-pulse" />
            </div>
            <div className="h-20 w-full min-w-[240px] rounded-2xl bg-surface-container-high animate-pulse" />
          </div>
        </div>

        <div className="mt-6 flex flex-wrap gap-3 rounded-full border border-outline-variant/20 bg-surface-container-lowest p-1">
          <div className="h-11 flex-1 rounded-full bg-surface-container-high animate-pulse" />
          <div className="h-11 flex-1 rounded-full bg-surface-container-high animate-pulse" />
        </div>

        <div className="mt-6 space-y-6">
          <div className="h-72 rounded-[1.75rem] border border-outline-variant/20 bg-surface-container-lowest animate-pulse" />
          <div className="h-72 rounded-[1.75rem] border border-outline-variant/20 bg-surface-container-lowest animate-pulse" />
        </div>
      </main>
    </div>
  )
}

export default function StyleProfilePage() {
  const { user } = useAuth()
  const reduceMotion = useReducedMotion()

  const [activeTab, setActiveTab] = useState('style')
  const [displayName, setDisplayName] = useState('')
  const [selectedStyles, setSelectedStyles] = useState([])
  const [selectedBodyType, setSelectedBodyType] = useState(null)
  const [selectedFit, setSelectedFit] = useState(null)
  const [selectedColors, setSelectedColors] = useState([])
  const [loading, setLoading] = useState(true)
  const [savingProfile, setSavingProfile] = useState(false)
  const [profileError, setProfileError] = useState('')
  const [profileSuccess, setProfileSuccess] = useState('')

  const [addresses, setAddresses] = useState([])
  const [addressLoading, setAddressLoading] = useState(true)
  const [addressSaving, setAddressSaving] = useState(false)
  const [addressError, setAddressError] = useState('')
  const [addressSuccess, setAddressSuccess] = useState('')
  const [addressFormOpen, setAddressFormOpen] = useState(false)
  const [editingAddressId, setEditingAddressId] = useState(null)
  const [pendingDeleteAddress, setPendingDeleteAddress] = useState(null)
  const [addressDeleteError, setAddressDeleteError] = useState('')
  const [unsavedChangeConfirmOpen, setUnsavedChangeConfirmOpen] = useState(false)
  const [addressForm, setAddressForm] = useState(createEmptyAddressForm())
  const [addressFormBaseline, setAddressFormBaseline] = useState(createEmptyAddressForm())
  const [provinces, setProvinces] = useState([])
  const [wards, setWards] = useState([])
  const [administrativeLoading, setAdministrativeLoading] = useState(false)

  useEffect(() => {
    let active = true

    setLoading(true)
    getProfile()
      .then((profile) => {
        if (!active) return

        setDisplayName(profile?.displayName || '')
        setSelectedBodyType(profile?.bodyMorphology || null)
        setSelectedFit(profile?.preferredFit || null)

        if (profile?.stylePersonas) {
          try {
            const parsed = JSON.parse(profile.stylePersonas)
            setSelectedStyles(Array.isArray(parsed.styles) ? parsed.styles : [])
            setSelectedColors(Array.isArray(parsed.colors) ? parsed.colors : [])
          } catch {
            setSelectedStyles([])
            setSelectedColors([])
          }
        } else {
          setSelectedStyles([])
          setSelectedColors([])
        }
      })
      .catch(() => {
        if (active) setProfileError('Không thể tải hồ sơ phong cách của bạn.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    let active = true
    setAdministrativeLoading(true)
    getProvinces()
      .then((list) => {
        if (active) setProvinces(Array.isArray(list) ? list : [])
      })
      .catch(() => {
        if (active) setAddressError('Không thể tải dữ liệu tỉnh/thành phố.')
      })
      .finally(() => {
        if (active) setAdministrativeLoading(false)
      })
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    if (!addressForm.provinceCode) {
      setWards([])
      return undefined
    }
    getWards(addressForm.provinceCode)
      .then((list) => {
        if (active) setWards(Array.isArray(list) ? list : [])
      })
      .catch(() => {
        if (active) setAddressError('Không thể tải dữ liệu phường/xã.')
      })
    return () => { active = false }
  }, [addressForm.provinceCode])

  useEffect(() => {
    let active = true

    setAddressLoading(true)
    getAddresses()
      .then((list) => {
        if (!active) return
        setAddresses(Array.isArray(list) ? list : [])
      })
      .catch(() => {
        if (active) setAddressError('Không thể tải danh sách địa chỉ.')
      })
      .finally(() => {
        if (active) setAddressLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  const profileDisplayName = displayName.trim() || user?.name || user?.email || 'Khách hàng'
  const profileHeadingName = displayName.trim() || (!isEmailLike(user?.name) ? user?.name : '') || 'Chưa đặt tên'
  const profileEmail = user?.email || 'Chưa có email'
  const defaultAddress = addresses.find((address) => address.isDefault)
  const isAddressFormDirty =
    JSON.stringify(addressForm) !== JSON.stringify(addressFormBaseline)
  const tabTransition = reduceMotion
    ? { duration: 0 }
    : { duration: 0.24, ease: [0.16, 1, 0.3, 1] }

  const closeAddressForm = () => {
    setAddressFormOpen(false)
    setEditingAddressId(null)
    setAddressForm(createEmptyAddressForm())
    setAddressFormBaseline(createEmptyAddressForm())
    setAddressError('')
    setPendingDeleteAddress(null)
    setAddressDeleteError('')
    setUnsavedChangeConfirmOpen(false)
  }

  const requestCloseAddressForm = () => {
    if (!addressFormOpen) return

    if (isAddressFormDirty) {
      setUnsavedChangeConfirmOpen(true)
      return
    }

    closeAddressForm()
  }

  const openCreateAddressForm = () => {
    if (addressFormOpen && editingAddressId === null) {
      requestCloseAddressForm()
      return
    }

    setActiveTab('addresses')
    setEditingAddressId(null)
    const nextForm = createEmptyAddressForm()
    setAddressForm(nextForm)
    setAddressFormBaseline(nextForm)
    setAddressFormOpen(true)
    setAddressError('')
    setAddressSuccess('')
    setPendingDeleteAddress(null)
    setAddressDeleteError('')
    setUnsavedChangeConfirmOpen(false)
  }

  const openEditAddressForm = (address) => {
    if (addressFormOpen && editingAddressId === address.id) {
      requestCloseAddressForm()
      return
    }

    setActiveTab('addresses')
    const nextForm = {
      recipientName: address.recipientName || '',
      phoneNumber: address.phoneNumber || '',
      addressLine: address.addressLine || '',
      provinceCode: address.provinceCode || '',
      wardCode: address.wardCode || '',
      shippingNote: address.shippingNote || '',
      isDefault: Boolean(address.isDefault),
    }
    setEditingAddressId(address.id)
    setAddressForm(nextForm)
    setAddressFormBaseline(nextForm)
    setAddressFormOpen(true)
    setAddressError('')
    setAddressSuccess('')
    setPendingDeleteAddress(null)
    setAddressDeleteError('')
    setUnsavedChangeConfirmOpen(false)
  }

  const reloadAddresses = async () => {
    const list = await getAddresses()
    setAddresses(Array.isArray(list) ? list : [])
  }

  const toggleStyle = (style) => {
    setSelectedStyles((previous) =>
      previous.includes(style)
        ? previous.filter((value) => value !== style)
        : [...previous, style]
    )
    setProfileSuccess('')
  }

  const toggleColor = (color) => {
    setSelectedColors((previous) =>
      previous.includes(color)
        ? previous.filter((value) => value !== color)
        : [...previous, color]
    )
    setProfileSuccess('')
  }

  const completeProfile = async () => {
    setSavingProfile(true)
    setProfileError('')
    setProfileSuccess('')

    try {
      await updateProfile({
        displayName: displayName.trim() || null,
        bodyMorphology: selectedBodyType,
        preferredFit: selectedFit,
        stylePersonas: JSON.stringify({
          styles: selectedStyles,
          colors: selectedColors,
        }),
      })
      setProfileSuccess('Cập nhật hồ sơ phong cách thành công.')
    } catch {
      setProfileError('Không thể cập nhật hồ sơ phong cách. Vui lòng thử lại.')
    } finally {
      setSavingProfile(false)
    }
  }

  const handleAddressSubmit = async (event) => {
    event.preventDefault()
    setAddressSaving(true)
    setAddressError('')
    setAddressSuccess('')

    const phoneError = getVietnamesePhoneValidationMessage(addressForm.phoneNumber)
    if (phoneError) {
      setAddressError(phoneError)
      setAddressSaving(false)
      return
    }

    try {
      const payload = buildAddressPayload(addressForm)
      if (!payload.provinceCode || !payload.wardCode) {
        setAddressError('Vui lòng chọn đầy đủ Tỉnh/thành phố và Phường/xã.')
        setAddressSaving(false)
        return
      }
      if (editingAddressId) {
        await updateAddress(editingAddressId, payload)
        setAddressSuccess('Cập nhật địa chỉ thành công.')
      } else {
        await createAddress(payload)
        setAddressSuccess('Đã thêm địa chỉ giao hàng.')
      }

      await reloadAddresses()
      closeAddressForm()
    } catch (error) {
      setAddressError(error?.message || 'Không thể cập nhật địa chỉ. Vui lòng thử lại.')
    } finally {
      setAddressSaving(false)
    }
  }

  const requestDeleteAddress = (address) => {
    if (pendingDeleteAddress?.id === address.id) {
      setPendingDeleteAddress(null)
      setAddressDeleteError('')
      return
    }

    setPendingDeleteAddress(address)
    setAddressError('')
    setAddressSuccess('')
    setAddressDeleteError('')
    setUnsavedChangeConfirmOpen(false)
  }

  const confirmDeleteAddress = async () => {
    if (!pendingDeleteAddress) return

    setAddressSaving(true)
    setAddressError('')
    setAddressSuccess('')
    setAddressDeleteError('')

    try {
      await deleteAddress(pendingDeleteAddress.id)
      await reloadAddresses()
      setAddressSuccess('Đã xóa địa chỉ giao hàng.')
      if (editingAddressId === pendingDeleteAddress.id) {
        closeAddressForm()
      }
      setPendingDeleteAddress(null)
    } catch {
      setAddressDeleteError('Không thể xóa địa chỉ. Vui lòng thử lại.')
    } finally {
      setAddressSaving(false)
    }
  }

  const closeDeleteDialog = () => {
    if (addressSaving) return
    setPendingDeleteAddress(null)
    setAddressDeleteError('')
  }

  const discardAddressFormChanges = () => {
    setUnsavedChangeConfirmOpen(false)
    closeAddressForm()
  }

  const keepEditingAddressForm = () => {
    setUnsavedChangeConfirmOpen(false)
  }

  const handleSetDefault = async (address) => {
    if (address.isDefault) return

    setAddressSaving(true)
    setAddressError('')
    setAddressSuccess('')

    try {
      await updateAddress(
        address.id,
        buildAddressPayload({
          recipientName: address.recipientName,
          phoneNumber: address.phoneNumber,
          addressLine: address.addressLine,
          provinceCode: address.provinceCode,
          wardCode: address.wardCode,
          shippingNote: address.shippingNote,
          isDefault: true,
        })
      )
      await reloadAddresses()
      setAddressSuccess('Đã đặt địa chỉ mặc định.')
    } catch {
      setAddressError('Không thể đặt địa chỉ mặc định. Vui lòng thử lại.')
    } finally {
      setAddressSaving(false)
    }
  }

  if (loading) {
    return <LoadingSkeleton />
  }

  return (
    <div className="min-h-screen bg-background text-primary">
      <header className="flex items-center justify-between px-6 py-5 md:px-8">
        <a href="/" className="font-display-lg tracking-tighter text-primary no-underline">
          StyleMind
        </a>
        <span className="text-[11px] font-medium uppercase tracking-[0.18em] text-on-surface-variant">
          Tài khoản
        </span>
      </header>

      <main className="mx-auto max-w-6xl px-4 pb-16 pt-4 sm:px-6 lg:px-8">
        <section className="rounded-[1.75rem] border border-outline-variant/20 bg-surface-container-lowest p-6 shadow-[0_20px_40px_-28px_rgba(15,23,42,0.16)]">
          <div className="grid gap-6 lg:grid-cols-[1fr_auto] lg:items-center">
            <div className="flex items-center gap-4">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-[1.25rem] border border-outline-variant/20 bg-surface-container-low text-lg font-semibold text-primary">
                {getInitials(profileDisplayName)}
              </div>
              <div className="min-w-0">
                <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-on-surface-variant">
                  Hồ sơ cá nhân
                </p>
                <h1 className="mt-2 text-3xl tracking-tight text-primary md:text-4xl">
                  {profileHeadingName}
                </h1>
                <div className="mt-3 flex items-center gap-2 text-sm text-on-surface-variant">
                  <Mail size={15} className="shrink-0 text-primary" />
                  <span className="truncate">{profileEmail}</span>
                </div>
              </div>
            </div>

            <div className="rounded-[1.25rem] border border-outline-variant/20 bg-surface-container-low px-4 py-3">
              <p className="text-[11px] font-medium uppercase tracking-[0.18em] text-on-surface-variant">
                Địa chỉ mặc định
              </p>
              <div className="mt-2 flex items-start gap-2 text-sm text-primary">
                <MapPin size={15} className="mt-0.5 shrink-0 text-primary" />
                <span className="max-w-[28ch]">{defaultAddress ? formatAddressLine(defaultAddress) : 'Chưa có địa chỉ mặc định'}</span>
              </div>
            </div>
          </div>
        </section>

        <div className="mt-6 rounded-full border border-outline-variant/20 bg-surface-container-lowest p-1">
          <div className="flex gap-1">
            {tabs.map((tab) => (
              <PillTab
                key={tab.id}
                active={activeTab === tab.id}
                label={tab.label}
                onClick={() => setActiveTab(tab.id)}
              />
            ))}
          </div>
        </div>

        <AnimatePresence mode="wait" initial={false}>
          {activeTab === 'style' ? (
            <motion.section
              key="style"
              initial={reduceMotion ? { opacity: 1 } : { opacity: 0, y: 12 }}
              animate={reduceMotion ? { opacity: 1 } : { opacity: 1, y: 0 }}
              exit={reduceMotion ? { opacity: 1 } : { opacity: 0, y: -8 }}
              transition={tabTransition}
              className="mt-6 space-y-6"
            >
              <SectionCard
                eyebrow="Hồ sơ phong cách"
                title="Điều chỉnh cách StyleMind hiểu gu mặc của bạn"
                description="Chỉ giữ những lựa chọn thật sự phản ánh phong cách hiện tại của bạn. Các mục dưới đây có thể thay đổi bất cứ lúc nào."
              />

              <div className="grid gap-6 lg:grid-cols-[1fr_1fr]">
                <div className="space-y-6">
                  <div className="rounded-[1.5rem] border border-outline-variant/20 bg-surface-container-lowest p-6">
                    <label className="mb-2 block text-sm font-medium text-primary">
                      Tên hiển thị
                    </label>
                    <input
                      value={displayName}
                      onChange={(event) => {
                        setDisplayName(event.target.value)
                        setProfileSuccess('')
                      }}
                      maxLength={150}
                      className="w-full rounded-2xl border border-outline-variant/20 bg-transparent px-4 py-3 text-sm text-primary outline-none transition-colors placeholder:text-on-surface-variant/55 focus:border-primary"
                      placeholder="StyleMind nên gọi bạn như thế nào?"
                    />
                  </div>

                  <div className="rounded-[1.5rem] border border-outline-variant/20 bg-surface-container-lowest p-6">
                    <div className="mb-4">
                      <h3 className="text-sm font-medium text-primary">Vóc dáng</h3>
                      <p className="mt-1 text-xs text-on-surface-variant">
                        Chọn kiểu vóc dáng gần nhất với bạn.
                      </p>
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      {bodyTypes.map((type) => (
                        <button
                          key={type.id}
                          type="button"
                          onClick={() => {
                            setSelectedBodyType(type.id)
                            setProfileSuccess('')
                          }}
                          className={clsx(
                            'flex min-h-[150px] flex-col items-center justify-center gap-3 rounded-[1.25rem] border px-3 py-4 text-center transition-all',
                            selectedBodyType === type.id
                              ? 'border-primary bg-surface-container-low text-primary'
                              : 'border-outline-variant/20 bg-surface text-on-surface-variant hover:border-outline-variant/40'
                          )}
                        >
                          <span className="material-symbols-outlined text-4xl text-primary">
                            {type.icon}
                          </span>
                          <span className="text-base font-medium text-primary">{type.label}</span>
                          <span className="text-xs leading-relaxed text-on-surface-variant">
                            {type.description}
                          </span>
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="rounded-[1.5rem] border border-outline-variant/20 bg-surface-container-lowest p-6">
                    <div className="mb-4">
                      <h3 className="text-sm font-medium text-primary">Phom dáng</h3>
                      <p className="mt-1 text-xs text-on-surface-variant">
                        Chọn phom mặc khiến bạn thoải mái nhất.
                      </p>
                    </div>
                    <div className="grid grid-cols-2 gap-3">
                      {fits.map((fit) => (
                        <button
                          key={fit.id}
                          type="button"
                          onClick={() => {
                            setSelectedFit(fit.id)
                            setProfileSuccess('')
                          }}
                          className={clsx(
                            'rounded-[1.25rem] border px-4 py-4 text-center text-sm font-medium transition-all',
                            selectedFit === fit.id
                              ? 'border-primary bg-surface-container-low text-primary'
                              : 'border-outline-variant/20 text-on-surface-variant hover:border-outline-variant/40'
                          )}
                        >
                          {fit.label}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>

                <div className="space-y-6">
                  <div className="rounded-[1.5rem] border border-outline-variant/20 bg-surface-container-lowest p-6">
                    <div className="mb-4">
                      <h3 className="text-sm font-medium text-primary">Phong cách yêu thích</h3>
                      <p className="mt-1 text-xs text-on-surface-variant">
                        Chọn các phong cách bạn thường muốn nhận gợi ý.
                      </p>
                    </div>
                    <div className="grid grid-cols-2 gap-3 md:grid-cols-3">
                      {styleOptions.map((style) => (
                        <button
                          key={style.value}
                          type="button"
                          onClick={() => toggleStyle(style.value)}
                          className={clsx(
                            'rounded-[1.15rem] border px-4 py-4 text-sm font-medium transition-all',
                            selectedStyles.includes(style.value)
                              ? 'border-primary bg-surface-container-low text-primary'
                              : 'border-outline-variant/20 text-on-surface-variant hover:border-outline-variant/40'
                          )}
                        >
                          {style.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="rounded-[1.5rem] border border-outline-variant/20 bg-surface-container-lowest p-6">
                    <div className="mb-4">
                      <h3 className="text-sm font-medium text-primary">Màu sắc yêu thích</h3>
                      <p className="mt-1 text-xs text-on-surface-variant">
                        Chọn những tông màu bạn thường mặc.
                      </p>
                    </div>
                    <div className="flex flex-wrap gap-3">
                      {colors.map((color) => (
                        <button
                          key={color.value}
                          type="button"
                          onClick={() => toggleColor(color.value)}
                          className={clsx(
                            'rounded-full border px-4 py-2.5 text-sm font-medium transition-all',
                            selectedColors.includes(color.value)
                              ? 'border-primary bg-surface-container-low text-primary'
                              : 'border-outline-variant/20 text-on-surface-variant hover:border-outline-variant/40'
                          )}
                        >
                          {color.label}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="flex flex-col gap-3 rounded-[1.5rem] border border-outline-variant/20 bg-surface-container-low p-5 sm:flex-row sm:items-center sm:justify-between">
                    <div className="space-y-1">
                      <p className="text-xs uppercase tracking-[0.18em] text-on-surface-variant">
                        Thay đổi hồ sơ
                      </p>
                      <p className="text-sm text-on-surface-variant">
                        Lưu các lựa chọn phong cách sau khi chỉnh sửa xong.
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={completeProfile}
                      disabled={savingProfile}
                      className="inline-flex items-center justify-center gap-2 rounded-full bg-primary px-5 py-3 text-sm font-medium text-on-primary transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-70"
                    >
                      {savingProfile && <Loader2 size={14} className="animate-spin" />}
                      Lưu thay đổi
                    </button>
                  </div>

                  {profileSuccess && (
                    <p role="status" className="text-sm text-green-status">
                      {profileSuccess}
                    </p>
                  )}
                  {profileError && (
                    <p role="alert" className="text-sm text-error">
                      {profileError}
                    </p>
                  )}
                </div>
              </div>
            </motion.section>
          ) : (
            <motion.section
              key="addresses"
              initial={reduceMotion ? { opacity: 1 } : { opacity: 0, y: 12 }}
              animate={reduceMotion ? { opacity: 1 } : { opacity: 1, y: 0 }}
              exit={reduceMotion ? { opacity: 1 } : { opacity: 0, y: -8 }}
              transition={tabTransition}
              className="mt-6 space-y-6"
            >
              <SectionCard
                eyebrow="Địa chỉ giao hàng"
                title="Quản lý địa chỉ nhận hàng"
                description="Lưu nhiều địa chỉ, chọn địa chỉ mặc định và sửa nhanh khi bạn đổi nơi nhận hàng."
                action={
                  <button
                    type="button"
                    onClick={openCreateAddressForm}
                    className={clsx(
                      'inline-flex items-center justify-center gap-2 rounded-full border px-4 py-2.5 text-sm font-medium transition-colors',
                      addressFormOpen && editingAddressId === null
                        ? 'border-primary bg-primary text-on-primary'
                        : 'border-outline-variant/20 text-primary hover:bg-surface-container-high'
                    )}
                  >
                    <Plus size={14} />
                    {addressFormOpen && editingAddressId === null ? 'Đóng' : 'Thêm địa chỉ'}
                  </button>
                }
              />

              <div className="space-y-4">
                {addressLoading ? (
                  <div className="rounded-[1.5rem] border border-outline-variant/20 bg-surface-container-lowest p-6 text-sm text-on-surface-variant">
                    Đang tải địa chỉ...
                  </div>
                ) : addresses.length === 0 ? (
                  <div className="rounded-[1.5rem] border border-dashed border-outline-variant/25 bg-surface-container-lowest p-8 text-center">
                    <MapPin size={28} className="mx-auto mb-3 text-on-surface-variant/40" />
                    <p className="text-sm text-on-surface-variant">Chưa có địa chỉ giao hàng nào.</p>
                    <p className="mt-1 text-xs text-on-surface-variant/80">
                      Thêm một địa chỉ để thanh toán và giao hàng thuận tiện hơn.
                    </p>
                  </div>
                ) : (
                  addresses.map((address) => (
                    <article
                      key={address.id}
                      className="rounded-[1.5rem] border border-outline-variant/20 bg-surface-container-lowest p-5 transition-colors hover:border-outline-variant/40"
                    >
                      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                        <div className="space-y-2">
                          <div className="flex items-center gap-2">
                            <span className="text-base font-medium text-primary">{address.recipientName}</span>
                            {address.isDefault && (
                              <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-1 text-[11px] font-medium text-primary">
                                <Star size={10} />
                                Mặc định
                              </span>
                            )}
                          </div>
                          <p className="text-sm text-on-surface-variant">{address.phoneNumber}</p>
                          <p className="text-sm text-on-surface-variant">{formatAddressLine(address)}</p>
                          {address.validationStatus !== 'VALID' && (
                            <p className="text-xs font-medium text-error">
                              Cần cập nhật địa chỉ trước khi thanh toán.
                            </p>
                          )}
                        </div>

                        <div className="flex flex-wrap items-center gap-2">
                          {!address.isDefault && (
                            <button
                              type="button"
                              onClick={() => handleSetDefault(address)}
                              disabled={addressSaving}
                              className="inline-flex items-center gap-1.5 rounded-full border border-outline-variant/20 px-3 py-2 text-xs font-medium text-primary transition-colors hover:bg-surface-container-high disabled:cursor-not-allowed disabled:opacity-60"
                            >
                              <Home size={12} />
                              Đặt mặc định
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => openEditAddressForm(address)}
                            className={clsx(
                              'inline-flex items-center gap-1.5 rounded-full border px-3 py-2 text-xs font-medium transition-colors',
                              addressFormOpen && editingAddressId === address.id
                                ? 'border-primary bg-primary text-on-primary'
                                : 'border-outline-variant/20 text-primary hover:bg-surface-container-high'
                            )}
                          >
                            <PencilLine size={12} />
                            Chỉnh sửa
                          </button>
                          <button
                            type="button"
                            onClick={() => requestDeleteAddress(address)}
                            disabled={addressSaving}
                            className={clsx(
                              'inline-flex items-center gap-1.5 rounded-full border px-3 py-2 text-xs font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-60',
                              pendingDeleteAddress?.id === address.id
                                ? 'border-error bg-error-container/20 text-error'
                                : 'border-error/20 text-error hover:bg-error-container/20'
                            )}
                          >
                            <Trash2 size={12} />
                            Xóa
                          </button>
                        </div>
                      </div>
                    </article>
                  ))
                )}
              </div>

              <AnimatePresence mode="wait">
                {pendingDeleteAddress && (
                  <Modal
                    isOpen
                    onClose={closeDeleteDialog}
                    title="Xóa địa chỉ giao hàng?"
                    className="max-w-xl"
                  >
                    <div className="space-y-4">
                      <p className="text-sm leading-relaxed text-on-surface-variant">
                        Địa chỉ này sẽ bị xóa khỏi hồ sơ của bạn. Bạn vẫn có thể thêm lại địa chỉ mới sau.
                      </p>
                      {addressDeleteError && (
                        <p role="alert" className="text-sm text-error">
                          {addressDeleteError}
                        </p>
                      )}
                      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                        <button
                          type="button"
                          onClick={closeDeleteDialog}
                          disabled={addressSaving}
                          className="rounded-full border border-outline-variant/20 px-4 py-2 text-sm font-medium text-primary transition-colors hover:bg-surface-container-high disabled:cursor-not-allowed disabled:opacity-60"
                        >
                          Hủy
                        </button>
                        <button
                          type="button"
                          onClick={confirmDeleteAddress}
                          disabled={addressSaving}
                          className="inline-flex items-center justify-center gap-2 rounded-full bg-error px-4 py-2 text-sm font-medium text-white transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-70"
                        >
                          {addressSaving && <Loader2 size={14} className="animate-spin" />}
                          {addressSaving ? 'Đang xóa...' : 'Xóa địa chỉ'}
                        </button>
                      </div>
                    </div>
                  </Modal>
                )}
              </AnimatePresence>

              <AnimatePresence mode="wait">
                {addressFormOpen && (
                  <motion.form
                    key={editingAddressId ? `edit-${editingAddressId}` : 'create-address'}
                    onSubmit={handleAddressSubmit}
                    initial={reduceMotion ? { opacity: 1 } : { opacity: 0, y: 12 }}
                    animate={reduceMotion ? { opacity: 1 } : { opacity: 1, y: 0 }}
                    exit={reduceMotion ? { opacity: 1 } : { opacity: 0, y: -8 }}
                    transition={tabTransition}
                    className="rounded-[1.75rem] border border-outline-variant/20 bg-surface-container-lowest p-6 shadow-[0_20px_40px_-28px_rgba(15,23,42,0.16)]"
                  >
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                      <div>
                        <h3 className="text-xl font-semibold tracking-tight text-primary">
                          {editingAddressId ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ'}
                        </h3>
                        <p className="mt-2 text-sm text-on-surface-variant">
                          {editingAddressId
                            ? 'Chỉnh lại thông tin nhận hàng hoặc đổi địa chỉ mặc định.'
                            : 'Lưu một địa chỉ mới để dùng cho các đơn hàng tiếp theo.'}
                        </p>
                      </div>
                      <button
                        type="button"
                        onClick={requestCloseAddressForm}
                        className="rounded-full border border-outline-variant/20 px-4 py-2 text-sm font-medium text-primary transition-colors hover:bg-surface-container-high"
                      >
                        Hủy
                      </button>
                    </div>

                    <div className="mt-6 grid gap-4 md:grid-cols-2">
                      <div className="space-y-2 md:col-span-1">
                        <label className="block text-sm font-medium text-primary">Tên người nhận</label>
                        <input
                          value={addressForm.recipientName}
                          onChange={(event) =>
                            setAddressForm((current) => ({ ...current, recipientName: event.target.value }))
                          }
                          className="w-full rounded-2xl border border-outline-variant/20 bg-transparent px-4 py-3 text-sm text-primary outline-none transition-colors placeholder:text-on-surface-variant/55 focus:border-primary"
                          placeholder="Ví dụ: Nguyễn Minh Khôi"
                        />
                      </div>

                      <div className="space-y-2 md:col-span-1">
                        <label className="block text-sm font-medium text-primary">Số điện thoại người nhận</label>
                        <input
                          value={addressForm.phoneNumber}
                          onChange={(event) =>
                            setAddressForm((current) => ({ ...current, phoneNumber: event.target.value }))
                          }
                          className="w-full rounded-2xl border border-outline-variant/20 bg-transparent px-4 py-3 text-sm text-primary outline-none transition-colors placeholder:text-on-surface-variant/55 focus:border-primary"
                          placeholder="Ví dụ: 09xxxxxxxx"
                        />
                      </div>

                      <div className="space-y-2 md:col-span-2">
                        <label className="block text-sm font-medium text-primary">Địa chỉ giao hàng</label>
                        <textarea
                          rows={4}
                          value={addressForm.addressLine}
                          onChange={(event) =>
                            setAddressForm((current) => ({ ...current, addressLine: event.target.value }))
                          }
                          className="w-full rounded-2xl border border-outline-variant/20 bg-transparent px-4 py-3 text-sm text-primary outline-none transition-colors placeholder:text-on-surface-variant/55 focus:border-primary"
                          placeholder="Số nhà, tên đường, phường/xã..."
                        />
                      </div>

                      <div className="space-y-2 md:col-span-1">
                        <label htmlFor="address-province" className="block text-sm font-medium text-primary">Tỉnh/thành phố</label>
                        <select
                          id="address-province"
                          value={addressForm.provinceCode}
                          disabled={administrativeLoading || addressSaving}
                          onChange={(event) => setAddressForm((current) => ({
                            ...current,
                            provinceCode: event.target.value,
                            wardCode: '',
                          }))}
                          className="w-full rounded-2xl border border-outline-variant/20 bg-transparent px-4 py-3 text-sm text-primary outline-none transition-colors focus:border-primary"
                        >
                          <option value="">Chọn tỉnh/thành phố</option>
                          {provinces.map((province) => (
                            <option key={province.code} value={province.code}>{province.name}</option>
                          ))}
                        </select>
                      </div>

                      <div className="space-y-2 md:col-span-1">
                        <label htmlFor="address-ward" className="block text-sm font-medium text-primary">Phường/xã</label>
                        <select
                          id="address-ward"
                          value={addressForm.wardCode}
                          disabled={!addressForm.provinceCode || administrativeLoading || addressSaving}
                          onChange={(event) => setAddressForm((current) => ({ ...current, wardCode: event.target.value }))}
                          className="w-full rounded-2xl border border-outline-variant/20 bg-transparent px-4 py-3 text-sm text-primary outline-none transition-colors focus:border-primary"
                        >
                          <option value="">Chọn phường/xã</option>
                          {wards.map((ward) => (
                            <option key={ward.code} value={ward.code}>{ward.name}</option>
                          ))}
                        </select>
                      </div>

                      <div className="space-y-2 md:col-span-2">
                        <label htmlFor="shipping-note" className="block text-sm font-medium text-primary">Ghi chú giao hàng (không bắt buộc)</label>
                        <input
                          id="shipping-note"
                          value={addressForm.shippingNote}
                          onChange={(event) => setAddressForm((current) => ({ ...current, shippingNote: event.target.value }))}
                          className="w-full rounded-2xl border border-outline-variant/20 bg-transparent px-4 py-3 text-sm text-primary outline-none transition-colors placeholder:text-on-surface-variant/55 focus:border-primary"
                          placeholder="Ví dụ: Gọi trước khi giao"
                        />
                      </div>

                      <label className="flex items-center gap-3 rounded-2xl border border-outline-variant/20 px-4 py-3 text-sm text-primary md:col-span-1">
                        <input
                          type="checkbox"
                          checked={addressForm.isDefault}
                          onChange={(event) =>
                            setAddressForm((current) => ({ ...current, isDefault: event.target.checked }))
                          }
                          className="h-4 w-4 rounded border-outline-variant/30 text-primary focus:ring-primary"
                        />
                        Đặt làm địa chỉ mặc định
                      </label>
                    </div>

                    <div className="mt-6 flex flex-wrap items-center justify-between gap-3">
                      <div className="flex items-center gap-3">
                        <button
                          type="submit"
                          disabled={addressSaving}
                          className="inline-flex items-center gap-2 rounded-full bg-primary px-5 py-3 text-sm font-medium text-on-primary transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-70"
                        >
                          {addressSaving && <Loader2 size={14} className="animate-spin" />}
                          {editingAddressId ? 'Cập nhật địa chỉ' : 'Lưu địa chỉ'}
                        </button>
                      </div>
                    </div>
                  </motion.form>
                )}
              </AnimatePresence>

              <AnimatePresence>
                {unsavedChangeConfirmOpen && (
                  <Modal
                    isOpen
                    onClose={keepEditingAddressForm}
                    title="Hủy thay đổi?"
                    className="max-w-xl"
                  >
                    <div className="space-y-4">
                      <p className="text-sm leading-relaxed text-on-surface-variant">
                        Những thông tin bạn vừa nhập sẽ không được lưu.
                      </p>
                      <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                        <button
                          type="button"
                          onClick={keepEditingAddressForm}
                          className="rounded-full border border-outline-variant/20 px-4 py-2 text-sm font-medium text-primary transition-colors hover:bg-surface-container-high"
                        >
                          Tiếp tục chỉnh sửa
                        </button>
                        <button
                          type="button"
                          onClick={discardAddressFormChanges}
                          className="rounded-full bg-error px-4 py-2 text-sm font-medium text-white transition-opacity hover:opacity-90"
                        >
                          Hủy thay đổi
                        </button>
                      </div>
                    </div>
                  </Modal>
                )}
              </AnimatePresence>

              {addressSuccess && (
                <p role="status" className="text-sm text-green-status">
                  {addressSuccess}
                </p>
              )}
              {addressError && (
                <p role="alert" className="text-sm text-error">
                  {addressError}
                </p>
              )}
            </motion.section>
          )}
        </AnimatePresence>
      </main>
    </div>
  )
}
