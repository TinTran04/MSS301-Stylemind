# Security Requirements

| ID | Requirement | Priority |
|---|---|---|
| SEC-01 | Gateway validate JWT | Must |
| SEC-02 | Admin APIs yêu cầu ADMIN | Must |
| SEC-03 | CUSTOMER không gọi `/api/v1/admin/**` | Must |
| SEC-04 | Frontend không gọi `/internal/v1/**` | Must |
| SEC-05 | Internal APIs yêu cầu `X-Internal-Token` | Must |
| SEC-06 | Webhook SePay verify (API key/chữ ký) trước khi xử lý | Must |
| SEC-07 | Admin self-protection (self & last-admin) enforce ở backend | Must |
| SEC-08 | Không log password/OTP/reset token/secret SePay | Must |
| SEC-09 | Rate limit login & forgot-password | Should |
| SEC-10 | Audit log cho destructive admin actions | Should |

Auth flow & trust model: xem `architecture/06-security.md`.
