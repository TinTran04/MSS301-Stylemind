# Migration Playbook (sync code có sẵn với docs v2.0)

Playbook đầy đủ (12 prompt tiếng Anh F0→F12 để dán vào AI coding assistant) nằm ở file riêng: **`StyleMind_Migration_Playbook.md`**.

## Thứ tự thực thi (tóm tắt)
| Wave | Feature | Change |
|---|---|---|
| 0 | F0 API versioning `/api/v1` | High (rộng) |
| 1 | F1 Auth↔User boundary; F2 Auth completeness; F3 Admin self-protection | Medium |
| 2 | F4 Product + internal snapshot; F5 Cart + merge | Low |
| 3 | F6 Order State Machine; F7 Checkout saga; F8 SePay webhook | High |
| 4 | F9 Notification; F10 Admin order; F11 AI stylist | Low |
| 5 | F12 Hardening (security/observability/test) | Medium |

## Nguyên tắc
- Một feature một lần; mỗi prompt có **Step 1 — Inspect first** (báo gap trước khi sửa).
- Backend + frontend đi cùng một prompt để không lệch contract.
- Trục **F6 → F7 → F8** là phần rủi ro nhất (state machine → checkout → tiền thật qua webhook).
- Sau mỗi feature: test + commit rồi mới sang feature kế.
