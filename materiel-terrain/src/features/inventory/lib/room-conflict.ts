/**
 * Détection des conflits sur l'emplacement d'un matériel, sans modifier le backend.
 * Le PATCH /materials/{id}/room n'accepte pas de version : avant d'envoyer une relocalisation,
 * on relit le matériel et on compare sa salle actuelle à celle que l'agent avait sous les yeux.
 * Seule la salle compte : une autre modification (date, type) n'empêche pas de relocaliser.
 * Fonctions pures, testées dans room-conflict.test.ts.
 */
import type {Material} from '../../../models/material.model.ts';

/** apply = envoyer ; already-done = le serveur est déjà dans l'état voulu ; conflict = l'agent doit arbitrer. */
type RoomCheck = 'apply' | 'already-done' | 'conflict';

/** Levée quand le serveur contredit une relocalisation : porte l'état actuel du matériel. */
export class MaterialConflictError extends Error {
    readonly server: Material;

    constructor(server: Material) {
        super('Le matériel a été modifié entre-temps sur le serveur.');
        this.name = 'MaterialConflictError';
        this.server = server;
    }
}

/**
 * Compare l'état du serveur à l'action de l'agent.
 * @param server matériel tel qu'il est sur le serveur maintenant
 * @param baseRoomId salle que l'agent croyait déclarée (null = en stock, undefined = inconnue)
 * @param targetRoomId salle visée par l'agent (null = en stock)
 */
export function checkRoom(server: Material, baseRoomId: number | null | undefined,
                          targetRoomId: number | null): RoomCheck {
    const serverRoomId = server.room?.id ?? null;
    // Archivé entre-temps : il ne peut plus être affecté (RG-06), l'agent doit le savoir.
    if (server.archived) return 'conflict';
    if (serverRoomId === targetRoomId) return 'already-done';
    if (baseRoomId !== undefined && serverRoomId !== baseRoomId) return 'conflict';
    return 'apply';
}

export type {RoomCheck};
