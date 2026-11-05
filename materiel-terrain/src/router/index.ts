/**
 * Création du router et installation de la garde d'accès.
 * Les routes sont déclarées dans routes.ts.
 */
import {createRouter, createWebHistory} from 'vue-router';
import {routes} from './routes.ts';
import {installGuards} from './guards.ts';

export const router = createRouter({
    history: createWebHistory(),
    routes,
    // Chaque page s'ouvre en haut ; retour arrière : position d'avant.
    scrollBehavior: (_to, _from, saved) => saved ?? {top: 0},
});

installGuards(router);
