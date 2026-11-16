<script setup lang="ts">
/**
 * Tableau de bord du formateur : aperçu des postes (unités centrales) de sa salle et de leur état,
 * le reste du matériel en pastilles, et un lien vers la fiche de la salle.
 * Composant de présentation.
 * Utilisé par : home-view.vue
 * RG : RG-07, RG-24 (fin de disponibilité signalée sur chaque poste)
 */
import {computed} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import type {TypeCount} from '../composables/use-room-overview.ts';
import Card from '../../../shared/components/card.vue';
import Link from '../../../shared/components/Link.vue';
import MaterialStatusBadge from '../../../shared/components/material-status-badge.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Unités centrales de la salle (non archivées). */
  ucs: Material[];
  /** Autres types de matériel de la salle, avec leur nombre. */
  others: TypeCount[];
  /** Salle de cours du formateur. */
  roomId: number;
}>();

/** Au-delà, la liste renvoie à la fiche de la salle. */
const MAX_ROWS = 6;

//------------------ COMPUTED --------------------------------
const shown = computed(() => props.ucs.slice(0, MAX_ROWS));
const isEmpty = computed(() => props.ucs.length === 0);
const moreLabel = computed(() => (props.ucs.length > MAX_ROWS ? `Et ${props.ucs.length - MAX_ROWS} autre(s) poste(s).` : ''));
const hasOthers = computed(() => props.others.length > 0);
</script>

<template>
  <Card title="Postes de la salle">
    <template #header>
      <p>Les unités centrales de votre salle et leur état.</p>
    </template>
    <template #body>
      <p v-if="isEmpty" class="empty-state">Aucune unité centrale dans cette salle.</p>
      <ul v-else class="preview-list">
        <li
            v-for="uc in shown"
            :key="uc.id"
            class="preview-row"
        >
          <Link class="serial" :to="{ name: 'material-detail', params: { id: uc.id } }">{{ uc.serialNumber }}</Link>
          <MaterialStatusBadge :material="uc" />
        </li>
      </ul>
      <p v-if="moreLabel" class="muted">{{ moreLabel }}</p>
      <!-- Le reste du matériel de la salle, en nombre par type -->
      <ul v-if="hasOthers" class="type-chips">
        <li v-for="typeCount in others" :key="typeCount.type">
          {{ typeCount.label }} <strong>{{ typeCount.count }}</strong>
        </li>
      </ul>
    </template>
    <template #footer>
      <Link class="preview-link" :to="{ name: 'room-detail', params: { id: roomId } }">Tout le matériel de la salle</Link>
    </template>
  </Card>
</template>
