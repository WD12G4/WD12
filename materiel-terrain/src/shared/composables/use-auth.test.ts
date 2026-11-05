import {describe, expect, it, vi} from 'vitest';

/** Réponse HTTP minimale, sans corps si body est vide. */
function response(status: number, body = '', headers: Record<string, string> = {}): Response {
    return new Response(body.length > 0 ? body : null, {status, headers});
}

/** Profil renvoyé par GET /auth/me/. */
const ME = JSON.stringify({id: 2, version: 0, username: 'manager', role: 'MANAGER', enabled: true});

describe('useAuth : rafraîchissement de session', () => {
    it("n'envoie qu'un seul refresh/ quand plusieurs appels reçoivent une 401 en même temps", async () => {
        let refreshCalls = 0;
        vi.stubGlobal('navigator', {onLine: true});
        vi.stubGlobal('fetch', vi.fn(async (url: string, init: RequestInit) => {
            if (url.endsWith('/auth/login/')) return response(200, '', {Authorization: 'Bearer expire'});
            if (url.endsWith('/auth/me/')) return response(200, ME);
            if (url.endsWith('/auth/refresh/')) {
                refreshCalls++;
                await new Promise((resolve) => setTimeout(resolve, 20));
                return response(200, '', {Authorization: 'Bearer neuf'});
            }
            // Ressources : seul le nouveau jeton est accepté (l'ancien a expiré).
            const token = new Headers(init.headers).get('Authorization');
            return token === 'Bearer neuf' ? response(200, '{"ok":true}') : response(401);
        }));

        const {useAuth} = await import('./use-auth.ts');
        const {apiRequest} = await import('../../services/http-client.ts');
        await useAuth().login('manager', 'secret');

        const results = await Promise.all([apiRequest('/materials'), apiRequest('/rooms'), apiRequest('/buildings')]);

        expect(results).toEqual([{ok: true}, {ok: true}, {ok: true}]);
        // Deux refresh/ simultanés avec le même cookie feraient révoquer la session par le backend.
        expect(refreshCalls).toBe(1);
        vi.unstubAllGlobals();
    });
});
