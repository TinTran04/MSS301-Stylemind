# StyleMind — Tài liệu dự án (docs-as-code)

> Fashion E-commerce Platform with AI Stylist Assistant
> Microservices (Spring Boot) + ReactJS/Vite + API Gateway + Database per service.
> **Phiên bản tài liệu: v2.0** (SePay thật, API `/api/v1`, Order State Machine, Admin self-protection, Saga, Auth↔User boundary).

Tài liệu này là bản tách nhỏ (modular) của BRD/PRD v2.0 để mỗi người mở đúng phần mình cần khi làm việc.

## Cách đọc theo vai trò
| Bạn là | Bắt đầu từ |
|---|---|
| Mới vào dự án | `overview/01-project-overview.md` → `overview/02-glossary.md` |
| PM / BA | `business/`, `product/`, `requirements/01-functional-requirements.md` |
| Backend dev | `services/<service>.md` + `architecture/` + `api/01-api-catalog.md` |
| Frontend dev | `frontend/01-frontend-requirements.md` + `api/01-api-catalog.md` |
| Kiến trúc/lead | `architecture/` (đọc hết) |
| Bắt tay migrate code | `delivery/03-migration-playbook.md` |

## Cấu trúc thư mục
```
stylemind-docs/
├── README.md
├── overview/       # tổng quan, thuật ngữ, changelog
├── business/       # BRD: mục tiêu, roles, business process
├── product/        # PRD: vision, goals, user stories
├── architecture/   # kiến trúc hệ thống, API conventions, state machine, saga, boundary, security
├── services/       # tài liệu từng microservice (mở khi code service đó)
├── api/            # API catalog (toàn bộ endpoint)
├── frontend/       # yêu cầu frontend
├── requirements/   # FR / NFR / Security requirements (traceability)
└── delivery/       # roadmap, MVP acceptance, migration playbook
```

## Quy ước
- Mọi endpoint public/admin: `/api/v1/...`; nội bộ service-to-service: `/internal/v1/...`.
- Frontend chỉ gọi API Gateway (`:3001`), không bao giờ gọi `/internal/**`.
- Thanh toán online = **SePay (VietQR / Open Banking)**, xác nhận qua **Webhook**. Không dùng "simulated online payment".
