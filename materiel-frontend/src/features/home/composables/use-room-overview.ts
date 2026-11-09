/**
 * Vue d'ensemble du parc pour le tableau de bord : toutes les salles visibles d'un coup, en une seule
 * grille triée par bâtiment puis par salle, sans rien à déplier. Un clic sur une salle mène à sa fiche.
 * Les matériels archivés ne sont pas comptés (RG-10).
 * Utilisé par : home-view.vue
 * Besoin : vue des salles simple et lisible (entretien 4, 19:36). RG : RG-07, RG-24
 */
import {computed, type Ref} from 'vue';
import {type Material, type MaterialType, materialTypes} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {isMaterialExpiring} from '../../../shared/composables/use-material-status.ts';

/** Libellés courts des pastilles, pour qu'elles tiennent à plusieurs sur une ligne de tuile. */
const shortTypeLabels: Readonly<Record<MaterialType, string>> = {
    UC: 'UC',
    SCREEN: 'Écran',
    PROJECTOR: 'Projecteur',
    PERIPHERAL: 'Périph.',
};

/** Nombre de matériels d'un type, pour les pastilles d'une tuile. */
interface TypeCount {
    type: MaterialType;
    label: string;
    count: number;
}

/** Résumé d'un lot de matériels (une salle ou le stock). Les booléens évitent la logique dans les templates. */
interface MaterialSummary {
    total: number;
    /** Types présents seulement, dans l'ordre de materialTypes. */
    byType: TypeCount[];
    /** Matériels dont la fin de disponibilité approche ou est passée (RG-24). */
    expiring: number;
    hasMaterials: boolean;
    hasExpiring: boolean;
}

/** Tuile d'une salle. */
interface RoomSummary extends MaterialSummary {
    room: Room;
    /** Libellé principal, ex. « Salle 10 ». */
    title: string;
    /** Libellé secondaire, ex. « Bâtiment 101 ». */
    subtitle: string;
}

/**
 * Résume un lot de matériels : total, répartition par type, alertes de fin de disponibilité.
 * @param materials matériels non archivés
 */
function summarize(materials: Material[]): MaterialSummary {
    const byType = materialTypes
        .map(type => ({type, label: shortTypeLabels[type], count: materials.filter(m => m.materialType === type).length}))
        .filter(typeCount => typeCount.count > 0);
    const expiring = materials.filter(isMaterialExpiring).length;
    return {total: materials.length, byType, expiring, hasMaterials: materials.length > 0, hasExpiring: expiring > 0};
}

/** Regroupe le matériel affecté par identifiant de salle. */
function groupByRoom(materials: Material[]): Map<number, Material[]> {
    const byRoom = new Map<number, Material[]>();
    for (const material of materials) {
        if (!material.room) continue;
        const list = byRoom.get(material.room.id) ?? [];
        list.push(material);
        byRoom.set(material.room.id, list);
    }
    return byRoom;
}

/** Ordre d'affichage : bâtiment, puis salle. */
function compareRooms(a: Room, b: Room): number {
    return a.building.number - b.building.number || a.number - b.number;
}

/**
 * Crée la vue d'ensemble à partir des listes chargées par la vue.
 * @returns roomSummaries (une tuile par salle, triées) et stockSummary (matériel sans salle, RG-07)
 */
export function useRoomOverview(rooms: Ref<Room[]>, materials: Ref<Material[]>) {
    const activeMaterials = computed(() => materials.value.filter(material => !material.archived));

    const roomSummaries = computed<RoomSummary[]>(() => {
        const byRoom = groupByRoom(activeMaterials.value);
        return [...rooms.value].sort(compareRooms).map(room => ({
            room,
            title: `Salle ${room.number}`,
            subtitle: `Bâtiment ${room.building.number}`,
            ...summarize(byRoom.get(room.id) ?? []),
        }));
    });

    const stockSummary = computed(() => summarize(activeMaterials.value.filter(material => material.room === null)));

    return {roomSummaries, stockSummary};
}

export type {MaterialSummary, RoomSummary};
