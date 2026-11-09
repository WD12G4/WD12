<script setup lang="ts">
/**
 * Vue d'ensemble du parc : une seule grille de tuiles, une par salle (triées par bâtiment puis
 * par salle), puis le matériel en stock. Rien à déplier : un seul clic mène à la fiche d'une salle.
 * Composant de présentation.
 * Utilisé par : home-view.vue
 * RG : RG-07 (le matériel sans salle est en stock)
 */
import {computed} from 'vue';
import type {MaterialSummary, RoomSummary} from '../composables/use-room-overview.ts';
import {sharedRoomId} from '../../../router/view-transition.ts';
import OverviewTile from './overview-tile.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Une entrée par salle, déjà triée. */
  rooms: RoomSummary[];
  /** Résumé du matériel sans salle. */
  stock: MaterialSummary;
}>();

/** Les tuiles apparaissent en cascade après les 4 compteurs du tableau de bord. */
const STATS_COUNT = 4;

//------------------ COMPUTED --------------------------------
/** Vrai si aucune salle n'est enregistrée. */
const isEmpty = computed(() => props.rooms.length === 0);

//------------------ FUNCTIONS -------------------------------
/** Rang d'apparition d'une tuile (variable CSS --i, lue par l'animation d'entrée). */
function tileStyle(index: number): Record<string, number> {
  return {'--i': index + STATS_COUNT};
}

/** La tuile de la salle ouverte (ou quittée) porte le nom de la transition partagée. */
function tileClass(roomId: number): Record<string, boolean> {
  return {'vt-target': roomId === sharedRoomId.value};
}
</script>

<template>
  <p v-if="isEmpty" class="empty-state">Aucune salle enregistrée.</p>
  <div class="overview-grid">
    <OverviewTile
        v-for="(roomSummary, index) in rooms"
        :key="roomSummary.room.id"
        :style="tileStyle(index)"
        :class="tileClass(roomSummary.room.id)"
        :title="roomSummary.title"
        :subtitle="roomSummary.subtitle"
        :summary="roomSummary"
        :to="{ name: 'room-detail', params: { id: roomSummary.room.id } }"
    />
    <!-- RG-07 : le matériel sans salle est en stock ; la tuile ouvre la liste déjà filtrée. -->
    <OverviewTile
        title="En stock"
        subtitle="Hors salle"
        :style="tileStyle(rooms.length)"
        :summary="stock"
        :to="{ name: 'materials', query: { emplacement: 'stock' } }"
        dashed
    />
  </div>
</template>
