<script setup lang="ts">
/**
 * Coquille de l'application : en-tête, menu (si connecté), message temporaire, page courante,
 * et alerte des remontées non lues pour le gestionnaire (RG-23).
 */
import {useAuth} from './shared/composables/use-auth.ts';
import {useFlashMessage} from './shared/composables/use-flash-message.ts';
import {useNavLinks} from './shared/composables/use-nav-links.ts';
import {navigateTo} from './router/navigation.ts';
import AppNav from './shared/components/app-nav.vue';
import FlashMessage from './shared/components/flash-message.vue';
import ReportAlert from './features/reports/components/report-alert.vue';
import {useUnreadReports} from './features/reports/composables/use-unread-reports.ts';

//------------------ DATAS -----------------------------------
const {currentUser, logout} = useAuth();
const {visibleLinks} = useNavLinks();
const {clear: clearFlash} = useFlashMessage();
const {unreadCount, visible: showReportAlert, dismiss: dismissReportAlert} = useUnreadReports();

//------------------ FUNCTIONS -------------------------------
/** Déconnecte l'utilisateur puis affiche la page de connexion (sans message de l'utilisateur précédent). */
async function signOut(): Promise<void> {
  await logout();
  clearFlash();
  await navigateTo({name: 'login'});
}
</script>

<template>
  <main class="container">
    <!-- En-tête -->
    <header class="header">
      <h1>Gestion du parc</h1>
      <p>Le matériel informatique et les salles de l'école, au même endroit.</p>
    </header>

    <!-- Menu, seulement pour un utilisateur connecté -->
    <AppNav
        v-if="currentUser"
        :links="visibleLinks"
        :user="currentUser"
        @logout="signOut"
    />

    <FlashMessage />
    <ReportAlert
        v-if="showReportAlert"
        :count="unreadCount"
        @dismiss="dismissReportAlert"
    />

    <router-view />
  </main>
</template>

<style scoped>
.container {
  width: min(1200px, calc(100% - 2rem));
  margin: 0 auto;
  padding: 2rem 0;
}

.header {
  margin-bottom: 2rem;
}

.header h1 {
  margin-bottom: 0.25rem;
}

.header p {
  margin-top: 0;
}
</style>
