/**
 * Types des cibles de navigation, déduits du tableau `routes` de routes.ts.
 * Copié à l'identique du projet du cours (vue1) : une cible invalide (nom inconnu,
 * paramètre manquant) est refusée à la compilation.
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

/** Cible de navigation : { name } ou { name, params } selon que la route a des paramètres. */
export type RouteTarget = RouteTargetMap[RouteNames];
