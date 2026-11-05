/**
 * Déclaration des routes de la PWA terrain. Chaque route déclare meta.roles (voir route-meta.d.ts).
 * Toutes les pages sont consultables par un formateur (USER) ; les actions de modification
 * sont masquées pour lui et refusées par le backend (RG-17).
 */
import type {RouteRecordRaw} from 'vue-router';
import ScanView from '../features/scan/views/scan-view.vue';
import LoginView from '../features/auth/views/login-view.vue';

export const routes: RouteRecordRaw[] = [
    {
        path: '/',
        name: 'scan',
        component: ScanView,
        meta: {roles: ['USER']},
    },
    {
        path: '/connexion',
        name: 'login',
        component: LoginView,
        meta: {roles: [], public: true},
    },
    {
        path: '/position',
        name: 'position',
        component: () => import('../features/location/views/room-picker-view.vue'),
        meta: {roles: ['USER']},
    },
    {
        path: '/synchro',
        name: 'sync',
        component: () => import('../features/inventory/views/sync-view.vue'),
        meta: {roles: ['USER']},
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: {name: 'scan'},
        meta: {roles: ['USER']},
    },
];
