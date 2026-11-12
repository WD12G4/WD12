<script setup lang="ts">
/**
 * Tableau des matériels, avec le nombre de résultats et un état vide.
 * Composant de présentation : boucle sur les matériels et relaie les événements de chaque ligne.
 * Un clic sur un numéro de série ouvre l'aperçu latéral (material-preview.vue), sans quitter la liste ;
 * « Modifier » l'ouvre directement en modification. Un matériel enregistré est renvoyé au parent (updated).
 * Utilisé par : material-view.vue, features/rooms/views/room-detail-view.vue
 */
import {ref} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import MaterialItem from './material-item.vue';
import MaterialPreview from './material-preview.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Matériels à afficher (déjà filtrés). */
  materials: Material[];
  /** Vrai si l'utilisateur peut modifier et archiver (gestionnaire et administrateur, RG-16). */
  canManage: boolean;
}>();

const emit = defineEmits<{
  /** Demande d'archivage d'un matériel. */
  archive: [material: Material];
  /** Un matériel a été modifié depuis l'aperçu (version enregistrée). */
  updated: [material: Material];
}>();

//------------------ DATAS -----------------------------------
/** Matériel affiché dans l'aperçu latéral, ou null. */
const previewed = ref<Material | null>(null);
/** Vrai si l'aperçu est en modification. */
const editing = ref(false);

//------------------ FUNCTIONS -------------------------------
/** La ligne du matériel en aperçu est surlignée. */
const rowClass = (material: Material) => ({previewed: material.id === previewed.value?.id});

/**
 * Ouvre l'aperçu d'un matériel, en lecture ou directement en modification.
 * @param material matériel choisi
 * @param edit vrai pour « Modifier »
 */
function openPreview(material: Material, edit: boolean): void {
  previewed.value = material;
  editing.value = edit;
}

/** Matériel enregistré depuis l'aperçu : l'aperçu repasse en lecture, le parent met sa liste à jour. */
function onSaved(material: Material): void {
  openPreview(material, false);
  emit('updated', material);
}
</script>

<template>
  <!-- Nombre de résultats -->
  <p class="list-count">{{ materials.length }} matériel(s)</p>

  <!-- État vide -->
  <p v-if="materials.length === 0" class="empty-state">
    Aucun matériel à afficher. Modifiez la recherche ou les filtres.
  </p>

  <!-- Tableau -->
  <table v-else class="data-table">
    <thead>
      <tr>
        <th scope="col">Numéro de série</th>
        <th scope="col">Type</th>
        <th scope="col">Emplacement</th>
        <th scope="col">Fin de disponibilité</th>
        <th scope="col">Statut</th>
        <th scope="col"><span class="visually-hidden">Actions</span></th>
      </tr>
    </thead>
    <tbody>
      <tr
          v-for="material in materials"
          :key="material.id"
          :class="rowClass(material)"
      >
        <MaterialItem
            :material="material"
            :can-manage="canManage"
            @archive="emit('archive', $event)"
            @preview="openPreview($event, false)"
            @edit="openPreview($event, true)"
        />
      </tr>
    </tbody>
  </table>
  <MaterialPreview
      :material="previewed"
      :editing="editing"
      @close="previewed = null"
      @edit="editing = true"
      @cancel-edit="editing = false"
      @saved="onSaved"
  />
</template>
