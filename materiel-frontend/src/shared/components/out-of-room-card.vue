<script setup lang="ts">
/**
 * Carte affichée au formateur qui ouvre une page hors de sa salle de cours (adresse tapée à la main,
 * lien copié) : la page n'est pas affichée, un bouton le ramène à sa salle.
 * Ce filtre est fait par le front seulement : l'API ne limite pas encore le formateur à une salle.
 * Composant de présentation.
 * Utilisé par : room-detail-view.vue, material-detail-view.vue
 * RG : RG-34 (formateur : sa salle seulement)
 */
import type {RouteTarget} from '../../router/route-types.ts';
import Card from './card.vue';
import Link from './Link.vue';

//------------------ PROPS / EMITS ---------------------------
defineProps<{
  /** Titre de la carte, ex. « Salle non accessible ». */
  title: string;
  /** Page de la salle du formateur. */
  to: RouteTarget;
  /** Texte du bouton, ex. « Ouvrir ma salle ». */
  label: string;
}>();
</script>

<template>
  <Card :title="title">
    <template #body>
      <p>Vous n'avez accès qu'à votre salle de cours.</p>
      <Link class="button-link" :to="to">{{ label }}</Link>
    </template>
  </Card>
</template>
