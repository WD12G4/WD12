<script setup lang="ts">
/**
 * Position de l'agent et bilan de la salle : déclarés, vus (par n'importe quel agent), manquants (déclarés, pas encore vus).
 * Utilisé par : features/scan/views/scan-view.vue
 */
import type {Material} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {roomLabel} from '../../../shared/composables/use-room-label.ts';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  room: Room | null;
  expected: Material[];
  missing: Material[];
  seenCount: number;
  /** Vrai pour MANAGER et ADMIN : effacer les constats de la salle, pour tous. */
  canReset: boolean;
}>();

const emit = defineEmits<{
  /** Ouvrir le choix de la salle. */
  change: [];
  /** Recommencer la reconnaissance de la salle. */
  reset: [];
}>();
</script>

<template>
  <section class="position">
    <div class="position-head">
      <div>
        <p class="mono small">POSITION</p>
        <p class="position-label">{{ room ? roomLabel(room) : 'Aucune salle choisie' }}</p>
      </div>
      <button type="button" class="secondary" @click="emit('change')">{{ room ? 'Changer' : 'Choisir' }}</button>
    </div>

    <details v-if="room" class="room-check">
      <summary>
        <span class="counter">Déclarés <strong>{{ expected.length }}</strong></span>
        <span class="counter">Vus <strong>{{ seenCount }}</strong></span>
        <span class="counter" :class="{alert: missing.length > 0}">Manquants <strong>{{ missing.length }}</strong></span>
      </summary>
      <p v-if="missing.length === 0" class="muted small">Tous les matériels déclarés ici ont été vus.</p>
      <ul v-else class="plain-list">
        <li v-for="material in missing" :key="material.id" class="mono">{{ material.serialNumber }}</li>
      </ul>
      <div class="row">
        <RouterLink :to="{name: 'room', params: {id: room.id}}" class="small">Compte rendu de la salle →</RouterLink>
        <button v-if="canReset" type="button" class="ghost small" @click="emit('reset')">Recommencer cette salle</button>
      </div>
    </details>
    <p v-else class="muted small">Sans salle, les matériels sont seulement identifiés.</p>
  </section>
</template>
