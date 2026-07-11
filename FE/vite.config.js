import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  // Honor a PORT env var (used by the preview harness / auto-port) so the dev
  // server can bind an assigned port instead of always claiming 5173.
  server: {
    port: process.env.PORT ? Number(process.env.PORT) : 5173,
  },
})
