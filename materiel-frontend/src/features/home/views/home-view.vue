<script setup lang="ts">
/**
 * Tableau de bord : compteurs, indicateur groupé des fins de disponibilité et arbre du parc.
 * Accessible à tous les rôles.
 * Besoin : vision globale en arbre, chiffres plutôt que graphiques (entretien 1, 12:49 et 13:37).
 * RG : RG-07, RG-10, RG-24
 */
import {computed, onMounted, ref} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import {useDashboardStats} from '../composables/use-dashboard-stats.ts';
import {useInventoryTree} from '../composables/use-inventory-tree.ts';
import Card from '../../../shared/components/card.vue';
import LoadState from '../../../shared/components/load-state.vue';
import StatsPanel from '../components/stats-panel.vue';
import ExpiryIndicator from '../components/expiry-indicator.vue';
import InventoryTree from '../components/inventory-tree.vue';

//------------------ DATAS -----------------------------------
const {loading, errorMessage, run} = useAsyncState();
const {buildings, rooms, loadLocations} = useLocations();
const materials = ref<Material[]>([]);
const {stats} = useDashboardStats(materials);
const {buildingNodes, stockMaterials} = useInventoryTree(buildings, rooms, materials);

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
    <StatsPanel :stats="stats" />
    <ExpiryIndicator :count="stats.expiring" />
    <Card title="Le parc, salle par salle">
      <template #header>
        <p>Dépliez un bâtiment puis une salle pour voir le matériel qui s'y trouve.</p>
      </template>
      <template #body>
        <InventoryTree :buildings="buildingNodes" :stock-materials="stockMaterials" />
      </template>
    </Card>
  </template>
</template>
