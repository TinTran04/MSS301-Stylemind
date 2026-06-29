# API Client — StyleMind Frontend

## 1. Axios Client

```ts
// src/services/apiClient.ts

import axios from "axios";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("access_token");

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    return Promise.reject(error);
  }
);
```

## 2. Environment

```env
VITE_API_BASE_URL=http://localhost:3001
```

## 3. API Integration Rules

- Không hard-code service port ở frontend.
- Tất cả request đi qua API Gateway.
- Normalize error response trước khi hiển thị.
- Không log token ra console.
