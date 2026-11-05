<script setup lang="ts">
/**
 * Résultat d'une lecture, en panneau bas d'écran : verdict, informations connues, action proposée.
 * - déplacé / en stock → « Relocaliser ici » ; inconnu → formulaire de prise en compte ;
 * - un formateur (USER) consulte seulement : les actions sont réservées à MANAGER et ADMIN (RG-17).
 * Utilisé par : features/scan/views/scan-view.vue
 */
import {computed} from 'vue';
import type {MaterialInput} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {type ScanOutcome, verdictLabels} from '../../../models/scan.model.ts';
import {locationLabel, roomLabel} from '../../../shared/composables/use-room-label.ts';
import FormError from '../../../shared/components/form-error.vue';
import MaterialFacts from './material-facts.vue';
import MaterialCreateForm from './material-create-form.vue';
import ReadingDetails from './reading-details.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  outcome: ScanOutcome;
  currentRoom: Room | null;
  /** Vrai pour MANAGER et ADMIN : relocalisation et prise en compte autorisées. */
  canEdit: boolean;
  busy: boolean;
  errorMessage: string | null;
}>();

const emit = defineEmits<{
  relocate: [];
  register: [input: MaterialInput];
  pickRoom: [];
  close: [];
}>();

//------------------ VALEURS ---------------------------------
/** Couleur du badge selon le verdict. */
const badgeKinds = {conform: 'ok', moved: 'warn', stock: 'warn', archived: 'danger', known: 'muted', unknown: 'danger'};

//------------------ COMPUTED --------------------------------
const verdict = computed(() => props.outcome.verdict);
const serialNumber = computed(() => props.outcome.material?.serialNumber ?? props.outcome.reading.serialNumber);
const canRelocate = computed(() => props.canEdit && props.currentRoom !== null
    && (verdict.value === 'moved' || verdict.value === 'stock'));

/** Explication du verdict. */
const message = computed(() => {
  const declared = locationLabel(props.outcome.material?.room ?? null);
  switch (verdict.value) {
    case 'conform': return 'Présent dans la salle déclarée.';
    case 'moved': return `Déclaré ailleurs : ${declared}.`;
    case 'stock': return 'Déclaré en stock, trouvé ici.';
    case 'archived': return 'Archivé : il ne peut plus être affecté à une salle. Signalez-le au gestionnaire.';
    case 'known': return 'Choisissez votre salle pour contrôler son emplacement.';
    default: return props.canEdit ? 'Absent du parc : prenez-le en compte.'
        : 'Absent du parc. Seul un gestionnaire peut le prendre en compte : notez le numéro.';
  }
});
</script>

<template>
  <aside class="sheet" :class="`verdict-${verdict}`" role="dialog" aria-live="assertive" aria-label="Résultat du scan">
    <header class="sheet-header">
      <span class="badge" :class="badgeKinds[verdict]">{{ verdictLabels[verdict] }}</span>
      <button type="button" class="icon-button" aria-label="Fermer" @click="emit('close')">×</button>
    </header>
    <p class="serial mono">{{ serialNumber }}</p>
    <p class="sheet-message">{{ message }}</p>

    <MaterialFacts v-if="outcome.material" :material="outcome.material" />

    <MaterialCreateForm
        v-if="verdict === 'unknown' && canEdit"
        :reading="outcome.reading"
        :room="currentRoom"
        :submitting="busy"
        :error-message="errorMessage"
        @submit="emit('register', $event)"
    />

    <div class="sheet-actions">
      <template v-if="canRelocate && currentRoom">
        <FormError :message="errorMessage" />
        <button type="button" class="primary" :disabled="busy" @click="emit('relocate')">
          {{ busy ? 'Enregistrement…' : `Relocaliser en ${roomLabel(currentRoom)}` }}
        </button>
      </template>
      <button v-if="verdict === 'known'" type="button" class="primary" @click="emit('pickRoom')">Choisir ma salle</button>
      <button type="button" class="secondary" @click="emit('close')">{{ verdict === 'conform' ? 'Suivant' : 'Ignorer' }}</button>
    </div>

    <ReadingDetails :reading="outcome.reading" />
  </aside>
</template>
