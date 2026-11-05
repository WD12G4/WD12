/**
 * Point d'entrée : crée l'application, branche le router et les styles globaux.
 * Le service worker (mode hors ligne, installation) est enregistré par vite-plugin-pwa, en build seulement.
 */
import {createApp} from 'vue';
import './style.css';
import App from './App.vue';
import {router} from './router/index.ts';

// En dev, un service worker laissé par un build (npm run terrain) servirait l'ancienne version : on le retire.
if (import.meta.env.DEV) {
    void navigator.serviceWorker?.getRegistrations()
        .then((registrations) => registrations.forEach((registration) => void registration.unregister()));
}

const app = createApp(App);
app.use(router);
app.mount('#app');
