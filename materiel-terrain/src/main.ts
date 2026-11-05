/**
 * Point d'entrée : crée l'application, branche le router et les styles globaux.
 * Enregistrement du service worker pour l'installation PWA et le mode hors-ligne.
 */
import {createApp} from 'vue';
import {registerSW} from 'virtual:pwa-register';
import './style.css';
import App from './App.vue';
import {router} from './router/index.ts';

// Enregistrement immédiat du Service Worker (requis pour l'installation PWA sur Android & iOS)
registerSW({
    immediate: true,
});

const app = createApp(App);
app.use(router);
app.mount('#app');
