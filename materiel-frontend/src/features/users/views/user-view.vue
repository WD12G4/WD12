<script setup lang="ts">
/**
 * Gestion des comptes (ADMIN) : création, modification du rôle, du mot de passe et de l'activation,
 * suppression.
 * RG : RG-14, RG-19, RG-20, RG-32
 */
import {computed, onMounted, ref} from 'vue';
import type {User, UserCreateInput, UserUpdateInput} from '../../../models/user.model.ts';
import {usersServices} from '../../../services/users.services.ts';
import {useAuth} from '../../../shared/composables/use-auth.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import {useFlashMessage} from '../../../shared/composables/use-flash-message.ts';
import Card from '../../../shared/components/card.vue';
import LoadState from '../../../shared/components/load-state.vue';
import UserCreateForm from '../components/user-create-form.vue';
import UserEditForm from '../components/user-edit-form.vue';
import UserList from '../components/user-list.vue';

//------------------ DATAS -----------------------------------
const {currentUser} = useAuth();
const {show} = useFlashMessage();
const {loading, errorMessage, run} = useAsyncState();
const {loading: saving, errorMessage: saveError, run: runSave} = useAsyncState();
const users = ref<User[]>([]);
/** Compte en cours de modification, ou null. */
const editing = ref<User | null>(null);
/** Changer cette clé recrée le formulaire de création, donc le vide (après un succès). */
const formKey = ref(0);

//------------------ COMPUTED --------------------------------
const currentUserId = computed(() => currentUser.value?.id ?? null);
const editTitle = computed(() => `Modifier le compte ${editing.value?.username ?? ''}`);

//------------------ FUNCTIONS -------------------------------
/** Charge la liste des comptes. */
async function load(): Promise<void> {
  await run(async () => {
    users.value = await usersServices.findAll();
  });
}

/** Exécute un enregistrement, puis affiche un message et recharge la liste. */
async function saveThen(operation: () => Promise<unknown>, message: string): Promise<void> {
  const succeeded = await runSave(async () => {
    await operation();
  });
  if (succeeded) {
    show(message, 'success');
    editing.value = null;
    formKey.value++;
    await load();
  }
}

/** Crée un compte. */
async function create(input: UserCreateInput): Promise<void> {
  await saveThen(() => usersServices.create(input), `Le compte ${input.username} a été créé.`);
}

/** Enregistre la modification du compte en cours d'édition. */
async function update(input: UserUpdateInput): Promise<void> {
  const user = editing.value;
  if (!user) return;
  await saveThen(() => usersServices.update(user.id, input), `Le compte ${user.username} a été modifié.`);
}

/** Supprime après confirmation (suppression définitive côté backend). */
async function remove(user: User): Promise<void> {
  if (!window.confirm(`Supprimer définitivement le compte ${user.username} ?`)) return;
  await run(async () => {
    await usersServices.delete(user.id);
    users.value = await usersServices.findAll();
  });
}

//------------------ LIFECYCLE -------------------------------
onMounted(load);
</script>

<template>
  <Card v-if="editing" :title="editTitle">
    <template #body>
      <UserEditForm
          :user="editing"
          :submitting="saving"
          :error-message="saveError"
          @submit="update"
          @cancel="editing = null"
      />
    </template>
  </Card>
  <Card v-else title="Nouveau compte">
    <template #body>
      <UserCreateForm
          :key="formKey"
          :submitting="saving"
          :error-message="saveError"
          @submit="create"
      />
    </template>
  </Card>
  <Card title="Comptes">
    <template #body>
      <LoadState
          :loading="loading"
          :error-message="errorMessage"
          @retry="load"
      />
      <UserList
          v-if="!loading"
          :users="users"
          :current-user-id="currentUserId"
          @edit="editing = $event"
          @remove="remove"
      />
    </template>
  </Card>
</template>
