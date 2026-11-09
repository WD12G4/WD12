/**
 * Création du router, installation de la garde d'accès et de la transition partagée
 * tuile ↔ fiche de salle (view-transition.ts).
 * Les routes sont déclarées dans routes.ts.
 */
import {createRouter, createWebHistory} from 'vue-router';
import {routes} from './routes.ts';
import {installGuards} from './guards.ts';
import {installViewTransitions} from './view-transition.ts';

export const router = createRouter({
    history: createWebHistory(),
    routes: [...routes],
});

installGuards(router);
installViewTransitions(router);
