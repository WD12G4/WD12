<script setup lang="ts">
/**
 * Fiche d'un matériel, avec les actions autorisées par le rôle :
 * modifier et archiver (MANAGER et ADMIN, RG-16), changer d'emplacement (MANAGER et ADMIN, RG-17).
 */
import {computed, onMounted, ref, watch} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import {useArchiveMaterial} from '../composables/use-archive-material.ts';
import Card from '../../../shared/components/card.vue';
import BackLink from '../../../shared/components/back-link.vue';
import LoadState from '../../../shared/components/load-state.vue';
import MaterialDetails from '../components/material-details.vue';
import MaterialActions from '../components/material-actions.vue';
import MaterialAssign from '../components/material-assign.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Identifiant du matériel, reçu de l'URL (chaîne). */
  id: string;
}>();

//------------------ DATAS -----------------------------------
const {hasRole} = useAuth();
const {loading, errorMessage, errorStatus, run} = useAsyncState();
const {loading: assigning, errorMessage: assignError, run: runAssign} = useAsyncState();
const {errorMessage: archiveError, run: runArchive} = useAsyncState();
const {rooms, loadLocations} = useLocations();
const {archiveMaterial} = useArchiveMaterial();
const material = ref<Material | null>(null);

//------------------ COMPUTED --------------------------------
const materialId = computed(() => Number(props.id));
const canEdit = computed(() => hasRole('MANAGER'));
const canAssign = computed(() => hasRole('MANAGER'));
const isNotFound = computed(() => errorStatus.value === 404);

//------------------ FUNCTIONS -------------------------------
/** Charge le matériel et, en parallèle, les salles si l'utilisateur peut l'affecter (fiche affichée à la fin). */
async function load(): Promise<void> {
  material.value = null;
  await run(async () => {
    const [loaded] = await Promise.all([
      materialsServices.findById(materialId.value),
      canAssign.value ? loadLocations() : Promise.resolve(),
    ]);
    material.value = loaded;
  });
}

/** Affecte à une salle ou remet en stock (RG-07), puis affiche le matériel à jour. */
async function assign(roomId: number | null): Promise<void> {
  await runAssign(async () => {
    material.value = await materialsServices.assignRoom(materialId.value, roomId);
  });
}

/** Archive après confirmation, puis recharge la fiche (RG-10). */
async function archive(current: Material): Promise<void> {
  await runArchive(async () => {
    if (await archiveMaterial(current)) {
      material.value = await materialsServices.findById(materialId.value);
    }
  });
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
watch(() => props.id, load);
</script>

<template>
  <!-- Retour à la page d'où l'on vient (liste du matériel, fiche d'une salle…), sinon au matériel -->
  <BackLink :fallback="{ name: 'materials' }" />

  <!-- 404 : carte « introuvable » -->
  <Card v-if="isNotFound" title="Matériel introuvable">
    <template #body>
      <p>Ce matériel n'existe pas ou plus.</p>
    </template>
  </Card>

  <!-- Chargement ou erreur -->
  <LoadState
      v-else-if="!material"
      :loading="loading"
      :error-message="errorMessage"
      @retry="load"
  />

  <!-- Données -->
  <template v-else>
    <Card :title="`Matériel ${material.serialNumber}`">
      <template #body>
        <MaterialDetails :material="material" />
        <MaterialActions
            :material="material"
            :can-edit="canEdit"
            :error-message="archiveError"
            @archive="archive"
        />
      </template>
    </Card>
    <Card v-if="canAssign" title="Emplacement">
      <template #body>
        <MaterialAssign
            :material="material"
            :rooms="rooms"
            :submitting="assigning"
            :error-message="assignError"
            @assign="assign"
        />
      </template>
    </Card>
  </template>
</template>
