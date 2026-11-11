<script setup lang="ts">
/**
 * Détail d'un poste dans le panneau latéral : état, nom d'hôte, adresse MAC, applications ouvertes
 * (depuis quand, bouton Arrêter) et, pour l'administrateur, la masterisation, à part en bas.
 * Composant de présentation : émet des événements.
 * Utilisé par : supervision-view.vue
 * RG : RG-26, RG-28 (masterisation par l'administrateur), RG-29 (identification du poste)
 */
import {computed} from 'vue';
import type {ProcessInfo} from '../../../models/supervision.model.ts';
import type {WorkstationView} from '../composables/use-supervision.ts';
import {openAppsLabel, statusLabel} from '../composables/use-station-labels.ts';
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
const hostname = computed(() => props.view?.workstation.hostname ?? 'Inconnu (agent non connecté)');
const macAddress = computed(() => props.view?.workstation.macAddress ?? '—');
const status = computed(() => (props.view ? statusLabel(props.view.workstation) : ''));
const isOnline = computed(() => props.view?.workstation.status === 'online');
const emptyLabel = computed(() => (props.view ? `${openAppsLabel(props.view.workstation, 0)}.` : ''));
/** Total affiché à côté du titre de la section (« Applications ouvertes · 21 »). */
const openCount = computed(() => props.view?.processes.length ?? 0);
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
      <dl class="detail-grid">
        <div><dt>État</dt><dd>{{ status }}</dd></div>
        <div><dt>Adresse MAC</dt><dd class="code">{{ macAddress }}</dd></div>
        <div class="full-width"><dt>Nom d'hôte</dt><dd class="code">{{ hostname }}</dd></div>
      </dl>
      <section class="panel-section">
        <h3 class="eyebrow">
          Applications ouvertes · <span class="panel-count">{{ openCount }}</span>
        </h3>
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
