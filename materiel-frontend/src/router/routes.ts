/**
 * Déclaration de toutes les routes de l'application.
 * Même mécanisme que le projet du cours : le tableau est `as const`, ce qui permet à
 * route-types.ts de typer les noms et les paramètres de navigation.
 * Chaque route déclare meta.roles (voir route-meta.d.ts), lu par guards.ts.
 * Toutes les vues sont chargées en différé, sauf l'accueil et la connexion.
 */
import HomeView from '../features/home/views/home-view.vue';
import LoginView from '../features/auth/views/login-view.vue';

export const routes = [
    //------------------ ACCUEIL, CONNEXION (MINDEF CONNECT SIMULÉ), SALLE DU FORMATEUR (guards.ts)
    {
        path: '/',
        name: 'home',
        component: HomeView,
        meta: {roles: ['USER']},
    },
    {
        path: '/connexion',
        name: 'login',
        component: LoginView,
        meta: {roles: [], public: true},
    },
    {
        // SIMULATION : écran d'identification de MinDef Connect, public comme la connexion.
        path: '/mindef-connect',
        name: 'mindef-connect',
        component: () => import('../features/auth/views/mindef-connect-view.vue'),
        meta: {roles: [], public: true},
    },
    {
        path: '/choix-salle',
        name: 'room-choice',
        component: () => import('../features/auth/views/room-choice-view.vue'),
        meta: {roles: ['USER']},
    },
    //------------------ MATÉRIEL (lot 2) -----------------------
    {
        path: '/materiel',
        name: 'materials',
        component: () => import('../features/materials/views/material-view.vue'),
        meta: {roles: ['USER']},
    },
    {
        // Déclarée avant /materiel/:id pour que « nouveau » ne soit pas lu comme un identifiant.
        path: '/materiel/nouveau',
        name: 'material-new',
        component: () => import('../features/materials/views/material-create-view.vue'),
        meta: {roles: ['MANAGER']},
    },
    {
        path: '/materiel/:id',
        name: 'material-detail',
        component: () => import('../features/materials/views/material-detail-view.vue'),
        props: true,
        meta: {roles: ['USER']},
    },
    {
        path: '/materiel/:id/modifier',
        name: 'material-edit',
        component: () => import('../features/materials/views/material-edit-view.vue'),
        props: true,
        meta: {roles: ['MANAGER']},
    },
    //------------------ SALLES, BÂTIMENTS, COMPTES (lot 3) ------
    {
        path: '/salles',
        name: 'rooms',
        component: () => import('../features/rooms/views/room-view.vue'),
        meta: {roles: ['USER']},
    },
    {
        path: '/salles/:id',
        name: 'room-detail',
        component: () => import('../features/rooms/views/room-detail-view.vue'),
        props: true,
        meta: {roles: ['USER']},
    },
    {
        path: '/batiments',
        name: 'buildings',
        component: () => import('../features/buildings/views/building-view.vue'),
        meta: {roles: ['USER']},
    },
    {
        path: '/comptes',
        name: 'users',
        component: () => import('../features/users/views/user-view.vue'),
        meta: {roles: ['ADMIN']},
    },
    //------------------ REMONTÉES (lot 5, maquette) -------------
    {
        path: '/remontees/nouvelle',
        name: 'report-new',
        component: () => import('../features/reports/views/report-create-view.vue'),
        meta: {roles: ['USER']},
    },
    {
        path: '/remontees',
        name: 'reports',
        component: () => import('../features/reports/views/report-view.vue'),
        meta: {roles: ['USER']},
    },
    {
        path: '/remontees/:id/document',
        name: 'report-document',
        component: () => import('../features/reports/views/report-document-view.vue'),
        props: true,
        meta: {roles: ['USER']},
    },
    //------------------ SUPERVISION (lot 6, maquette) -----------
    {
        path: '/supervision',
        name: 'supervision',
        component: () => import('../features/supervision/views/supervision-view.vue'),
        meta: {roles: ['USER']},
    },
    //------------------ PAGE INCONNUE ---------------------------
    {
        path: '/:pathMatch(.*)*',
        name: 'not-found',
        component: () => import('../features/home/views/not-found-view.vue'),
        meta: {roles: ['USER']},
    },
] as const;
