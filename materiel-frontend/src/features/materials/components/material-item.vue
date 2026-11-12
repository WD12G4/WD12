<script setup lang="ts">
/**
 * Une ligne du tableau des matériels.
 * Composant de présentation : n'appelle aucun service, émet des événements.
 * Un clic sur le numéro de série ouvre l'aperçu latéral (material-preview.vue) ; Cmd ou Ctrl + clic
 * ouvre la fiche complète dans un nouvel onglet.
 * Utilisé par : material-list.vue
 */
import {computed} from 'vue';
import {useRouter} from 'vue-router';
import {type Material, materialTypeLabels} from '../../../models/material.model.ts';
import {formatDate} from '../../../shared/composables/use-dates.ts';
import {locationLabel} from '../../../shared/composables/use-room-label.ts';
import MaterialStatusBadge from '../../../shared/components/material-status-badge.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Matériel à afficher. */
  material: Material;
  /** Vrai si l'utilisateur peut archiver (gestionnaire et administrateur, RG-16). */
  canArchive: boolean;
}>();

const emit = defineEmits<{
  /** Demande d'archivage du matériel affiché. */
  archive: [material: Material];
  /** Clic simple sur le numéro de série : aperçu latéral. */
  preview: [material: Material];
}>();

//------------------ DATAS -----------------------------------
const router = useRouter();

//------------------ COMPUTED --------------------------------
/** Emplacement lisible : « Bât. 12 – Salle 104 » ou « En stock » (RG-07). */
const location = computed(() => locationLabel(props.material.room));
const endDate = computed(() => formatDate(props.material.endAvailabilityDate));
/** Le bouton Archiver n'a pas de sens pour un matériel déjà archivé. */
const showArchive = computed(() => props.canArchive && !props.material.archived);
/** Adresse de la fiche, pour Cmd ou Ctrl + clic (nouvel onglet). */
const detailHref = computed(() => router.resolve({name: 'material-detail', params: {id: props.material.id}}).href);

//------------------ FUNCTIONS -------------------------------
/**
 * Clic simple : aperçu latéral, sans quitter la liste. Clic avec modificateur : laissé au navigateur.
 * Un <a> ordinaire et non <Link> : RouterLink naviguerait avant que ce gestionnaire puisse l'en empêcher.
 */
function onSerialClick(event: MouseEvent): void {
  if (event.metaKey || event.ctrlKey || event.shiftKey || event.button !== 0) return;
  event.preventDefault();
  emit('preview', props.material);
}
</script>

<template>
  <td>
    <a
        class="code"
        :href="detailHref"
        @click="onSerialClick"
    >
      {{ material.serialNumber }}
    </a>
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
