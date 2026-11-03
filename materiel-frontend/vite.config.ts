import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],

  server: {
    proxy: {
      // Le backend expose déjà /api/v1/... : PAS de rewrite (contrairement au projet du cours).
      // Le front et l'API restent sur la même origine, condition pour que le cookie
      // de rafraîchissement (SameSite=Strict, chemin /api/v1/auth) soit envoyé.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
