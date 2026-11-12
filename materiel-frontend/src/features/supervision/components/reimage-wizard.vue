<script setup lang="ts">
/**
 * Assistant de masterisation des postes simulés, en 4 étapes : système, image de base,
 * applications à installer, puis postes visés et lancement. Fenêtre au-dessus de la page ;
 * Échap, × ou un clic sur le fond la ferment.
 * Utilisé par : supervision-view.vue (administrateur)
 * RG : RG-28
 */
import {onBeforeUnmount, onMounted, useTemplateRef} from 'vue';
import type {ReimageConfig} from '../../../models/reimage.model.ts';
import type {WorkstationView} from '../composables/use-supervision.ts';
import {useReimageWizard} from '../composables/use-reimage-wizard.ts';
import ChoiceCards from './choice-cards.vue';
import ReimageAppsStep from './reimage-apps-step.vue';
import ReimageTargetsStep from './reimage-targets-step.vue';
import WizardFooter from './wizard-footer.vue';
import WizardHeader from './wizard-header.vue';

//------------------ PROPS / EMITS ---------------------------
const props = defineProps<{
  /** Postes simulés de la salle. */
  stations: WorkstationView[];
  /** Postes cochés à l'ouverture. */
  preselected: number[];
}>();

const emit = defineEmits<{
  /** Fermeture sans lancer. */
  close: [];
  /** Masterisation demandée sur ces postes. */
  launch: [workstationIds: number[], config: ReimageConfig];
}>();

//------------------ DATAS -----------------------------------
const wizard = useReimageWizard(props.preselected);
const {steps, step, osId, profileId, appIds, targetIds, osChoices, profileChoices, availableApps, summary, estimate, isLast, canLaunch} = wizard;
const dialog = useTemplateRef<HTMLElement>('dialog');

//------------------ FUNCTIONS -------------------------------
const onKeydown = (event: KeyboardEvent): void => void (event.key === 'Escape' && emit('close'));
const launch = (): void => emit('launch', targetIds.value, wizard.config.value);

//------------------ LIFECYCLE -------------------------------
onMounted(() => {
  document.addEventListener('keydown', onKeydown);
  dialog.value?.focus();
});
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown));
</script>

<template>
  <Teleport to="body">
    <div class="wizard-backdrop" @click.self="emit('close')">
      <section
          ref="dialog"
          class="wizard card"
          role="dialog"
          aria-modal="true"
          aria-labelledby="reimage-wizard-title"
          tabindex="-1"
      >
        <WizardHeader
            :steps="steps"
            :step="step"
            title-id="reimage-wizard-title"
            @close="emit('close')"
        />
        <ChoiceCards
            v-if="step === 0"
            label="Système"
            :options="osChoices"
            :selected="osId"
            @choose="wizard.chooseOs"
        />
        <ChoiceCards
            v-else-if="step === 1"
            label="Image de base"
            :options="profileChoices"
            :selected="profileId"
            @choose="wizard.chooseProfile"
        />
        <ReimageAppsStep
            v-else-if="step === 2"
            :apps="availableApps"
            :selected="appIds"
            :os-id="osId"
            @toggle="wizard.toggleApp"
        />
        <ReimageTargetsStep
            v-else
            :stations="stations"
            :selected="targetIds"
            :summary="summary"
            :estimate="estimate"
            @toggle="wizard.toggleTarget"
        />
        <WizardFooter
            :can-go-back="step > 0"
            :is-last="isLast"
            :can-launch="canLaunch"
            @back="wizard.back"
            @next="wizard.next"
            @launch="launch"
        />
      </section>
    </div>
  </Teleport>
</template>
