<script setup lang="ts">
/**
 * OUTIL DE TEST (mode développement uniquement) : le bloc de l'utilisateur (pastille, nom, rôle)
 * devient une liste pour changer de compte de démonstration (Administrateur, Gestionnaire,
 * Formateur) sans se déconnecter. Absent de la version de production (voir user-card.vue).
 * Utilisé par : user-card.vue
 */
import {computed, ref, watch} from 'vue';
import type {User} from '../../models/user.model.ts';
import {demoAccountOptions, switchAccount} from '../composables/use-dev-accounts.ts';
import AppSelect from './app-select.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Utilisateur connecté. */
  user: User;
}>();

//------------------ DATAS -----------------------------------
const selected = ref(props.user.username);

//------------------ COMPUTED --------------------------------
/** Initiale affichée dans la pastille de l'utilisateur. */
const initial = computed(() => props.user.username.charAt(0).toUpperCase());

//------------------ FUNCTIONS -------------------------------
/** Change de compte ; en cas d'échec, la liste revient au compte en cours. */
async function onSelect(username: string): Promise<void> {
  if (username === props.user.username) return;
  if (!(await switchAccount(username))) selected.value = props.user.username;
}

//------------------ LIFECYCLE -------------------------------
watch(selected, onSelect);
watch(() => props.user.username, username => (selected.value = username));
</script>

<template>
  <div class="user-switch">
    <label for="dev-user-switch" class="visually-hidden">Tester avec le compte</label>
    <AppSelect
        id="dev-user-switch"
        v-model="selected"
        :options="demoAccountOptions"
    >
      <template #trigger="{ label }">
        <span class="avatar" aria-hidden="true">{{ initial }}</span>
        <span class="user-meta">
          <strong>{{ user.username }}</strong>
          <span>{{ label }}</span>
        </span>
      </template>
    </AppSelect>
  </div>
</template>
