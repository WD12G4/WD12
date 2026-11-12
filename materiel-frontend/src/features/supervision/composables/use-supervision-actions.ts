/**
 * Actions de la supervision : fermer une application (un clic, RG-26), la fermer partout (après
 * confirmation), et masteriser des postes simulés avec l'assistant (RG-28). Pendant une action, les
 * boutons sont désactivés (busy) ; une erreur s'affiche au-dessus des tuiles (actionError).
 * Utilisé par : supervision-view.vue
 * RG : RG-26, RG-28
 */
import {computed, ref, type ComputedRef} from 'vue';
import type {ProcessInfo} from '../../../models/supervision.model.ts';
import type {ReimageConfig} from '../../../models/reimage.model.ts';
import {useAsyncState} from '../../../shared/composables/use-async-state.ts';
import type {useSupervision, WorkstationView} from './use-supervision.ts';

/** Supervision de la salle (use-supervision.ts). */
type Supervision = ReturnType<typeof useSupervision>;

/**
 * Postes cochés à l'ouverture de l'assistant : le poste choisi, ou tous les postes simulés libres.
 * @param simulated postes simulés de la salle
 * @param view poste depuis lequel l'assistant est ouvert, absent depuis le résumé de la salle
 */
function preselection(simulated: WorkstationView[], view?: WorkstationView): number[] {
    if (view) return [view.workstation.id];
    return simulated.filter(item => item.workstation.status !== 'reimaging').map(item => item.workstation.id);
}

/**
 * Crée les actions de la supervision.
 * @param supervision état de la salle (use-supervision.ts)
 * @returns busy, actionError, l'assistant (wizardPreselect, simulatedViews) et les actions
 */
export function useSupervisionActions(supervision: Supervision) {
    const {loading: busy, errorMessage: actionError, run: act} = useAsyncState();
    /** Postes cochés à l'ouverture de l'assistant de masterisation ; null quand il est fermé. */
    const wizardPreselect = ref<number[] | null>(null);
    const simulatedViews: ComputedRef<WorkstationView[]> = computed(() =>
        supervision.workstationViews.value.filter(view => view.workstation.simulated));
    return {
        busy, actionError, wizardPreselect, simulatedViews,
        kill: (process: ProcessInfo) => act(() => supervision.killProcess(process)),
        closeEverywhere: async (name: string) => {
            if (!window.confirm(`Fermer « ${name} » sur tous les postes de la salle ?`)) return;
            await act(() => supervision.killEverywhere(name));
        },
        openWizard: (view?: WorkstationView) => void (wizardPreselect.value = preselection(simulatedViews.value, view)),
        launchReimage: async (workstationIds: number[], config: ReimageConfig) => {
            wizardPreselect.value = null;
            await act(() => supervision.reimage(workstationIds, config));
        },
    };
}
