/**
 * Danh sách 63 Tỉnh / Thành phố trực thuộc Trung ương của Việt Nam (Open-source Dataset)
 */
export const VIETNAM_PROVINCES = [
  'An Giang', 'Bà Rịa - Vũng Tàu', 'Bắc Giang', 'Bắc Kạn', 'Bạc Liêu',
  'Bắc Ninh', 'Bến Tre', 'Bình Định', 'Bình Dương', 'Bình Phước',
  'Bình Thuận', 'Cà Mau', 'Cần Thơ', 'Cao Bằng', 'Đà Nẵng',
  'Đắk Lắk', 'Đắk Nông', 'Điện Biên', 'Đồng Nai', 'Đồng Tháp',
  'Gia Lai', 'Hà Giang', 'Hà Nam', 'Hà Nội', 'Hà Tĩnh',
  'Hải Dương', 'Hải Phòng', 'Hậu Giang', 'Hòa Bình', 'Hưng Yên',
  'Khánh Hòa', 'Kiên Giang', 'Kon Tum', 'Lai Châu', 'Lâm Đồng',
  'Lạng Sơn', 'Lào Cai', 'Long An', 'Nam Định', 'Nghệ An',
  'Ninh Bình', 'Ninh Thuận', 'Phú Thọ', 'Phú Yên', 'Quảng Bình',
  'Quảng Nam', 'Quảng Ngãi', 'Quảng Ninh', 'Quảng Trị', 'Sóc Trăng',
  'Sơn La', 'Tây Ninh', 'Thái Bình', 'Thái Nguyên', 'Thanh Hóa',
  'Thừa Thiên Huế', 'Tiền Giang', 'TP. Hồ Chí Minh', 'Trà Vinh', 'Tuyên Quang',
  'Vĩnh Long', 'Vĩnh Phúc', 'Yên Bái'
]

/**
 * Kiểm tra Định dạng Số điện thoại Việt Nam (10 chữ số, các đầu số nhà mạng VN hoặc +84)
 * @param {string} phone 
 * @returns {boolean}
 */
export function validateVietnamesePhone(phone) {
  if (!phone || typeof phone !== 'string') return false
  const cleaned = phone.replace(/[\s\-\.]/g, '')
  const vnPhoneRegex = /^(?:(?:\+84)|0)(?:3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$/
  return vnPhoneRegex.test(cleaned)
}

/**
 * Chuẩn hóa chuỗi tiếng Việt bỏ dấu
 */
function normalizeText(text) {
  if (!text) return ''
  return text
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'd')
}

/**
 * Kiểm tra Tỉnh / Thành phố có thuộc 63 Tỉnh Thành Việt Nam hay không
 * @param {string} city 
 * @returns {boolean}
 */
export function validateVietnameseCity(city) {
  if (!city || typeof city !== 'string') return false
  const normalizedInput = normalizeText(city)
    .replace(/^(tinh|thanh pho|tp\.)\s*/, '')
    .trim()

  return VIETNAM_PROVINCES.some((prov) => {
    const normalizedProv = normalizeText(prov)
      .replace(/^(tinh|thanh pho|tp\.)\s*/, '')
      .trim()
    return normalizedInput === normalizedProv || normalizedInput.includes(normalizedProv) || normalizedProv.includes(normalizedInput)
  })
}
