<script setup lang="ts">
/**
 * Détail d'un poste dans le panneau latéral : en tête, sur une ligne, le stagiaire et l'état ; puis un
 * bloc compact (nom d'hôte, système, adresse MAC, n° de série) pour laisser la place à la liste des
 * applications ouvertes (depuis quand, bouton Arrêter). Poste simulé : marque « Simulé », système, image et applications
 * installées, et, pour l'administrateur, la masterisation (assistant), à part en bas.
 * Composant de présentation : émet des événements.
 * Utilisé par : supervision-view.vue
 * RG : RG-26, RG-28 (masterisation par l'administrateur), RG-29 (identification du poste)
 */
import {computed} from 'vue';
import {type ProcessInfo, workstationOsLabels} from '../../../models/supervision.model.ts';
import type {WorkstationView} from '../composables/use-supervision.ts';
import {openAppsLabel, statusLabel} from '../composables/use-station-labels.ts';
import SidePanel from '../../../shared/components/side-panel.vue';
import AppIcon from '../../../shared/components/app-icon.vue';
import OsLogo from './os-logo.vue';
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
/** Stagiaire connecté (session ouverte sur le poste, RG-30). */
const sessionUser = computed(() => props.view?.workstation.sessionUser ?? (isOnline.value ? 'Aucune session ouverte' : '—'));
const eyebrow = computed(() => (props.view?.workstation.simulated ? 'Poste simulé · démonstration' : 'Poste de la salle'));
const installed = computed(() => props.view?.installed ?? null);
/** Système : logo d'après l'agent ; libellé complet pour un poste simulé (« Ubuntu 24.04 LTS »), sinon famille. */
const os = computed(() => props.view?.workstation.os ?? null);
const systemLabel = computed(() => installed.value?.system ?? (os.value ? workstationOsLabels[os.value] : ''));
const serialNumber = computed(() => props.view?.workstation.serialNumber ?? null);
const hostname = computed(() => props.view?.workstation.hostname ?? 'Inconnu (agent non connecté)');
const macAddress = computed(() => props.view?.workstation.macAddress ?? '—');
const status = computed(() => (props.view ? statusLabel(props.view.workstation) : ''));
const isOnline = computed(() => props.view?.workstation.status === 'online');
const emptyLabel = computed(() => (props.view ? `${openAppsLabel(props.view.workstation, 0)}.` : ''));
/** Total affiché à côté du titre de la section (« Applications ouvertes · 21 »). */
const openCount = computed(() => props.view?.processes.length ?? 0);
/** Poste à masteriser (administrateur, poste simulé seulement), ou null pour masquer le bouton. */
const reimageTarget = computed(() => (props.canReimage && props.view?.workstation.simulated ? props.view : null));
/** Pas de masterisation sur un poste éteint ou déjà en cours de masterisation. */
const reimageDisabled = computed(() => props.busy || !isOnline.value);
</script>

<template>
  <SidePanel
      :panel-key="panelKey"
      :title="title"
      :eyebrow="eyebrow"
      @close="emit('close')"
  >
    <template v-if="view">
      <p class="panel-user" title="Stagiaire (session ouverte)">
        <AppIcon name="user" />
        <strong>{{ sessionUser }}</strong>
        <span class="panel-status" :class="view.workstation.status">{{ status }}</span>
      </p>
      <dl class="panel-facts">
        <div><dt>Nom d'hôte</dt><dd class="code">{{ hostname }}</dd></div>
        <div v-if="systemLabel"><dt>Système</dt><dd class="os-line"><OsLogo v-if="os" :os="os" />{{ systemLabel }}</dd></div>
        <div><dt>Adresse MAC</dt><dd class="code">{{ macAddress }}</dd></div>
        <div v-if="serialNumber"><dt>N° de série</dt><dd class="code">{{ serialNumber }}</dd></div>
        <div v-if="installed"><dt>Image</dt><dd>{{ installed.image }}</dd></div>
      </dl>
      <section v-if="installed" class="panel-section">
        <h3 class="eyebrow">Applications installées · <span class="panel-count">{{ installed.apps.length }}</span></h3>
        <ul class="installed-apps">
          <li v-for="app in installed.apps" :key="app">{{ app }}</li>
        </ul>
      </section>
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
        Masteriser…
      </button>
      <span class="field-hint">Simulation : choix du système, de l'image et des applications.</span>
    </template>
  </SidePanel>
</template>
