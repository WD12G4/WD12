/**
 * Archivage d'un matériel avec confirmation (gestionnaire et administrateur).
 * Partagé par la liste et la fiche pour ne pas dupliquer la confirmation.
 * Utilisé par : material-view.vue, material-detail-view.vue
 * RG : RG-10 (suppression = archivage), RG-16 (gestionnaire et administrateur)
 */
import type {Material} from '../../../models/material.model.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {useFlashMessage} from '../../../shared/composables/use-flash-message.ts';

/**
 * Crée l'action d'archivage.
 * @returns archiveMaterial(), qui renvoie vrai si le matériel a été archivé
 */
export function useArchiveMaterial() {
    const {show} = useFlashMessage();

    /**
     * Demande confirmation puis archive le matériel.
     * @param material matériel à archiver
     * @returns faux si l'utilisateur a annulé
     * @throws ApiError si l'API refuse
     */
    async function archiveMaterial(material: Material): Promise<boolean> {
        const confirmed = window.confirm(
            `Archiver le matériel ${material.serialNumber} ? Il sera retiré de sa salle.`,
        );
        if (!confirmed) return false;
        await materialsServices.delete(material.id);
        show(`Le matériel ${material.serialNumber} a été archivé.`, 'success');
        return true;
    }

    return {archiveMaterial};
}
