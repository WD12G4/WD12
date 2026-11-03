/**
 * Filtres de la liste des matériels (recherche, type, bâtiment, salle, emplacement, archivés).
 * Même structure que use-todo-filters.ts du cours : une fonction pure par critère.
 * Utilisé par : features/materials/views/material-view.vue
 * Besoin : recherche rapide (entretien 1, 0:31). RG : RG-07 (filtre « en stock »), RG-10 (archivés)
 */
import {computed, ref, type Ref, watch} from 'vue';
import type {
    Material, MaterialFilters, MaterialLocationFilter, MaterialTypeFilter,
} from '../../../models/material.model.ts';

/** Valeurs des filtres à l'ouverture de la page : tout, sauf les archivés. */
const defaultFilters: Readonly<MaterialFilters> = {
    search: '',
    type: 'all',
    buildingId: 'all',
    roomId: 'all',
    location: 'all',
    showArchived: false,
};

//------------------ CRITÈRES --------------------------------
/** Le texte est cherché dans le numéro de série, le numéro de salle et le numéro de bâtiment. */
function matchesSearch(material: Material, normalizedSearch: string): boolean {
    if (normalizedSearch === '') return true;
    const room = material.room;
    const haystack = [
        material.serialNumber,
        room ? `salle ${room.number}` : 'stock',
        room ? `bât ${room.building.number}` : '',
    ].join(' ').toLowerCase();
    return haystack.includes(normalizedSearch);
}

function matchesType(material: Material, type: MaterialTypeFilter): boolean {
    return type === 'all' || material.materialType === type;
}

function matchesBuilding(material: Material, buildingId: number | 'all'): boolean {
    return buildingId === 'all' || material.room?.building.id === buildingId;
}

function matchesRoom(material: Material, roomId: number | 'all'): boolean {
    return roomId === 'all' || material.room?.id === roomId;
}

/** RG-07 : un matériel sans salle est en stock. */
function matchesLocation(material: Material, location: MaterialLocationFilter): boolean {
    if (location === 'assigned') return material.room !== null;
    if (location === 'stock') return material.room === null;
    return true;
}

/** RG-10 : les archivés sont masqués par défaut. */
function matchesArchived(material: Material, showArchived: boolean): boolean {
    return showArchived || !material.archived;
}

/**
 * Crée les filtres et la liste filtrée.
 * @param materials liste complète, chargée par la vue
 * @returns filters (lié au composant de filtres) et visibleMaterials (liste filtrée)
 */
export function useMaterialFilters(materials: Ref<Material[]>) {
    //------------------ REACTIVE --------------------------------
    const filters = ref<MaterialFilters>({...defaultFilters});

    // Changer de bâtiment rend le choix de salle incohérent : on le remet à « toutes ».
    watch(() => filters.value.buildingId, () => {
        filters.value.roomId = 'all';
    });

    //------------------ COMPUTED --------------------------------
    const visibleMaterials = computed(() => {
        const current = filters.value;
        const normalizedSearch = current.search.trim().toLowerCase();
        return materials.value.filter(material =>
            matchesSearch(material, normalizedSearch)
            && matchesType(material, current.type)
            && matchesBuilding(material, current.buildingId)
            && matchesRoom(material, current.roomId)
            && matchesLocation(material, current.location)
            && matchesArchived(material, current.showArchived),
        );
    });

    return {filters, visibleMaterials};
}
