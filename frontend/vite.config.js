import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

// Config do Vite. O plugin do react cuida do JSX e o do tailwind processa as
// classes utilitarias direto no build, sem precisar de postcss.config separado.
// O dev server sobe em localhost:5173, origem que o back-end libera no CORS.
export default defineConfig({
  plugins: [react(), tailwindcss()],

  resolve: {
    alias: {
      // "@" aponta pra src/. Um import passa a ser '@/ui/spinner.jsx' em vez de
      // '../../ui/spinner.jsx'.
      //
      // O ganho nao e so estetico: caminho relativo depende de ONDE o arquivo que
      // importa esta, entao mover um componente de pasta quebrava todos os imports
      // dele. Com o alias, o caminho e sempre o mesmo, venha de onde vier.
      //
      // O Vitest le este mesmo arquivo, entao o alias tambem vale nos testes - nao
      // existe uma segunda configuracao pra manter em sincronia.
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  server: {
    port: 5173,
  },
})
