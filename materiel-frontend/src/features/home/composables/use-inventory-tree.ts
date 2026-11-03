/**
 * Construit l'arbre du tableau de bord : bâtiment → salle → matériel, plus le matériel en stock.
 * Les matériels archivés ne sont pas affichés (RG-10).
 * Utilisé par : home-view.vue
 * Besoin : tableau de bord en arbre dépliable, pas de carte 2D (entretien 1, 12:49). RG : RG-07
 */
import {computed, type Ref} from 'vue';
import type {Building} from '../../../models/building.model.ts';
import type {Material} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';

/** Nœud « salle » : la salle et son matériel. */
interface RoomNode {
    room: Room;
    materials: Material[];
}

/** Nœud « bâtiment » : ses salles et le nombre total de matériels. */
interface BuildingNode {
    building: Building;
    rooms: RoomNode[];
    materialCount: number;
}

/** Regroupe le matériel actif par identifiant de salle. */
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

/** Construit le nœud d'un bâtiment à partir de ses salles. */
function toBuildingNode(building: Building, rooms: Room[], byRoom: Map<number, Material[]>): BuildingNode {
    const roomNodes = rooms
        .filter(room => room.building.id === building.id)
        .map(room => ({room, materials: byRoom.get(room.id) ?? []}));
    const materialCount = roomNodes.reduce((total, node) => total + node.materials.length, 0);
    return {building, rooms: roomNodes, materialCount};
}

/**
 * Crée l'arbre à partir des listes chargées par la vue.
 * @returns buildingNodes (bâtiments avec leurs salles) et stockMaterials (matériel sans salle, RG-07)
 */
export function useInventoryTree(buildings: Ref<Building[]>, rooms: Ref<Room[]>, materials: Ref<Material[]>) {
    const activeMaterials = computed(() => materials.value.filter(material => !material.archived));

    const buildingNodes = computed(() => {
        const byRoom = groupByRoom(activeMaterials.value);
        return buildings.value.map(building => toBuildingNode(building, rooms.value, byRoom));
    });

    const stockMaterials = computed(() => activeMaterials.value.filter(material => material.room === null));

    return {buildingNodes, stockMaterials};
}

export type {BuildingNode, RoomNode};
