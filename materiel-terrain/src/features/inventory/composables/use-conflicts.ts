/**
 * Arbitrage des conflits de la file : quand le serveur contredit une action faite hors ligne
 * (matériel déplacé ou déjà pris en compte par un autre agent), l'agent choisit quelle version garder.
 * Utilisé par : features/inventory/views/sync-view.vue
 */
import {materialFromDto} from '../../../models/material.dto.ts';
import {useInventory} from './use-inventory.ts';
import {useSyncQueue} from './use-sync-queue.ts';

//------------------ DATAS -----------------------------------
const {upsertMaterial, removeMaterial} = useInventory();
const {queue, updateAction, flush, discard} = useSyncQueue();

//------------------ FUNCTIONS -------------------------------
/**
 * Garde la version du serveur : l'action de l'agent est abandonnée, le cache reprend l'état du serveur.
 * @param id action en conflit
 */
function keepServer(id: string): void {
    const conflict = queue.value.find((action) => action.id === id)?.conflict;
    discard(id);
    if (conflict) upsertMaterial(materialFromDto(conflict));
}

/**
 * Garde la version de l'agent : la salle du serveur devient la nouvelle référence et l'action repart.
 * Pour une prise en compte, le matériel existe déjà : il est placé dans la salle de l'agent au lieu d'être recréé.
 * Impossible si le matériel a été archivé (le backend refuse de l'affecter, RG-06).
 * @param id action en conflit
 */
async function keepMine(id: string): Promise<void> {
    const action = queue.value.find((pending) => pending.id === id);
    const server = action?.conflict;
    if (!action || !server || server.archived) return;
    if (action.kind === 'create') removeMaterial(action.materialId);
    upsertMaterial(materialFromDto(server));
    updateAction(id, {
        kind: 'relocate', materialId: server.id, request: null,
        baseRoomId: server.room?.id ?? null, conflict: null,
    });
    await flush();
}

/**
 * Accès à l'arbitrage des conflits.
 * @returns keepMine() et keepServer()
 */
export function useConflicts() {
    return {keepMine, keepServer};
}
