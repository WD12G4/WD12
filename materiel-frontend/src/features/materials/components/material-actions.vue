<script setup lang="ts">
/**
 * Boutons de gestion d'une fiche matériel : modifier et archiver (gestionnaire et administrateur).
 * Composant de présentation : les droits arrivent par la prop canEdit.
 * Utilisé par : material-detail-view.vue
 * RG : RG-10 (archivage), RG-16 (gestionnaire et administrateur)
 */
import {computed} from 'vue';
import type {Material} from '../../../models/material.model.ts';
import Link from '../../../shared/components/Link.vue';
import FormError from '../../../shared/components/form-error.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Matériel affiché. */
  material: Material;
  /** Vrai si l'utilisateur est gestionnaire ou administrateur. */
  canEdit: boolean;
  /** Erreur de l'archivage, ou null. */
  errorMessage: string | null;
}>();

const emit = defineEmits<{
  /** Demande d'archivage. */
  archive: [material: Material];
}>();

//------------------ COMPUTED --------------------------------
/** Un matériel déjà archivé n'a plus de bouton Archiver. */
const canArchive = computed(() => props.canEdit && !props.material.archived);
</script>

<template>
  <div v-if="canEdit" class="actions">
    <Link class="button-link" :to="{ name: 'material-edit', params: { id: material.id } }">Modifier</Link>
    <button
        v-if="canArchive"
        type="button"
        class="delete-button"
        @click="emit('archive', material)"
    >
      Archiver
    </button>
  </div>
  <FormError :message="errorMessage" />
</template>
