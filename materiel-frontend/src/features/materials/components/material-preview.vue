<script setup lang="ts">
/**
 * Aperçu latéral d'un matériel : la fiche s'ouvre à droite de la liste, sans la quitter ni perdre ses
 * filtres (panneau partagé side-panel.vue). Un clic sur un autre matériel change l'aperçu.
 * Le gestionnaire et l'administrateur le modifient sur place : « Modifier » remplace le détail par
 * le formulaire, « Enregistrer » revient au détail mis à jour (sans changer de page).
 * Utilisé par : material-list.vue (liste du matériel, fiche d'une salle)
 * RG : RG-16 (modification : gestionnaire et administrateur)
 */
import {computed, watch} from 'vue';
import type {Material, MaterialInput} from '../../../models/material.model.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useFlashMessage} from '../../../shared/composables/use-flash-message.ts';
import {useLocations} from '../../../shared/composables/use-locations.ts';
import Link from '../../../shared/components/Link.vue';
import SidePanel from '../../../shared/components/side-panel.vue';
import MaterialDetails from './material-details.vue';
import MaterialForm from './material-form.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Matériel affiché, ou null quand l'aperçu est fermé. */
  material: Material | null;
  /** Vrai si l'aperçu montre le formulaire de modification. */
  editing: boolean;
}>();

const emit = defineEmits<{
  /** Fermeture demandée (×, Échap, voile). */
  close: [];
  /** Passage en modification (bouton « Modifier » du panneau). */
  edit: [];
  /** Abandon de la modification. */
  cancelEdit: [];
  /** Modification enregistrée : la version renvoyée par l'API. */
  saved: [material: Material];
}>();

//------------------ DATAS -----------------------------------
const {hasRole} = useAuth();
const {loading: saving, errorMessage: saveError, run: runSave} = useAsyncState();
const {rooms, loadLocations} = useLocations();
const {show} = useFlashMessage();

//------------------ COMPUTED --------------------------------
const canEdit = computed(() => hasRole('MANAGER'));
const showForm = computed(() => props.editing && canEdit.value && props.material !== null);
const materialId = computed(() => props.material?.id ?? 0);
const panelKey = computed(() => props.material?.id ?? null);
const title = computed(() => props.material?.serialNumber ?? '');
const eyebrow = computed(() => (showForm.value ? 'Modifier le matériel' : 'Aperçu du matériel'));

//------------------ FUNCTIONS -------------------------------
/**
 * Enregistre la modification, puis renvoie le matériel mis à jour à la liste.
 * @param input saisie du formulaire
 */
async function save(input: MaterialInput): Promise<void> {
  await runSave(async () => {
    const updated = await materialsServices.update(materialId.value, input);
    show('Les modifications ont été enregistrées.', 'success');
    emit('saved', updated);
  });
}

//------------------ LIFECYCLE -------------------------------
// Les salles proposées par le formulaire sont chargées au premier passage en modification.
watch(showForm, visible => {
  if (visible && rooms.value.length === 0) void loadLocations().catch(() => undefined);
}, {immediate: true});
</script>

<template>
  <SidePanel
      :panel-key="panelKey"
      :title="title"
      :eyebrow="eyebrow"
      title-class="code"
      @close="emit('close')"
  >
    <MaterialForm
        v-if="showForm"
        class="panel-form"
        :initial="material"
        :rooms="rooms"
        :submitting="saving"
        :error-message="saveError"
        @submit="save"
        @cancel="emit('cancelEdit')"
    />
    <MaterialDetails v-else-if="material" :material="material" />
    <template v-if="!showForm" #actions>
      <Link class="button-link" :to="{ name: 'material-detail', params: { id: materialId } }">Ouvrir la fiche</Link>
      <button
          v-if="canEdit"
          type="button"
          class="secondary-button"
          @click="emit('edit')"
      >
        Modifier
      </button>
    </template>
  </SidePanel>
</template>
