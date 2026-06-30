# Frontend Architecture — StyleMind

## 1. Tech Stack

| Layer | Tech |
|---|---|
| Build tool | Vite |
| UI | React |
| Routing | React Router |
| Styling | Tailwind CSS |
| Client state | Zustand |
| API client | Axios |
| Server state | TanStack Query recommended |
| Form | React Hook Form |
| Validation | Zod |
| Testing | Vitest, React Testing Library, Playwright |

## 2. Folder Structure

```text
src/
├── app/
│   ├── router.tsx
│   └── providers.tsx
├── components/
│   ├── common/
│   ├── customer/
│   ├── admin/
│   └── ai/
├── features/
│   ├── auth/
│   ├── users/
│   ├── products/
│   ├── cart/
│   ├── orders/
│   ├── payments/
│   ├── notifications/
│   └── ai-stylist/
├── pages/
│   ├── customer/
│   ├── auth/
│   └── admin/
├── services/
│   ├── apiClient.ts
│   └── endpoints.ts
└── types/
```

## 3. Frontend Principles

- Feature-based organization.
- Shared API client.
- Centralized auth token handling.
- Loading/error/empty states for async UI.
- Admin route guard.
- Avoid direct service URLs.
