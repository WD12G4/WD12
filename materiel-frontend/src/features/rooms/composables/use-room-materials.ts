/**
 * Matériel présent dans une salle.
 * L'API ne filtre pas : on charge tout le matériel et on garde celui de la salle.
 * Utilisé par : room-detail-view.vue
 * RG : RG-07 (un matériel affecté a une salle)
 */
import {ref} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import {materialsServices} from '../../../services/materials.services.ts';

/**
 * Crée la liste du matériel d'une salle.
 * @returns materials et loadMaterials(roomId) (lève ApiError en cas d'échec)
 */
export function useRoomMaterials() {
    //------------------ DATAS -----------------------------------
    const materials = ref<Material[]>([]);

    //------------------ FUNCTIONS -------------------------------
    /**
     * Charge le matériel non archivé de la salle.
     * TODO(perf): à remplacer par un filtre côté backend quand il existera.
     * @param roomId identifiant de la salle
     */
    async function loadMaterials(roomId: number): Promise<void> {
        const all = await materialsServices.findAll();
        materials.value = all.filter(material => material.room?.id === roomId && !material.archived);
    }

    return {materials, loadMaterials};
}
