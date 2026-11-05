<script setup lang="ts">
/**
 * Synchronisation : état du parc en cache, actions en attente d'envoi, actions refusées par l'API.
 * Une action refusée (ex. numéro déjà pris entre-temps) se réessaie ou s'abandonne ici.
 */
import {computed} from 'vue';
import {pendingActionLabels} from '../../../models/sync.model.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useFlashMessage} from '../../../shared/composables/use-flash-message.ts';
import {formatDate, formatTime} from '../../../shared/composables/use-dates.ts';
import {roomLabel} from '../../../shared/composables/use-room-label.ts';
import {useInventory} from '../composables/use-inventory.ts';
import {useSyncQueue} from '../composables/use-sync-queue.ts';
import {useSynchronize} from '../composables/use-synchronize.ts';

//------------------ DATAS -----------------------------------
const {materials, rooms, syncedAt, roomById} = useInventory();
const {queue, flushing, retry, discard} = useSyncQueue();
const {synchronize} = useSynchronize();
const {show} = useFlashMessage();
const {loading, run} = useAsyncState();

//------------------ COMPUTED --------------------------------
const lastSync = computed(() =>
    (syncedAt.value ? `${formatDate(syncedAt.value)} à ${formatTime(syncedAt.value)}` : 'jamais'));

//------------------ FUNCTIONS -------------------------------
/** Libellé de la salle cible d'une action. */
function targetLabel(roomId: number | null): string {
  if (roomId === null) return 'En stock';
  const room = roomById(roomId);
  return room ? roomLabel(room) : `Salle #${roomId}`;
}

async function syncNow(): Promise<void> {
  await run(async () => {
    const ok = await synchronize();
    show(ok ? 'Parc à jour.' : "Serveur injoignable : les actions restent sur le téléphone.", ok ? 'success' : 'error');
  });
}

function abandon(id: string): void {
  if (window.confirm('Abandonner cette action ? Elle ne sera jamais envoyée.')) discard(id);
}
</script>

<template>
  <section class="stack">
    <div>
      <p class="mono small">SYNCHRONISATION</p>
      <h1>Liaison avec le serveur</h1>
    </div>

    <section class="panel">
      <p>Parc en cache : <strong>{{ materials.length }}</strong> matériels, <strong>{{ rooms.length }}</strong> salles.</p>
      <p class="muted small">Dernière synchronisation : {{ lastSync }}</p>
      <button type="button" class="primary" :disabled="loading || flushing" @click="syncNow">
        {{ loading || flushing ? 'Synchronisation…' : 'Synchroniser maintenant' }}
      </button>
    </section>

    <h2 class="mono small">ACTIONS EN ATTENTE ({{ queue.length }})</h2>
    <p v-if="queue.length === 0" class="muted">Rien en attente : tout est enregistré sur le serveur.</p>
    <ul v-else class="plain-list stack">
      <li v-for="action in queue" :key="action.id" class="panel" :class="{blocked: action.error}">
        <p><strong>{{ pendingActionLabels[action.kind] }}</strong> · <span class="mono">{{ action.serialNumber }}</span></p>
        <p class="muted small">→ {{ targetLabel(action.roomId) }} · {{ formatTime(new Date(action.createdAt)) }}</p>
        <p v-if="action.error" class="form-error">Refusé : {{ action.error }}</p>
        <div class="row">
          <button v-if="action.error" type="button" class="secondary" @click="retry(action.id)">Réessayer</button>
          <button type="button" class="ghost" @click="abandon(action.id)">Abandonner</button>
        </div>
      </li>
    </ul>
  </section>
</template>
