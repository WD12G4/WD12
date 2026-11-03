/**
 * Création du router et installation de la garde d'accès.
 * Les routes sont déclarées dans routes.ts.
 */
import {createRouter, createWebHistory} from 'vue-router';
import {routes} from './routes.ts';
import {installGuards} from './guards.ts';

export const router = createRouter({
    history: createWebHistory(),
    routes: [...routes],
});

installGuards(router);
