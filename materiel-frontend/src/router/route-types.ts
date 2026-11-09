/**
 * Types des cibles de navigation, déduits du tableau `routes` de routes.ts.
 * Copié du projet du cours (vue1) : une cible invalide (nom inconnu,
 * paramètre manquant) est refusée à la compilation.
 * Seul ajout : un paramètre d'URL facultatif (query), pour ouvrir une liste déjà filtrée
 * (ex. { name: 'materials', query: { emplacement: 'stock' } }).
 * Utilisé par : navigation.ts, shared/components/Link.vue
 */
import type { routes } from './routes.ts';

/** Noms de toutes les routes déclarées. */
export type RouteNames = typeof routes[number]["name"];

type RouteDefinition = typeof routes[number];

type ExtractParams<Path extends string> =
    Path extends `${string}:${infer Param}/${infer Rest}`
        ? Param | ExtractParams<`/${Rest}`>
        : Path extends `${string}:${infer Param}`
            ? Param
            : never;

type RouteTargetMap = {
    [R in RouteDefinition as R["name"]]:
    ExtractParams<R["path"]> extends never
        ? { name: R["name"] }
        : {
            name: R["name"];
            params: Record<
                ExtractParams<R["path"]>,
                string | number
            >;
        };
};

/** Paramètres d'URL facultatifs (?cle=valeur). */
type WithQuery<Target> = Target & { query?: Record<string, string> };

/** Cible de navigation : { name } ou { name, params } selon que la route a des paramètres, plus une query facultative. */
export type RouteTarget = WithQuery<RouteTargetMap[RouteNames]>;
