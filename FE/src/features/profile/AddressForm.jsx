export function createEmptyAddressForm() {
  return {
    recipientName: '',
    phoneNumber: '',
    provinceCode: '',
    wardCode: '',
    addressLine: '',
    shippingNote: '',
    isDefault: false,
  }
}

export function addressToForm(address) {
  return {
    recipientName: address?.recipientName || '',
    phoneNumber: address?.phoneNumber || '',
    provinceCode: address?.provinceCode || '',
    wardCode: address?.wardCode || '',
    addressLine: address?.addressLine || '',
    shippingNote: address?.shippingNote || '',
    isDefault: Boolean(address?.isDefault),
  }
}

export default function AddressForm({
  form,
  provinces,
  wards,
  loadingAdministrativeData,
  submitting,
  submitLabel = 'Lưu địa chỉ',
  showDefaultOption = true,
  onChange,
  onSubmit,
  onCancel,
}) {
  const update = (field, value) => onChange({ ...form, [field]: value })

  return (
    <form className="space-y-4" onSubmit={onSubmit} noValidate>
      <div>
        <label htmlFor="address-recipient" className="mb-1.5 block text-sm font-medium text-primary">Người nhận</label>
        <input
          id="address-recipient"
          value={form.recipientName}
          onChange={(event) => update('recipientName', event.target.value)}
          placeholder="Ví dụ: Nguyễn Minh Khôi"
          maxLength={100}
          required
          className="w-full rounded-lg border border-outline-variant/30 bg-transparent px-3 py-2.5 text-sm text-primary outline-none focus:border-primary"
        />
      </div>
      <div>
        <label htmlFor="address-phone" className="mb-1.5 block text-sm font-medium text-primary">Số điện thoại</label>
        <input
          id="address-phone"
          value={form.phoneNumber}
          onChange={(event) => update('phoneNumber', event.target.value)}
          placeholder="Ví dụ: 09xxxxxxxx"
          inputMode="tel"
          maxLength={20}
          required
          className="w-full rounded-lg border border-outline-variant/30 bg-transparent px-3 py-2.5 text-sm text-primary outline-none focus:border-primary"
        />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label htmlFor="address-province" className="mb-1.5 block text-sm font-medium text-primary">Tỉnh/thành phố</label>
          <select
            id="address-province"
            value={form.provinceCode}
            onChange={(event) => onChange({ ...form, provinceCode: event.target.value, wardCode: '' })}
            disabled={loadingAdministrativeData}
            required
            className="w-full rounded-lg border border-outline-variant/30 bg-surface-container-lowest px-3 py-2.5 text-sm text-primary outline-none focus:border-primary disabled:cursor-not-allowed disabled:opacity-60"
          >
            <option value="">Chọn tỉnh/thành phố</option>
            {provinces.map((province) => <option key={province.code} value={province.code}>{province.name}</option>)}
          </select>
        </div>
        <div>
          <label htmlFor="address-ward" className="mb-1.5 block text-sm font-medium text-primary">Phường/xã</label>
          <select
            id="address-ward"
            value={form.wardCode}
            onChange={(event) => update('wardCode', event.target.value)}
            disabled={!form.provinceCode || loadingAdministrativeData}
            required
            className="w-full rounded-lg border border-outline-variant/30 bg-surface-container-lowest px-3 py-2.5 text-sm text-primary outline-none focus:border-primary disabled:cursor-not-allowed disabled:opacity-60"
          >
            <option value="">Chọn phường/xã</option>
            {wards.map((ward) => <option key={ward.code} value={ward.code}>{ward.name}</option>)}
          </select>
        </div>
      </div>
      <div>
        <label htmlFor="address-line" className="mb-1.5 block text-sm font-medium text-primary">Địa chỉ giao hàng</label>
        <textarea
          id="address-line"
          value={form.addressLine}
          onChange={(event) => update('addressLine', event.target.value)}
          placeholder="Số nhà, tên đường, phường/xã..."
          rows={3}
          required
          className="w-full resize-y rounded-lg border border-outline-variant/30 bg-transparent px-3 py-2.5 text-sm text-primary outline-none focus:border-primary"
        />
      </div>
      <div>
        <label htmlFor="address-note" className="mb-1.5 block text-sm font-medium text-primary">Ghi chú giao hàng <span className="text-on-surface-variant">(không bắt buộc)</span></label>
        <textarea
          id="address-note"
          value={form.shippingNote}
          onChange={(event) => update('shippingNote', event.target.value)}
          rows={2}
          className="w-full resize-y rounded-lg border border-outline-variant/30 bg-transparent px-3 py-2.5 text-sm text-primary outline-none focus:border-primary"
        />
      </div>
      {showDefaultOption && (
        <label className="flex items-center gap-2 text-sm text-primary">
          <input
            type="checkbox"
            checked={form.isDefault}
            onChange={(event) => update('isDefault', event.target.checked)}
            className="h-4 w-4 rounded border-outline-variant text-primary focus:ring-primary"
          />
          Đặt làm địa chỉ mặc định
        </label>
      )}
      <div className="flex justify-end gap-3 border-t border-outline-variant/15 pt-4">
        {onCancel && <button type="button" onClick={onCancel} disabled={submitting} className="rounded-lg px-4 py-2 text-sm font-medium text-on-surface-variant hover:bg-surface-container-high disabled:opacity-60">Hủy</button>}
        <button type="submit" disabled={submitting || loadingAdministrativeData} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-on-primary hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60">
          {submitting ? 'Đang lưu...' : submitLabel}
        </button>
      </div>
    </form>
  )
}
