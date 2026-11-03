<script setup lang="ts">
/**
 * Une ligne du tableau des matériels.
 * Composant de présentation : n'appelle aucun service, émet des événements.
 * Utilisé par : material-list.vue
 */
import {computed} from 'vue';
import {type Material, materialTypeLabels} from '../../../models/material.model.ts';
import {formatDate} from '../../../shared/composables/use-dates.ts';
import {locationLabel} from '../../../shared/composables/use-room-label.ts';
import Link from '../../../shared/components/Link.vue';
import MaterialStatusBadge from '../../../shared/components/material-status-badge.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Matériel à afficher. */
  material: Material;
  /** Vrai si l'utilisateur peut archiver (ADMIN, RG-16). */
  canArchive: boolean;
}>();

const emit = defineEmits<{
  /** Demande d'archivage du matériel affiché. */
  archive: [material: Material];
}>();

//------------------ COMPUTED --------------------------------
/** Emplacement lisible : « Bât. 12 – Salle 104 » ou « En stock » (RG-07). */
const location = computed(() => locationLabel(props.material.room));
const endDate = computed(() => formatDate(props.material.endAvailabilityDate));
/** Le bouton Archiver n'a pas de sens pour un matériel déjà archivé. */
const showArchive = computed(() => props.canArchive && !props.material.archived);
</script>

<template>
  <td>
    <Link :to="{ name: 'material-detail', params: { id: material.id } }">{{ material.serialNumber }}</Link>
  </td>
  <td>{{ materialTypeLabels[material.materialType] }}</td>
  <td>{{ location }}</td>
  <td>{{ endDate }}</td>
  <td>
    <MaterialStatusBadge :material="material" />
  </td>
  <td class="row-actions">
    <button
        v-if="showArchive"
        class="delete-button"
        type="button"
        @click="emit('archive', material)"
    >
      Archiver
    </button>
  </td>
</template>
