<script setup lang="ts">
/**
 * Badges de statut d'un matériel : statut principal, plus « Bientôt expiré » le cas échéant.
 * La couleur est toujours accompagnée d'un texte (accessibilité).
 * Utilisé par : features/materials (liste, fiche).
 * RG : RG-07, RG-10, RG-24
 */
import {computed} from 'vue';
import type {Material} from '../../models/material.model.ts';
import {
  isMaterialExpiring, materialStatus, materialStatusLabels,
} from '../composables/use-material-status.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Matériel dont on affiche le statut. */
  material: Material;
}>();

//------------------ COMPUTED --------------------------------
const status = computed(() => materialStatus(props.material));
const expiring = computed(() => isMaterialExpiring(props.material));
</script>

<template>
  <span class="badge" :class="status">{{ materialStatusLabels[status] }}</span>
  <span v-if="expiring" class="badge expiring">Bientôt expiré</span>
</template>
