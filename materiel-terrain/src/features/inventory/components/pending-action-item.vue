<script setup lang="ts">
/**
 * Une action gardée sur le téléphone : en attente, refusée par l'API, ou en conflit avec le serveur.
 * En conflit, les deux versions sont montrées et l'agent choisit laquelle garder.
 * Utilisé par : features/inventory/views/sync-view.vue
 */
import {computed} from 'vue';
import {materialFromDto} from '../../../models/material.dto.ts';
import {type PendingAction, pendingActionLabels} from '../../../models/sync.model.ts';
import {formatTime} from '../../../shared/composables/use-dates.ts';
import {locationLabel} from '../../../shared/composables/use-room-label.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  action: PendingAction;
  /** Libellé de la salle visée par l'agent. */
  target: string;
}>();

const emit = defineEmits<{
  retry: [];
  abandon: [];
  keepMine: [];
  keepServer: [];
}>();

//------------------ COMPUTED --------------------------------
/** État du serveur en cas de conflit, converti pour l'affichage. */
const server = computed(() => (props.action.conflict ? materialFromDto(props.action.conflict) : null));
const serverLabel = computed(() => {
  if (!server.value) return '';
  return server.value.archived ? 'archivé' : locationLabel(server.value.room);
});
const conflictTitle = computed(() => (props.action.kind === 'create'
    ? 'Déjà pris en compte par quelqu\'un d\'autre entre-temps.'
    : 'Déplacé par quelqu\'un d\'autre entre-temps.'));
</script>

<template>
  <li class="panel" :class="{blocked: action.error || action.conflict}">
    <p><strong>{{ pendingActionLabels[action.kind] }}</strong> · <span class="mono">{{ action.serialNumber }}</span></p>
    <p class="muted small">→ {{ target }} · {{ formatTime(new Date(action.createdAt)) }}</p>

    <template v-if="server">
      <p class="form-error">Conflit : {{ conflictTitle }}</p>
      <dl class="facts">
        <div><dt>Votre action</dt><dd>{{ target }}</dd></div>
        <div><dt>Serveur</dt><dd>{{ serverLabel }}</dd></div>
      </dl>
      <div class="row">
        <button v-if="!server.archived" type="button" class="secondary" @click="emit('keepMine')">
          {{ action.kind === 'create' ? `Le placer : ${target}` : 'Garder ma version' }}
        </button>
        <button type="button" class="ghost" @click="emit('keepServer')">Garder le serveur</button>
      </div>
    </template>

    <template v-else>
      <p v-if="action.error" class="form-error">Refusé : {{ action.error }}</p>
      <div class="row">
        <button v-if="action.error" type="button" class="secondary" @click="emit('retry')">Réessayer</button>
        <button type="button" class="ghost" @click="emit('abandon')">Abandonner</button>
      </div>
    </template>
  </li>
</template>
