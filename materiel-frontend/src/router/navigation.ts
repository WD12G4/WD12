/**
 * Navigation par programme, typée. Copié du projet du cours (vue1).
 * Toujours passer par navigateTo({ name, params }) : jamais router.push('/chemin').
 */
import { router } from './index.ts';
import type { RouteTarget } from './route-types.ts';

/**
 * Navigue vers une route nommée.
 * @param to cible typée (nom de route et paramètres)
 */
export function navigateTo(to: RouteTarget) {
    return router.push(to);
}
