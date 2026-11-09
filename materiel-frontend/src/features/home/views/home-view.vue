<script setup lang="ts">
/**
 * Tableau de bord : compteurs, indicateur groupé des fins de disponibilité et vue d'ensemble des salles.
 * Accessible à tous les rôles.
 * Besoin : vision globale du parc, chiffres plutôt que graphiques (entretien 1, 12:49 et 13:37) ;
 * vue des salles simple et lisible (entretien 4, 19:36). Plus d'arbre à déplier : toutes les salles
 * sont visibles d'un coup et un seul clic ouvre la fiche d'une salle.
 * RG : RG-07, RG-10, RG-24
 */
import {computed, onMounted, ref} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import {EXPIRY_WARNING_DAYS} from '../../../shared/composables/use-dates.ts';
import {useDashboardStats} from '../composables/use-dashboard-stats.ts';
import {useRoomOverview} from '../composables/use-room-overview.ts';
import Card from '../../../shared/components/card.vue';
import LoadState from '../../../shared/components/load-state.vue';
import StatsPanel from '../components/stats-panel.vue';
import ExpiryIndicator from '../components/expiry-indicator.vue';
import RoomOverview from '../components/room-overview.vue';

//------------------ DATAS -----------------------------------
const {loading, errorMessage, run} = useAsyncState();
const {rooms, loadLocations} = useLocations();
const materials = ref<Material[]>([]);
/** Horizon des fins de disponibilité (30, 60 ou 90 jours), réglé dans le bandeau : compteurs et tuiles le suivent. */
const horizon = ref(EXPIRY_WARNING_DAYS);
const {stats} = useDashboardStats(materials, horizon);
const {roomSummaries, stockSummary} = useRoomOverview(rooms, materials, horizon);

//------------------ COMPUTED --------------------------------
const isLoaded = computed(() => !loading.value && errorMessage.value === null);

//------------------ FUNCTIONS -------------------------------
/** Charge le matériel et les emplacements en parallèle. */
async function load(): Promise<void> {
  await run(async () => {
    const [loaded] = await Promise.all([materialsServices.findAll(), loadLocations()]);
    materials.value = loaded;
  });
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <LoadState
      :loading="loading"
      :error-message="errorMessage"
      @retry="load"
  />
  <template v-if="isLoaded">
    <StatsPanel :stats="stats" :horizon="horizon" />
    <ExpiryIndicator v-model:horizon="horizon" :count="stats.expiring" />
    <Card title="Le parc, salle par salle">
      <template #header>
        <p>Toutes les salles d'un coup d'œil. Cliquez sur une salle pour voir son matériel.</p>
      </template>
      <template #body>
        <RoomOverview :rooms="roomSummaries" :stock="stockSummary" />
      </template>
    </Card>
  </template>
</template>
