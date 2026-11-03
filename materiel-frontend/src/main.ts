/**
 * Point d'entrée : crée l'application, branche le router et les styles globaux. Rien d'autre.
 */
import {createApp} from 'vue';
import './style.css';
import App from './App.vue';
import {router} from './router/index.ts';

const app = createApp(App);
app.use(router);
app.mount('#app');
