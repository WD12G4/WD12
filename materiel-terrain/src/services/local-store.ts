/**
 * Stockage local du téléphone (localStorage), en JSON, pour travailler sans réseau.
 * Contenu : parc en cache, actions en attente, salle courante, matériels vus, dernier profil connu.
 * Jamais de jeton ici : le jeton d'accès reste en mémoire, comme dans materiel-frontend.
 * Utilisé par : use-auth.ts, features/inventory, features/location, features/scan
 */

/** Préfixe des clés : évite les collisions avec d'autres applications servies sur la même origine. */
const KEY_PREFIX = 'recon.';

/**
 * Lit une valeur enregistrée.
 * @param key clé sans préfixe
 * @param fallback valeur renvoyée si rien n'est enregistré ou si le stockage est illisible
 */
export function readStored<T>(key: string, fallback: T): T {
    try {
        const text = localStorage.getItem(KEY_PREFIX + key);
        return text === null ? fallback : (JSON.parse(text) as T);
    } catch {
        return fallback;
    }
}

/**
 * Enregistre une valeur. Un stockage plein ou interdit (navigation privée) n'empêche pas de travailler.
 * @param key clé sans préfixe
 * @param value valeur sérialisable en JSON
 */
export function writeStored(key: string, value: unknown): void {
    try {
        localStorage.setItem(KEY_PREFIX + key, JSON.stringify(value));
    } catch {
        // L'application continue sans cache.
    }
}

/**
 * Supprime une valeur enregistrée.
 * @param key clé sans préfixe
 */
export function removeStored(key: string): void {
    try {
        localStorage.removeItem(KEY_PREFIX + key);
    } catch {
        // Rien à faire.
    }
}
