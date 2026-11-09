<script setup lang="ts">
/**
 * Lien « ← Retour » qui ramène à la page d'où l'on vient : « Retour au tableau de bord » si la
 * fiche d'une salle a été ouverte depuis le tableau de bord, « Retour aux salles » depuis la liste.
 * Arrivée directe (adresse tapée, page rechargée) : retour à la page parente « fallback ».
 * Cmd ou Ctrl + clic ouvre la page dans un nouvel onglet, comme un lien ordinaire.
 * Utilisé par : room-detail-view, material-detail-view, report-document-view
 */
import {computed} from 'vue';
import {useRoute} from 'vue-router';
import type {RouteTarget} from '../../router/route-types.ts';
import {backLinkTarget, followBackLink} from '../../router/navigation.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Page parente, si l'on n'arrive pas d'une autre page de l'application. */
  fallback: RouteTarget;
}>();

//------------------ DATAS -----------------------------------
const route = useRoute();

//------------------ COMPUTED --------------------------------
/** Recalculé à chaque changement de page : history.state change en même temps que la route. */
const back = computed(() => {
  void route.fullPath;
  return backLinkTarget(props.fallback);
});

//------------------ FUNCTIONS -------------------------------
/** Clic simple : retour dans l'application. Clic avec modificateur : laissé au navigateur. */
function onClick(event: MouseEvent): void {
  if (event.metaKey || event.ctrlKey || event.shiftKey || event.button !== 0) return;
  event.preventDefault();
  void followBackLink(back.value);
}
</script>

<template>
  <a
      class="back-link"
      :href="back.href"
      @click="onClick"
  >
    ← {{ back.label }}
  </a>
</template>
