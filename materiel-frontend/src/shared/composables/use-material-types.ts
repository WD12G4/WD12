/**
 * Types de matériel (code et libellé) lus depuis l'API, c'est-à-dire l'enum EMaterialType du backend.
 * La liste est chargée une seule fois puis partagée par tous les écrans : elle ne change qu'avec le backend.
 * Utilisé par : formulaire, filtres, lignes et fiche du matériel, accueil, remontées.
 * RG : RG-03 (type de matériel)
 */
import {computed, ref} from 'vue';
import type {MaterialType, MaterialTypeOption} from '../../models/material.model.ts';
import {materialsServices} from '../../services/materials.services.ts';

//------------------ DATAS (partagées) -----------------------
const types = ref<MaterialTypeOption[]>([]);
/** Chargement en cours ou réussi ; remis à null après un échec, pour pouvoir réessayer. */
let pending: Promise<void> | null = null;
const labels = computed(() => new Map(types.value.map(type => [type.code, type.label])));

//------------------ FUNCTIONS -------------------------------
/**
 * Charge les types s'ils ne le sont pas déjà.
 * @throws ApiError si l'API refuse
 */
function loadMaterialTypes(): Promise<void> {
    pending ??= materialsServices.findTypes()
        .then(loaded => {
            types.value = loaded;
        })
        .catch((error: unknown) => {
            pending = null;
            throw error;
        });
    return pending;
}

/**
 * Libellé affiché d'un type.
 * @param code code du type (ex. 'UC')
 * @returns le libellé de l'API, ou le code tant que la liste n'est pas chargée
 */
function typeLabel(code: MaterialType): string {
    return labels.value.get(code) ?? code;
}

/**
 * Accès aux types de matériel. Le premier appel lance le chargement ; s'il échoue, les codes restent
 * affichés. Un écran de saisie attend loadMaterialTypes() pour afficher l'erreur éventuelle.
 * @returns materialTypes (dans l'ordre de l'enum), typeLabel() et loadMaterialTypes()
 */
export function useMaterialTypes() {
    void loadMaterialTypes().catch(() => undefined);
    return {materialTypes: computed(() => types.value), typeLabel, loadMaterialTypes};
}
