/**
 * Point d'entrée : crée l'application, branche le router et les styles globaux. Rien d'autre.
 * Bloc-marque de l'État (thème DSFR) : composant « logo » du paquet officiel @gouvfr/dsfr, dont les
 * règles ne visent que la classe .fr-logo (sans effet sur les autres thèmes).
 */
import {createApp} from 'vue';
import '@gouvfr/dsfr/dist/component/logo/logo.min.css';
import './style.css';
import App from './App.vue';
import {router} from './router/index.ts';

const app = createApp(App);
app.use(router);
app.mount('#app');
