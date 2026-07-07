# Functional Requirements (traceability)

Danh sách mã FR để trace. Chi tiết endpoint xem `api/01-api-catalog.md`; rule chi tiết xem `services/`.

## Authentication
| ID | Requirement | Priority |
|---|---|---|
| AUTH-01 | Đăng ký email/password (gửi OTP xác thực email, account chỉ tạo sau khi verify OTP) | Must |
| AUTH-01a | Verify register OTP → tạo account ACTIVE | Must |
| AUTH-01b | Resend register OTP (có cooldown) | Must |
| AUTH-02 | Đăng nhập | Must |
| AUTH-03 | Cấp JWT sau login | Must |
| AUTH-04 | API user hiện tại | Must |
| AUTH-05 | Forgot password | Must |
| AUTH-06 | Verify OTP/reset token | Must |
| AUTH-07 | Reset password | Must |
| AUTH-08 | Disabled user không login | Must |
| AUTH-09 | Admin APIs yêu cầu ADMIN | Must |

## Admin Account
| ID | Requirement | Priority |
|---|---|---|
| ADM-ACC-01 | Xem danh sách account | Must |
| ADM-ACC-02 | Search/filter keyword,role,status | Must |
| ADM-ACC-03 | Tạo account | Must |
| ADM-ACC-04 | Khóa/mở khóa | Must |
| ADM-ACC-05 | Cập nhật role | Should |
| ADM-ACC-06 | Không expose password/reset token | Must |
| ADM-SELF-01 | Không tự disable/lock | Must |
| ADM-SELF-02 | Không tự đổi role | Must |
| ADM-SELF-03 | Không tự xóa | Must |
| ADM-SELF-04 | Bảo vệ admin cuối cùng | Must |
| ADM-SELF-05 | Audit log hành động nhạy cảm | Should |
| ADM-DASH-01 | Admin dashboard hiển thị số liệu **thật** (orders/revenue/products/users/notifications) qua per-service `/admin/**/summary`, compose ở FE; không dùng dữ liệu giả, có loading/error/empty | Should |
| ADM-DASH-02 | Summary API dưới `/api/v1/admin/**`, yêu cầu ADMIN; chỉ trả count/sum, không lộ trường nhạy cảm | Must |

## User Profile
| ID | Requirement | Priority |
|---|---|---|
| USER-01 | Xem profile/style profile | Must |
| USER-02 | Cập nhật style profile | Must |
| USER-03 | Quản lý địa chỉ | Must |
| USER-04 | Một default address | Must |
| USER-05 | Lazy-init profile theo userId | Should |

## Product
| ID | Requirement | Priority |
|---|---|---|
| PROD-01..02 | Public xem list/detail ACTIVE | Must |
| PROD-03 | Search/category/targetDemographic/price/sort/pagination; chỉ dữ liệu thật | Must |
| PROD-03a | Product card cart icon mở product detail để chọn variant, không quick-add trực tiếp từ card | Must |
| PROD-04 | Admin CRUD category; chặn xóa category đang dùng/có child bằng `409` | Must |
| PROD-05 | Admin create/update/deactivate product; product được tạo trước subresources | Must |
| PROD-06 | Admin quản lý variants với unique SKU; mỗi variant là tổ hợp size/color/material với `stockQuantity`/`active` riêng; chặn trùng tổ hợp size+color+material cho cùng product | Must |
| PROD-07 | Admin upload/replace images qua backend Cloudinary; validate type/10 MB; lưu URL/public ID | Should |
| PROD-08 | Internal variant snapshot (giá authoritative) | Must |
| PROD-09 | Add Product dùng flow Product Info → Variants → Images / Finish; Step 1 luôn tạo INACTIVE và giữ nguyên create API/`ProductResponse` | Must |
| PROD-10 | Product chỉ được ACTIVE khi có variant; chặn xóa final variant của ACTIVE product bằng `409 LAST_ACTIVE_VARIANT` | Must |
| PROD-11 | Public list/detail/variants không trả product thiếu variant, kể cả dữ liệu legacy mang trạng thái ACTIVE | Must |
| PROD-12 | Admin product actions map backend/auth/network errors to friendly actionable UI; recoverable errors preserve drawer/form state, duplicate SKU marks SKU, and image upload failure does not rollback product | Must |

## Cart
| ID | Requirement | Priority |
|---|---|---|
| CART-01..02 | Guest & auth cart | Must |
| CART-03..05 | Add/update/remove | Must |
| CART-06 | Merge sau login: cùng variant cộng dồn quantity (không tạo dòng trùng); guest session id chỉ xóa khi merge thành công, giữ lại và báo lỗi thân thiện nếu merge thất bại | Must |
| CART-07 | Clear sau checkout | Must |
| CART-08 | Track AI recommended item | Should |
| CART-09 | Cart item hiển thị tên/ảnh/size/color/material/giá thật lấy từ product-service, không dùng placeholder cho item hợp lệ; item mất/ngừng bán hiển thị trạng thái "không còn khả dụng" thay vì giá "$0" | Must |
| CART-10 | Thêm vào giỏ chặn variant hết hàng (`stockQuantity=0`) hoặc bị vô hiệu hóa (`active=false`), trả `VARIANT_OUT_OF_STOCK` với thông báo "Biến thể này đã hết hàng." | Must |

## Order
| ID | Requirement | Priority |
|---|---|---|
| ORDER-01..03 | Tạo/list/detail của customer | Must |
| ORDER-04..05 | Giá từ product-service; lưu price_at_purchase | Must |
| ORDER-06 | Lưu AI conversion | Should |
| ORDER-07..09 | Admin list/detail/đổi trạng thái (state machine) | Must |
| ORDER-10 | Checkout success clear cart | Must |
| ORDER-11 | SePay quá hạn → EXPIRED/CANCELLED | Must |

## Payment
| ID | Requirement | Priority |
|---|---|---|
| PAY-01 | COD | Must |
| PAY-02 | SePay VietQR | Must |
| PAY-03 | Transaction log | Must |
| PAY-04 | VietQR với nội dung CK duy nhất | Must |
| PAY-05 | Webhook SePay (verify + idempotent) | Must |
| PAY-06 | Báo lại order-service khi đổi trạng thái | Must |
| PAY-07 | Admin xem payment logs | Should |

## Notification
| ID | Requirement | Priority |
|---|---|---|
| NOTI-01 | Internal tạo log | Must |
| NOTI-02 | Customer xem notification | Should |
| NOTI-03 | Admin xem logs | Must |
| NOTI-04 | Admin retry failed | Should |

## AI Stylist
| ID | Requirement | Priority |
|---|---|---|
| AI-01..02 | Chat + gợi ý sản phẩm/outfit | Must |
| AI-03 | Lưu chat history | Should |
| AI-04 | Add to cart từ recommendation | Should |
| AI-05 | Admin quản lý AI index jobs | Must |
