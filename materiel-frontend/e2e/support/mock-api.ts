/**
 * Fausse API des tests navigateur : remplace Keycloak et le backend, sans rien changer à l'application.
 * - Keycloak : le module src/services/keycloak.ts est remplacé par un faux, où la session est déjà ouverte
 *   (la déconnexion la ferme jusqu'à la prochaine connexion).
 * - Backend : chaque appel à /api/v1 est servi depuis l'état en mémoire (mock-data.ts), avec les mêmes
 *   règles que le vrai là où les tests en dépendent (doublon refusé, liste noire, compte de démonstration…).
 * Utilisé par : fixtures.ts
 */
import type {Page, Route} from '@playwright/test';
import {createState, type MockState, type Role} from './mock-data.ts';
import {applicationRoutes, asPage, materialRoutes, reportRoutes, supervisionRoutes} from './mock-routes.ts';
import type {ApiRoute, Context} from './mock-routes.ts';

/** Faux module keycloak.ts, servi à la place du vrai. */
const FAKE_KEYCLOAK = `
const loggedOut = () => sessionStorage.getItem('e2e-logged-out') === '1';
export const keycloak = {
  token: 'jeton-de-test', tokenParsed: {preferred_username: 'admin'}, onTokenExpired: null,
  init: async () => !loggedOut(), updateToken: async () => true, clearToken() {},
  login: async () => { sessionStorage.removeItem('e2e-logged-out'); window.location.href = '/'; },
  logout: async () => { sessionStorage.setItem('e2e-logged-out', '1'); window.location.href = '/connexion'; },
};
export function appUrl(path) { return window.location.origin + path; }
`;

/** Routes communes : compte connecté, bâtiments, salles, comptes. */
const commonRoutes: ApiRoute[] = [
    ['GET', /^\/auth\/me\/$/, ({state, headers}) => {
        // Comme le backend : un administrateur peut tester avec le rôle d'un compte de démonstration.
        const demo = headers['x-demo-account'];
        const acting = demo && state.me.role === 'ADMIN' ? state.users.find(user => user.username === demo && user.enabled) : null;
        return {body: acting ?? state.me};
    }],
    ['GET', /^\/buildings$/, ({state}) => ({body: asPage(state.buildings)})],
    ['GET', /^\/rooms$/, ({state}) => ({body: asPage(state.rooms)})],
    ['GET', /^\/rooms\/(\d+)$/, ({state, params}) => {
        const room = state.rooms.find(candidate => candidate.id === Number(params[0]));
        return room ? {body: room} : {status: 404, body: {error: 'Salle introuvable'}};
    }],
    ['GET', /^\/users$/, ({state}) => ({body: asPage(state.users)})],
];

const allRoutes: ApiRoute[] = [...commonRoutes, ...materialRoutes, ...reportRoutes, ...supervisionRoutes, ...applicationRoutes];

/**
 * Répond à un appel de l'API depuis l'état en mémoire, et note la requête.
 * @param route requête interceptée par Playwright
 * @param state état de la fausse API
 */
async function handle(route: Route, state: MockState): Promise<void> {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/api/v1', '');
    const raw = request.postData();
    const body = raw ? JSON.parse(raw) as Record<string, unknown> : null;
    state.requests.push({method: request.method(), path, query: url.search, body});
    const context: Omit<Context, 'params'> = {state, query: url.searchParams, body, headers: request.headers()};
    for (const [method, pattern, handler] of allRoutes) {
        const match = method === request.method() ? pattern.exec(path) : null;
        if (!match) continue;
        const reply = handler({...context, params: match.slice(1)});
        const status = reply.status ?? 200;
        await route.fulfill({status, contentType: 'application/json', body: status === 204 ? '' : JSON.stringify(reply.body)});
        return;
    }
    await route.fulfill({status: 200, contentType: 'application/json', body: JSON.stringify(request.method() === 'GET' ? asPage([]) : {})});
}

/**
 * Installe le faux Keycloak et la fausse API sur la page, avant toute navigation.
 * @param role rôle du compte connecté
 * @returns l'état en mémoire, que le test peut compléter ou vérifier
 */
export async function installMockBackend(page: Page, role: Role): Promise<MockState> {
    const state = createState(role);
    await page.route('**/src/services/keycloak.ts*', route => route.fulfill({contentType: 'application/javascript', body: FAKE_KEYCLOAK}));
    await page.route('**/api/v1/**', route => handle(route, state));
    return state;
}
