/**
 * Fiche d'un matériel : chargement, affectation à une salle et archivage, chacun avec son état
 * (chargement, erreur). Sorti de la vue pour qu'elle reste sous la limite de taille du cadre.
 * Utilisé par : material-detail-view.vue
 * RG : RG-07 (affectation ou remise en stock), RG-10 (archivage)
 */
import {type MaybeRefOrGetter, type Ref, ref, toValue} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import {useArchiveMaterial} from './use-archive-material.ts';

//------------------ TYPES -----------------------------------
/** Ce que les actions de la fiche lisent et modifient. */
interface DetailState {
    material: Ref<Material | null>;
    /** Identifiant du matériel affiché, lu au moment de l'action. */
    id: () => number;
}

//------------------ FUNCTIONS -------------------------------
/**
 * Charge le matériel et, en parallèle, les salles si besoin (la fiche s'affiche à la fin).
 * @param state état de la fiche
 * @param loadRooms chargement des salles, ou null si l'utilisateur ne peut pas affecter
 */
async function loadDetail(state: DetailState, loadRooms: (() => Promise<void>) | null): Promise<void> {
    state.material.value = null;
    const [loaded] = await Promise.all([materialsServices.findById(state.id()), loadRooms ? loadRooms() : Promise.resolve()]);
    state.material.value = loaded;
}

/**
 * Affecte à une salle ou remet en stock (RG-07), puis affiche le matériel à jour.
 * @param state état de la fiche
 * @param roomId salle choisie, ou null pour le stock
 */
async function assignDetail(state: DetailState, roomId: number | null): Promise<void> {
    state.material.value = await materialsServices.assignRoom(state.id(), roomId);
}

/**
 * Archive après confirmation, puis recharge la fiche (RG-10).
 * @param state état de la fiche
 * @param current matériel affiché
 * @param archiveMaterial archivage avec confirmation (use-archive-material.ts)
 */
async function archiveDetail(state: DetailState, current: Material, archiveMaterial: (material: Material) => Promise<boolean>): Promise<void> {
    if (await archiveMaterial(current)) state.material.value = await materialsServices.findById(state.id());
}

/**
 * Crée l'état et les actions de la fiche.
 * @param materialId identifiant du matériel affiché
 * @param withRooms vrai si l'utilisateur peut affecter le matériel : les salles sont alors chargées
 * @returns le matériel, les salles, les actions et leurs états
 */
export function useMaterialDetail(materialId: MaybeRefOrGetter<number>, withRooms: MaybeRefOrGetter<boolean>) {
    const {loading, errorMessage, errorStatus, run} = useAsyncState();
    const {loading: assigning, errorMessage: assignError, run: runAssign} = useAsyncState();
    const {errorMessage: archiveError, run: runArchive} = useAsyncState();
    const {rooms, loadLocations} = useLocations();
    const {archiveMaterial} = useArchiveMaterial();
    const state: DetailState = {material: ref<Material | null>(null), id: () => toValue(materialId)};
    const load = () => run(() => loadDetail(state, toValue(withRooms) ? loadLocations : null));
    const assign = (roomId: number | null) => runAssign(() => assignDetail(state, roomId));
    const archive = (current: Material) => runArchive(() => archiveDetail(state, current, archiveMaterial));
    return {material: state.material, rooms, loading, errorMessage, errorStatus, assigning, assignError, archiveError, load, assign, archive};
}
