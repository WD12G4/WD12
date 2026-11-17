<script setup lang="ts">
/**
 * Liste et recherche du matériel : l'écran le plus important pour le client
 * (besoin « scroller, chercher, vérifier », entretien 1, 0:31).
 * ?emplacement=stock|salle dans l'URL ouvre la liste déjà filtrée (liens du tableau de bord).
 * RG : RG-07, RG-10, RG-16 (création et archivage : gestionnaire et administrateur, comme le backend),
 * RG-34 (formateur : matériel de sa salle seulement)
 */
import {computed, onMounted, ref} from 'vue';
import {useRoute} from 'vue-router';
import type {Material} from '../../../models/material.model.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useTeachingRoom} from '../../../shared/composables/use-teaching-room.ts';
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
const {trainerRoomId} = useTeachingRoom();
const materials = ref<Material[]>([]);

//------------------ COMPUTED --------------------------------
/** RG-34 : le formateur ne voit que le matériel et la salle de sa salle de cours (choisie à la connexion). */
const scopedMaterials = computed(() => (trainerRoomId.value === null
  ? materials.value : materials.value.filter(material => material.room?.id === trainerRoomId.value)));
const scopedRooms = computed(() => (trainerRoomId.value === null ? rooms.value : rooms.value.filter(room => room.id === trainerRoomId.value)));
const scopedBuildings = computed(() => (trainerRoomId.value === null
  ? buildings.value : buildings.value.filter(building => scopedRooms.value.some(room => room.building.id === building.id))));
const intro = computed(() => (trainerRoomId.value === null
  ? 'Retrouvez un matériel par son numéro de série, sa salle ou son bâtiment.' : 'Le matériel de votre salle de cours.'));
const {filters, visibleMaterials} = useMaterialFilters(scopedMaterials, filtersFromQuery(useRoute().query));
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
      <p>{{ intro }}</p>
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
          :buildings="scopedBuildings"
          :rooms="scopedRooms"
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
