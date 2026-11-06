/**
 * Page de résultats renvoyée par les listes du backend (Spring Data Page).
 * Utilisé par : tous les services qui appellent une route de liste.
 *
 * Forme vérifiée le 03/11/2026 contre l'API réelle (branche proto-backend-frontend) :
 * sérialisation directe de PageImpl, métadonnées à la racine
 * ({ content, number, size, totalElements, totalPages, first, last, … }).
 */

/** Page renvoyée par l'API. Seul `content` est utilisé par le front (les listes sont chargées en une page). */
export interface PageDto<T> {
    content: T[];
    /** Numéro de la page, à partir de 0. */
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
}

/**
 * Extrait les éléments d'une page.
 * @param page page reçue de l'API
 * @returns les éléments de la page
 */
export function pageContent<T>(page: PageDto<T>): T[] {
    return page.content;
}
