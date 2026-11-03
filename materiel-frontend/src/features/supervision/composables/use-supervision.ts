/**
 * Données de la supervision d'une salle : postes et processus, regroupés par poste.
 * Utilisé par : supervision-view.vue
 * RG : RG-25, RG-26, RG-28
 */
import {computed, ref, type Ref} from 'vue';
import type {ProcessInfo, Workstation} from '../../../models/supervision.model.ts';
import {supervisionServices} from '../../../services/supervision.services.mock.ts';

/** Poste avec ses processus, prêt à afficher. */
interface WorkstationView {
    workstation: Workstation;
    processes: ProcessInfo[];
}

/** Listes chargées depuis le service. */
interface SupervisionState {
    workstations: Ref<Workstation[]>;
    processes: Ref<ProcessInfo[]>;
}

//------------------ FUNCTIONS -------------------------------
/** Recharge les processus des postes affichés. */
async function reloadProcesses(state: SupervisionState): Promise<void> {
    const ids = state.workstations.value.map(workstation => workstation.id);
    state.processes.value = await supervisionServices.findProcesses(ids);
}

/** Charge les postes d'une salle (aucun si roomId vaut null) et leurs processus. */
async function loadRoom(state: SupervisionState, roomId: number | null): Promise<void> {
    state.workstations.value = roomId === null ? [] : await supervisionServices.findWorkstations(roomId);
    await reloadProcesses(state);
}

/** Associe à chaque poste ses processus. */
function toViews(state: SupervisionState): WorkstationView[] {
    return state.workstations.value.map(workstation => ({
        workstation,
        processes: state.processes.value.filter(process => process.workstationId === workstation.id),
    }));
}

/**
 * Crée l'état de la supervision.
 * @returns workstationViews et les actions loadRoom, killProcess (RG-26), reimage (RG-28),
 * qui lèvent ApiError en cas d'échec
 */
export function useSupervision() {
    const state: SupervisionState = {workstations: ref([]), processes: ref([])};
    const workstationViews = computed(() => toViews(state));

    return {
        workstationViews,
        loadRoom: (roomId: number | null) => loadRoom(state, roomId),
        killProcess: async (process: ProcessInfo) => {
            await supervisionServices.killProcess(process);
            await reloadProcesses(state);
        },
        reimage: async (workstation: Workstation) => {
            await supervisionServices.reimage(workstation);
            await reloadProcesses(state);
        },
    };
}

export type {WorkstationView};
