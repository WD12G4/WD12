<script setup lang="ts">
/**
 * Liste et recherche du matériel : l'écran le plus important pour le client
 * (besoin « scroller, chercher, vérifier », entretien 1, 0:31).
 * ?emplacement=stock|salle dans l'URL ouvre la liste déjà filtrée (liens du tableau de bord).
 * RG : RG-07, RG-10, RG-16 (création et archivage : gestionnaire et administrateur, comme le backend)
 */
import {computed, onMounted, ref} from 'vue';
import {useRoute} from 'vue-router';
import type {Material} from '../../../models/material.model.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import {filtersFromQuery, useMaterialFilters} from '../composables/use-material-filters.ts';
import {useArchiveMaterial} from '../composables/use-archive-material.ts';
import Card from '../../../shared/components/card.vue';
import Link from '../../../shared/components/Link.vue';
import LoadState from '../../../shared/components/load-state.vue';
import MaterialFilters from '../components/material-filters.vue';
import MaterialList from '../components/material-list.vue';

//------------------ DATAS -----------------------------------
const {hasRole} = useAuth();
const {loading, errorMessage, run} = useAsyncState();
const {buildings, rooms, loadLocations} = useLocations();
const {archiveMaterial} = useArchiveMaterial();
const materials = ref<Material[]>([]);
const {filters, visibleMaterials} = useMaterialFilters(materials, filtersFromQuery(useRoute().query));

//------------------ COMPUTED --------------------------------
/** RG-16 : le gestionnaire et l'administrateur créent et archivent (backend : ADMIN ou MANAGER). */
const canManage = computed(() => hasRole('MANAGER'));
const isLoaded = computed(() => !loading.value && errorMessage.value === null);

//------------------ FUNCTIONS -------------------------------
/** Charge les matériels et les emplacements (pour les filtres). */
async function load(): Promise<void> {
  await run(async () => {
    const [loaded] = await Promise.all([materialsServices.findAll(), loadLocations()]);
    materials.value = loaded;
  });
}

/** Archive après confirmation, puis recharge la liste. */
async function archive(material: Material): Promise<void> {
  await run(async () => {
    if (await archiveMaterial(material)) {
      materials.value = await materialsServices.findAll();
    }
  });
}

/** Remplace un matériel modifié depuis l'aperçu, sans recharger toute la liste. */
function replace(material: Material): void {
  materials.value = materials.value.map(item => (item.id === material.id ? material : item));
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <Card class="materials-card" title="Matériel">
    <template #header>
      <p>Retrouvez un matériel par son numéro de série, sa salle ou son bâtiment.</p>
      <Link
          v-if="canManage"
          class="button-link"
          :to="{ name: 'material-new' }"
      >
        + Nouveau matériel
      </Link>
    </template>
    <template #body>
      <MaterialFilters
          v-model:filters="filters"
          :buildings="buildings"
          :rooms="rooms"
      />
      <LoadState
          :loading="loading"
          :error-message="errorMessage"
          @retry="load"
      />
      <MaterialList
          v-if="isLoaded"
          :materials="visibleMaterials"
          :can-manage="canManage"
          @archive="archive"
          @updated="replace"
      />
    </template>
  </Card>
</template>
