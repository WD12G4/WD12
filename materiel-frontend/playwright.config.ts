/**
 * Tests navigateur (Playwright) : le front tourne avec le serveur de développement de Vite, branché sur une
 * fausse API (e2e/support/mock-api.ts) qui remplace Keycloak et le backend. Chromium seulement.
 * Lancement : npm run test:e2e (le serveur Vite est démarré s'il ne tourne pas déjà).
 */
import {defineConfig, devices} from '@playwright/test';

const PORT = 5173;
const inCi = Boolean(process.env.CI);

export default defineConfig({
    testDir: './e2e',
    fullyParallel: true,
    forbidOnly: inCi,
    retries: inCi ? 1 : 0,
    reporter: inCi ? [['list'], ['html', {open: 'never'}]] : 'list',
    use: {
        baseURL: `http://localhost:${PORT}`,
        trace: 'off',
        screenshot: 'only-on-failure',
    },
    projects: [{name: 'chromium', use: {...devices['Desktop Chrome'], viewport: {width: 1366, height: 900}}}],
    webServer: {
        command: `npx vite --host 127.0.0.1 --port ${PORT} --strictPort`,
        url: `http://localhost:${PORT}`,
        reuseExistingServer: !inCi,
        timeout: 60_000,
    },
});
