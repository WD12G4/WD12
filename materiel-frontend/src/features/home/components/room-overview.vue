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
import OverviewTile from './overview-tile.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Une entrée par salle, déjà triée. */
  rooms: RoomSummary[];
  /** Résumé du matériel sans salle. */
  stock: MaterialSummary;
}>();

//------------------ COMPUTED --------------------------------
/** Vrai si aucune salle n'est enregistrée. */
const isEmpty = computed(() => props.rooms.length === 0);
</script>

<template>
  <p v-if="isEmpty" class="empty-state">Aucune salle enregistrée.</p>
  <div class="overview-grid">
    <OverviewTile
        v-for="roomSummary in rooms"
        :key="roomSummary.room.id"
        :title="roomSummary.title"
        :subtitle="roomSummary.subtitle"
        :summary="roomSummary"
        :to="{ name: 'room-detail', params: { id: roomSummary.room.id } }"
    />
    <!-- RG-07 : le matériel sans salle est en stock ; la tuile ouvre la liste déjà filtrée. -->
    <OverviewTile
        title="En stock"
        subtitle="Hors salle"
        :summary="stock"
        :to="{ name: 'materials', query: { emplacement: 'stock' } }"
        dashed
    />
  </div>
</template>
