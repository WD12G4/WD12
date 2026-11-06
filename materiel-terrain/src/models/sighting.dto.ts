/**
 * Constats de terrain tels qu'échangés avec l'API (/api/v1/sightings) : numéro scanné dans une salle.
 * Utilisé par : services/sightings.services.ts, features/inventory/composables/use-sightings.ts
 */

/** Constat renvoyé par l'API. */
export interface SightingDto {
    id: number;
    /** Numéro normalisé par le serveur (majuscules). */
    serialNumber: string;
    roomId: number;
    /** Date du dernier scan (ISO). */
    seenAt: string;
    /** Agent du dernier scan. */
    seenBy: string;
}

/** Corps de POST /api/v1/sightings. */
export interface SightingRequestDto {
    serialNumber: string;
    roomId: number;
    /** Date du scan (scan hors ligne envoyé plus tard). */
    seenAt: string | null;
    /** Numéro lu puis corrigé par l'agent : son constat est retiré. */
    replaces: string | null;
}
