import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Config do Vite. O plugin do react cuida do JSX e o do tailwind processa as
// classes utilitarias direto no build, sem precisar de postcss.config separado.
// O dev server sobe em localhost:5173, origem que o back-end libera no CORS.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
  },
})
