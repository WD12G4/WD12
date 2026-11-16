<script setup lang="ts">
/**
 * Tableau de bord du formateur : aperçu de la supervision de sa salle (postes en ligne, applications
 * ouvertes, état de chaque poste) et lien vers la page Supervision. Vrais postes et postes simulés.
 * Un clic sur un poste ouvre la supervision avec ce poste déjà ouvert (?poste=).
 * Composant de présentation : les postes arrivent de home-view.vue (use-supervision.ts).
 * Utilisé par : home-view.vue
 * RG : RG-25, RG-30 (stagiaire du poste)
 */
import {computed} from 'vue';
import type {WorkstationView} from '../../supervision/composables/use-supervision.ts';
import {openAppsLabel, statusLabel} from '../../supervision/composables/use-station-labels.ts';
import Card from '../../../shared/components/card.vue';
import Link from '../../../shared/components/Link.vue';
import {countLabel, plural} from '../../../shared/composables/use-plural.ts';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Postes de la salle et leurs applications. */
  views: WorkstationView[];
}>();

/** Au-delà, la liste renvoie à la page Supervision. */
const MAX_ROWS = 6;

//------------------ COMPUTED --------------------------------
const online = computed(() => props.views.filter(view => view.workstation.status === 'online').length);
const openApps = computed(() => props.views.reduce((total, view) => total + view.processes.length, 0));
const summary = computed(() => `${countLabel(online.value, 'poste')} en ligne sur ${props.views.length} · ${countLabel(openApps.value, 'application')} ${plural(openApps.value, 'ouverte')}`);
const isEmpty = computed(() => props.views.length === 0);
/** Une ligne par poste : stagiaire (ou nom du poste), nom du poste, état, applications ouvertes. */
const rows = computed(() => props.views.slice(0, MAX_ROWS).map(({workstation, processes}) => ({
  id: workstation.id,
  status: workstation.status,
  title: workstation.sessionUser ?? workstation.name,
  subtitle: workstation.sessionUser ? workstation.name : '',
  statusText: statusLabel(workstation),
  appsText: openAppsLabel(workstation, processes.length),
  to: {name: 'supervision', query: {poste: String(workstation.id)}} as const,
})));
</script>

<template>
  <Card title="Supervision">
    <template #header>
      <p>{{ summary }}</p>
    </template>
    <template #body>
      <p v-if="isEmpty" class="empty-state">Aucun poste supervisé dans cette salle.</p>
      <ul v-else class="preview-list">
        <li v-for="row in rows" :key="row.id">
          <Link
              class="preview-row"
              :class="row.status"
              :to="row.to"
          >
            <span class="preview-text">
              <strong>{{ row.title }}</strong>
              <span class="muted">{{ row.subtitle }}</span>
            </span>
            <span class="muted">{{ row.appsText }}</span>
            <span class="station-status">{{ row.statusText }}</span>
          </Link>
        </li>
      </ul>
    </template>
    <template #footer>
      <Link class="preview-link" :to="{ name: 'supervision' }">Ouvrir la supervision</Link>
    </template>
  </Card>
</template>
