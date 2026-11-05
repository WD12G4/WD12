<script setup lang="ts">
/**
 * Coquille de la PWA : barre d'état (si connecté), message temporaire, page courante.
 * Lance aussi la synchronisation automatique : à la connexion, au retour du réseau,
 * au retour au premier plan, et régulièrement tant que des actions attendent.
 */
import {computed, onBeforeUnmount, onMounted, watch} from 'vue';
import {useRouter} from 'vue-router';
import {useAuth} from './shared/composables/use-auth.ts';
import {useFlashMessage} from './shared/composables/use-flash-message.ts';
import {useNetwork} from './shared/composables/use-network.ts';
import {useSyncQueue} from './features/inventory/composables/use-sync-queue.ts';
import {useSynchronize} from './features/inventory/composables/use-synchronize.ts';
import FlashMessage from './shared/components/flash-message.vue';
import StatusBar from './shared/components/status-bar.vue';

/** Intervalle des nouvelles tentatives d'envoi quand des actions attendent. */
const AUTO_FLUSH_MS = 20_000;

//------------------ DATAS -----------------------------------
const router = useRouter();
const {currentUser, offlineSession, logout} = useAuth();
const {clear: clearFlash} = useFlashMessage();
const {online} = useNetwork();
const {pendingCount, blockedCount, readyActions, flush} = useSyncQueue();
const {synchronize} = useSynchronize();
let flushTimer: ReturnType<typeof setInterval> | undefined;

//------------------ COMPUTED --------------------------------
const linkOnline = computed(() => online.value && !offlineSession.value);

//------------------ FUNCTIONS -------------------------------
/** Déconnecte l'agent ; les actions non envoyées restent sur le téléphone. */
async function signOut(): Promise<void> {
  if (pendingCount.value > 0 && !window.confirm(
      `${pendingCount.value} action(s) pas encore envoyée(s) resteront sur ce téléphone. Se déconnecter ?`)) {
    return;
  }
  await logout();
  clearFlash();
  await router.push({name: 'login'});
}

function syncIfSignedIn(): void {
  if (currentUser.value) void synchronize();
}

function onVisibilityChange(): void {
  if (!document.hidden) syncIfSignedIn();
}

//------------------ LIFECYCLE -------------------------------
// À la connexion (ou à la restauration de session) : envoi des actions en attente puis parc à jour.
watch(() => currentUser.value?.username, syncIfSignedIn);

onMounted(() => {
  window.addEventListener('online', syncIfSignedIn);
  document.addEventListener('visibilitychange', onVisibilityChange);
  flushTimer = setInterval(() => {
    if (currentUser.value && readyActions.value.length > 0) void flush();
  }, AUTO_FLUSH_MS);
});

onBeforeUnmount(() => {
  window.removeEventListener('online', syncIfSignedIn);
  document.removeEventListener('visibilitychange', onVisibilityChange);
  clearInterval(flushTimer);
});
</script>

<template>
  <div class="shell" :class="{guest: !currentUser}">
    <StatusBar
        v-if="currentUser"
        :user="currentUser" :online="linkOnline" :pending-count="pendingCount" :blocked-count="blockedCount"
        @logout="signOut"
    />
    <main class="content">
      <FlashMessage />
      <router-view />
    </main>
  </div>
</template>
