/**
 * Données de la supervision d'une salle : vrais postes (UC, en ligne si leur agent est connecté) et
 * postes simulés (simulation.services.mock.ts), processus, et journal (arrêts enregistrés par le
 * backend, actions sur les postes simulés). La salle est relue toutes les 10 secondes ; pendant une
 * masterisation simulée, l'écran se met à jour chaque seconde (avancement dans les tuiles).
 * Utilisé par : supervision-view.vue
 * RG : RG-25, RG-26, RG-28 ; journal demandé en entretien 4
 */
import {computed, onScopeDispose, ref, type Ref} from 'vue';
import type {ProcessInfo, SupervisionEvent, Workstation} from '../../../models/supervision.model.ts';
import {supervisionServices} from '../../../services/supervision.services.ts';
import {killEventsServices} from '../../../services/kill-events.services.ts';
import {simulationServices} from '../../../services/simulation.services.mock.ts';
import type {InstalledConfig, ReimageConfig} from '../../../models/reimage.model.ts';

/** Intervalle de relecture de la salle, et de mise à jour d'une masterisation simulée. */
const REFRESH_MS = 10_000;
const TICK_MS = 1000;

/** Poste avec ses processus, prêt à afficher. */
interface WorkstationView {
    workstation: Workstation;
    processes: ProcessInfo[];
    /** Système, image et applications installées d'un poste simulé ; null pour un vrai poste. */
    installed: InstalledConfig | null;
}

/** Vue affichée : les postes en tuiles, le journal, ou la liste noire (administrateur). */
type SupervisionTab = 'stations' | 'journal' | 'blacklist';

/** Salle chargée et listes reçues de l'API. */
interface SupervisionState {
    roomId: number | null;
    workstations: Ref<Workstation[]>;
    processes: Ref<ProcessInfo[]>;
    events: Ref<SupervisionEvent[]>;
    /** Incrémenté pour relire les postes simulés (masterisation en cours, application fermée). */
    tick: Ref<number>;
}

//------------------ FUNCTIONS -------------------------------
/** Charge (ou relit) les vrais postes de la salle, leurs processus et les arrêts enregistrés. */
async function loadRoom(state: SupervisionState, roomId: number | null): Promise<void> {
    state.roomId = roomId;
    state.tick.value++; // postes simulés de la nouvelle salle, même si la lecture des vrais postes échoue
    const workstations = roomId === null ? [] : await supervisionServices.findWorkstations(roomId);
    const [processes, events] = await Promise.all([
        supervisionServices.findProcesses(workstations.filter(item => item.status === 'online').map(item => item.id)),
        roomId === null ? [] : killEventsServices.findByRoom(roomId),
    ]);
    state.workstations.value = workstations;
    state.processes.value = processes;
    state.events.value = events;
}

/** Vrais postes puis postes simulés, chacun avec ses processus (aucun pendant une masterisation). */
function toViews(state: SupervisionState): WorkstationView[] {
    void state.tick.value;
    const simulated = state.roomId === null ? [] : simulationServices.findWorkstations(state.roomId);
    const simProcesses = state.roomId === null ? [] : simulationServices.findProcesses(state.roomId);
    const processes = [...state.processes.value, ...simProcesses];
    return [...state.workstations.value, ...simulated].map(workstation => ({
        workstation, processes: processes.filter(process => process.workstationId === workstation.id),
        installed: workstation.simulated ? simulationServices.describe(workstation.id) : null,
    }));
}

/** Journal : arrêts enregistrés et actions sur les postes simulés, du plus récent au plus ancien. */
function toJournal(state: SupervisionState): SupervisionEvent[] {
    void state.tick.value;
    const simulated = state.roomId === null ? [] : simulationServices.findEvents(state.roomId);
    return [...state.events.value, ...simulated].sort((a, b) => b.at.getTime() - a.at.getTime());
}

/**
 * Arrête des processus (un seul, ou le même sur plusieurs postes), puis relit la salle (RG-26).
 * @throws la première erreur (ex. processus protégé refusé par l'agent), après la relecture
 */
async function killAll(state: SupervisionState, processes: ProcessInfo[]): Promise<void> {
    const results = await Promise.allSettled(processes.map(process => (process.workstationId < 0
        ? simulationServices.kill(process) : supervisionServices.killProcess(process))));
    await loadRoom(state, state.roomId);
    state.tick.value++;
    const failure = results.find(result => result.status === 'rejected');
    if (failure) throw failure.reason;
}

/**
 * Crée l'état de la supervision, relu toutes les 10 secondes tant que la page est visible.
 * @returns workstationViews, events et les actions loadRoom, reload, killProcess, killEverywhere (RG-26),
 * reimage (RG-28, postes simulés), qui lèvent ApiError en cas d'échec
 */
export function useSupervision() {
    const state: SupervisionState = {roomId: null, workstations: ref([]), processes: ref([]), events: ref([]), tick: ref(0)};
    const views = computed(() => toViews(state));
    // Relecture silencieuse : une erreur passagère est ignorée, la suivante réessaiera.
    const refresh = () => (document.hidden ? undefined : loadRoom(state, state.roomId).catch(() => undefined));
    const timers = [window.setInterval(refresh, REFRESH_MS),
        window.setInterval(() => simulationServices.hasActiveReimage() && state.tick.value++, TICK_MS)];
    onScopeDispose(() => timers.forEach(timer => window.clearInterval(timer)));
    return {
        workstationViews: views,
        events: computed(() => toJournal(state)),
        loadRoom: (roomId: number | null) => loadRoom(state, roomId),
        reload: () => loadRoom(state, state.roomId),
        killProcess: (process: ProcessInfo) => killAll(state, [process]),
        killEverywhere: (name: string) => killAll(state, views.value.flatMap(view => view.processes).filter(process => process.name === name)),
        reimage: async (workstationIds: number[], config: ReimageConfig) => {
            await simulationServices.reimage(workstationIds, config);
            state.tick.value++;
        },
    };
}

export type {WorkstationView, SupervisionTab};
