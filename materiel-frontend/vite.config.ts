import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],

  server: {
    proxy: {
      // Le backend expose déjà /api/v1/... : PAS de rewrite (contrairement au projet du cours).
      // Le front, l'API et Keycloak restent sur la même origine.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Keycloak (conteneur, port 8180) sous /auth, comme derrière le Nginx du front.
      // Pour ce mode, KEYCLOAK_URL=http://localhost:5173/auth et APP_URL=http://localhost:5173 (.env).
      '/auth': {
        target: 'http://localhost:8180',
      },
    },
  },
})
