<script setup lang="ts">
/**
 * Fiche d'un matériel, avec les actions autorisées par le rôle :
 * modifier et archiver (MANAGER et ADMIN, RG-16), changer d'emplacement (MANAGER et ADMIN, RG-16).
 * Le formateur n'ouvre que la fiche d'un matériel de sa salle de cours : une autre lui est refusée
 * (filtre du front seulement, l'API ne limite pas encore le formateur à une salle).
 * RG : RG-16, RG-34 (formateur : sa salle seulement)
 */
import {computed, onMounted, watch} from 'vue';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useTeachingRoom} from '../../../shared/composables/use-teaching-room.ts';
import {useMaterialDetail} from '../composables/use-material-detail.ts';
import Card from '../../../shared/components/card.vue';
import BackLink from '../../../shared/components/back-link.vue';
import LoadState from '../../../shared/components/load-state.vue';
import OutOfRoomCard from '../../../shared/components/out-of-room-card.vue';
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
const {trainerRoomId} = useTeachingRoom();
const {
  material, rooms, loading, errorMessage, errorStatus, assigning, assignError, archiveError, load, assign, archive,
} = useMaterialDetail(() => Number(props.id), () => hasRole('MANAGER'));

//------------------ COMPUTED --------------------------------
/** RG-16 : le gestionnaire et l'administrateur modifient, archivent et affectent le matériel. */
const canManage = computed(() => hasRole('MANAGER'));
const isNotFound = computed(() => errorStatus.value === 404);
/** RG-34 : vrai si un formateur ouvre un matériel qui n'est pas dans sa salle de cours (ou en stock). */
const isOtherRoom = computed(() => trainerRoomId.value !== null && material.value !== null
  && material.value.room?.id !== trainerRoomId.value);

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

  <!-- RG-34 : matériel hors de la salle du formateur -->
  <OutOfRoomCard
      v-else-if="isOtherRoom"
      title="Matériel non accessible"
      label="Le matériel de ma salle"
      :to="{ name: 'materials' }"
  />

  <!-- Données -->
  <template v-else>
    <Card :title="`Matériel ${material.serialNumber}`">
      <template #body>
        <MaterialDetails :material="material" />
        <MaterialActions
            :material="material"
            :can-edit="canManage"
            :error-message="archiveError"
            @archive="archive"
        />
      </template>
    </Card>
    <Card v-if="canManage" title="Emplacement">
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
