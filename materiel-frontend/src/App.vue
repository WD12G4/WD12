<script lang="ts" setup>
/**
 * Coquille de l'application : barre latérale (si connecté), barre du haut sur mobile,
 * message temporaire, page courante avec transition, et alerte des remontées non lues
 * pour le gestionnaire (RG-23). Sans utilisateur connecté, la page est centrée (connexion).
 * Transition entre les pages : le navigateur s'en charge s'il connaît l'API View Transitions
 * (router/view-transition.ts) ; sinon, fondu de Vue (<Transition name="page">).
 */
import {computed, ref, watch} from 'vue';
import {useRoute} from 'vue-router';
import {useAuth} from './shared/composables/use-auth.ts';
import {useFlashMessage} from './shared/composables/use-flash-message.ts';
import {useNavLinks} from './shared/composables/use-nav-links.ts';
import {navigateTo} from './router/navigation.ts';
import {viewTransitionsSupported} from './router/view-transition.ts';
import AppNav from './shared/components/app-nav.vue';
import AppTopbar from './shared/components/app-topbar.vue';
import FlashMessage from './shared/components/flash-message.vue';
import ReportAlert from './features/reports/components/report-alert.vue';
import {useUnreadReports} from './features/reports/composables/use-unread-reports.ts';

//------------------ DATAS -----------------------------------
const route = useRoute();
const {currentUser, logout} = useAuth();
const {visibleLinks} = useNavLinks();
const {clear: clearFlash} = useFlashMessage();
const {unreadCount, visible: showReportAlert, dismiss: dismissReportAlert} = useUnreadReports();
/** Menu latéral ouvert (mobile uniquement : sur grand écran il est toujours visible). */
const menuOpen = ref(false);

//------------------ COMPUTED --------------------------------
/** Mise en page : avec barre latérale si connecté, centrée sinon. */
const shellClass = computed(() => (currentUser.value ? 'app-shell' : 'guest-shell'));

//------------------ FUNCTIONS -------------------------------
/** Déconnecte l'utilisateur puis affiche la page de connexion (sans message de l'utilisateur précédent). */
async function signOut(): Promise<void> {
  menuOpen.value = false;
  await logout();
  clearFlash();
  
  await navigateTo({name: 'login'});
}

/** Ouvre ou ferme le menu latéral. */
function toggleMenu(): void {
  menuOpen.value = !menuOpen.value;
}

//------------------ LIFECYCLE -------------------------------
// Sur mobile, le menu se referme dès qu'on change de page.
watch(() => route.fullPath, () => {
  menuOpen.value = false;
});
</script>

<template>
  <div :class="shellClass">
    <!-- Menu, seulement pour un utilisateur connecté -->
    <template v-if="currentUser">
      <AppNav
          :links="visibleLinks"
          :open="menuOpen"
          :user="currentUser"
          @close="menuOpen = false"
          @logout="signOut"
      />
      <AppTopbar :menu-open="menuOpen" @toggle="toggleMenu" />
    </template>

    <main class="content">
      <FlashMessage />
      <ReportAlert
          v-if="showReportAlert"
          :count="unreadCount"
          @dismiss="dismissReportAlert"
      />

      <!-- Page courante : animée par le navigateur (View Transitions), sinon par Vue -->
      <router-view v-slot="{ Component, route: current }">
        <div
            v-if="viewTransitionsSupported"
            :key="current.fullPath"
            class="page"
        >
          <component :is="Component" />
        </div>
        <Transition
            v-else
            mode="out-in"
            name="page"
        >
          <div :key="current.fullPath" class="page">
            <component :is="Component" />
          </div>
        </Transition>
      </router-view>
    </main>
  </div>
</template>
