<script setup lang="ts">
/**
 * Aperçu latéral d'un matériel : la fiche s'ouvre à droite de la liste, sans la quitter ni perdre ses
 * filtres (panneau partagé side-panel.vue). Un clic sur un autre matériel change l'aperçu.
 * Utilisé par : material-list.vue (liste du matériel, fiche d'une salle)
 * RG : RG-16 (« Modifier » réservé à l'administrateur)
 */
import {computed} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import Link from '../../../shared/components/Link.vue';
import SidePanel from '../../../shared/components/side-panel.vue';
import MaterialDetails from './material-details.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Matériel affiché, ou null quand l'aperçu est fermé. */
  material: Material | null;
}>();

const emit = defineEmits<{
  /** Fermeture demandée (×, Échap, voile). */
  close: [];
}>();

//------------------ DATAS -----------------------------------
const {hasRole} = useAuth();

//------------------ COMPUTED --------------------------------
const canEdit = computed(() => hasRole('ADMIN'));
const materialId = computed(() => props.material?.id ?? 0);
const panelKey = computed(() => props.material?.id ?? null);
const title = computed(() => props.material?.serialNumber ?? '');
</script>

<template>
  <SidePanel
      :panel-key="panelKey"
      :title="title"
      eyebrow="Aperçu du matériel"
      title-class="code"
      @close="emit('close')"
  >
    <MaterialDetails v-if="material" :material="material" />
    <template #actions>
      <Link class="button-link" :to="{ name: 'material-detail', params: { id: materialId } }">Ouvrir la fiche</Link>
      <Link
          v-if="canEdit"
          class="button-link secondary-button"
          :to="{ name: 'material-edit', params: { id: materialId } }"
      >
        Modifier
      </Link>
    </template>
  </SidePanel>
</template>
