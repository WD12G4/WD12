import fs from 'node:fs';
import vue from '@vitejs/plugin-vue';
import {defineConfig} from 'vite';
import {VitePWA} from 'vite-plugin-pwa';

/**
 * Configuration de la PWA terrain.
 * - même port (5174) en dev et en preview : l'application installée sur le téléphone garde la même adresse ;
 * - HTTPS si les certificats locaux existent (npm run certs) : obligatoire pour la caméra et le service worker
 *   dès qu'on n'est plus sur localhost (téléphone → Mac) ;
 * - proxy /api sans réécriture, comme materiel-frontend : même origine, donc le cookie de
 *   rafraîchissement (SameSite=Strict, chemin /api/v1/auth) est bien envoyé.
 */

/** Adresse du backend. Surchargeable pour viser un autre conteneur : RECON_API_TARGET=http://localhost:8090 */
const apiTarget = process.env.RECON_API_TARGET ?? 'http://localhost:8080';

/** Certificat généré par scripts/generate-certs.sh, ou undefined pour rester en HTTP. */
function localCertificate() {
    const cert = new URL('./certs/server.crt', import.meta.url);
    const key = new URL('./certs/server.key', import.meta.url);
    if (process.env.RECON_HTTP === '1' || !fs.existsSync(cert) || !fs.existsSync(key)) return undefined;
    return {cert: fs.readFileSync(cert), key: fs.readFileSync(key)};
}

const https = localCertificate();
const server = {
    host: true,
    port: 5174,
    strictPort: true,
    https,
    proxy: {'/api': {target: apiTarget, changeOrigin: true}},
};

export default defineConfig({
    plugins: [
        vue(),
        VitePWA({
            registerType: 'autoUpdate',
            // Les icônes sont déjà couvertes par globPatterns : pas de doublon dans le précache.
            includeManifestIcons: false,
            manifest: {
                name: 'RECON — Reconnaissance du parc',
                short_name: 'RECON',
                description: 'Identifier, localiser et remettre à jour le matériel sur le terrain.',
                lang: 'fr',
                start_url: '/',
                scope: '/',
                display: 'standalone',
                orientation: 'portrait',
                background_color: '#060b09',
                theme_color: '#060b09',
                icons: [
                    {src: 'pwa-192x192.png', sizes: '192x192', type: 'image/png'},
                    {src: 'pwa-512x512.png', sizes: '512x512', type: 'image/png'},
                    {src: 'maskable-icon-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable'},
                ],
            },
            workbox: {
                // Le moteur de lecture (.wasm, ~1 Mo) est mis en cache : le scan fonctionne hors réseau.
                globPatterns: ['**/*.{js,css,html,svg,png,ico,wasm}'],
                maximumFileSizeToCacheInBytes: 4 * 1024 * 1024,
                navigateFallback: '/index.html',
                navigateFallbackDenylist: [/^\/api\//],
            },
        }),
    ],
    server,
    preview: server,
});
