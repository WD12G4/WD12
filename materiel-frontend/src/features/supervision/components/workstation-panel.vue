<script setup lang="ts">
/**
 * Détail d'un poste dans le panneau latéral : stagiaire, état, adresse MAC, applications ouvertes
 * (depuis quand, bouton Arrêter) et, pour l'administrateur, la masterisation, à part en bas.
 * Composant de présentation : émet des événements.
 * Utilisé par : supervision-view.vue
 * RG : RG-26, RG-28 (masterisation par l'administrateur), RG-29 (identification du poste)
 */
import {computed} from 'vue';
import type {ProcessInfo} from '../../../models/supervision.model.ts';
import type {WorkstationView} from '../composables/use-supervision.ts';
import {statusLabel, traineeInitials} from '../composables/use-station-labels.ts';
import SidePanel from '../../../shared/components/side-panel.vue';
import ProcessList from './process-list.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Poste affiché, ou null quand le panneau est fermé. */
  view: WorkstationView | null;
  /** Vrai si l'utilisateur peut masteriser (ADMIN, RG-28). */
  canReimage: boolean;
  /** Vrai pendant une action. */
  busy: boolean;
}>();

const emit = defineEmits<{
  close: [];
  kill: [process: ProcessInfo];
  reimage: [view: WorkstationView];
}>();

//------------------ COMPUTED --------------------------------
const panelKey = computed(() => props.view?.workstation.id ?? null);
const title = computed(() => props.view?.workstation.name ?? '');
const initials = computed(() => traineeInitials(props.view?.workstation.traineeLabel ?? ''));
const status = computed(() => (props.view ? statusLabel(props.view.workstation) : ''));
const isOnline = computed(() => props.view?.workstation.status === 'online');
const emptyLabel = computed(() => (isOnline.value ? 'Aucune application ouverte.' : `Poste indisponible (${status.value.toLowerCase()}).`));
/** Poste à masteriser (administrateur seulement), ou null pour masquer le bouton. */
const reimageTarget = computed(() => (props.canReimage ? props.view : null));
/** Pas de masterisation sur un poste éteint ou déjà en cours de masterisation. */
const reimageDisabled = computed(() => props.busy || !isOnline.value);
</script>

<template>
  <SidePanel
      :panel-key="panelKey"
      :title="title"
      eyebrow="Poste de la salle"
      @close="emit('close')"
  >
    <template v-if="view">
      <p class="station-trainee">
        <span class="trainee-avatar" aria-hidden="true">{{ initials }}</span>
        {{ view.workstation.traineeLabel }}
      </p>
      <dl class="detail-grid">
        <div><dt>État</dt><dd>{{ status }}</dd></div>
        <div><dt>Adresse MAC</dt><dd class="code">{{ view.workstation.macAddress }}</dd></div>
      </dl>
      <section class="panel-section">
        <h3 class="eyebrow">Applications ouvertes</h3>
        <ProcessList
            :processes="view.processes"
            :empty-label="emptyLabel"
            :busy="busy"
            @kill="emit('kill', $event)"
        />
      </section>
    </template>
    <template v-if="reimageTarget" #actions>
      <button
          type="button"
          class="secondary-button"
          :disabled="reimageDisabled"
          @click="emit('reimage', reimageTarget)"
      >
        Masteriser le poste
      </button>
      <span class="field-hint">Réinstalle le poste : le travail non enregistré est perdu.</span>
    </template>
  </SidePanel>
</template>
