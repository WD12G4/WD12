<script setup lang="ts">
/**
 * Onglet « Liste noire » de la supervision (administrateur) : le catalogue des applications vues sur les
 * postes, les interdites en premier. Une application interdite est fermée automatiquement sur tous les
 * postes dès qu'elle est détectée, et l'arrêt apparaît dans le journal. Recherche par nom.
 * Utilisé par : supervision-view.vue
 * RG : RG-27 (liste noire, définie par l'administrateur, entretien 4)
 */
import {computed, onMounted, ref} from 'vue';
import type {Application} from '../../../models/application.model.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useBlacklist} from '../composables/use-blacklist.ts';
import LoadState from '../../../shared/components/load-state.vue';
import FormError from '../../../shared/components/form-error.vue';
import BlacklistItem from './blacklist-item.vue';

//------------------ DATAS -----------------------------------
const {applications, forbiddenCount, loadApplications, setBlacklisted} = useBlacklist();
const {loading, errorMessage, run} = useAsyncState();
const {loading: busy, errorMessage: actionError, run: act} = useAsyncState();
const search = ref('');

//------------------ COMPUTED --------------------------------
const visible = computed(() => {
  const term = search.value.trim().toLowerCase();
  return applications.value.filter(app => !term || `${app.displayName} ${app.name}`.toLowerCase().includes(term));
});
const summary = computed(() => `${forbiddenCount.value} interdite(s) sur ${applications.value.length} application(s) vues sur les postes.`);
const showEmpty = computed(() => !loading.value && errorMessage.value === null && visible.value.length === 0);

//------------------ FUNCTIONS -------------------------------
/** Charge le catalogue. */
async function load(): Promise<void> {
  await run(loadApplications);
}

/**
 * Interdit ou autorise une application.
 * @param app application
 * @param blacklisted vrai pour l'interdire
 */
async function toggle(app: Application, blacklisted: boolean): Promise<void> {
  await act(() => setBlacklisted(app, blacklisted));
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <section class="blacklist" aria-label="Liste noire des applications">
    <p class="muted">Une application interdite est fermée automatiquement sur tous les postes dès qu'elle est lancée. {{ summary }}</p>
    <div class="field">
      <label for="blacklist-search">Rechercher une application</label>
      <input
          id="blacklist-search"
          v-model="search"
          type="search"
          placeholder="Nom de l'application ou du processus"
      >
    </div>
    <LoadState
        :loading="loading"
        :error-message="errorMessage"
        @retry="load"
    />
    <FormError :message="actionError" />
    <p v-if="showEmpty" class="empty-state">Aucune application : elles apparaissent ici dès qu'un agent les remonte.</p>
    <ul v-else class="process-list">
      <li v-for="app in visible" :key="app.id">
        <BlacklistItem
            :app="app"
            :busy="busy"
            @toggle="toggle"
        />
      </li>
    </ul>
  </section>
</template>
