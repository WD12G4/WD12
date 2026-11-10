/**
 * Données de la supervision d'une salle : postes (UC, en ligne si leur agent est connecté),
 * processus remontés par les agents, et journal (arrêts enregistrés par le backend, masterisations
 * simulées). La salle est relue toutes les 10 secondes : les processus changent sur les postes.
 * Utilisé par : supervision-view.vue
 * RG : RG-25, RG-26, RG-28 ; journal demandé en entretien 4
 */
import {computed, onScopeDispose, ref, type Ref} from 'vue';
import type {ProcessInfo, SupervisionEvent, Workstation} from '../../../models/supervision.model.ts';
import {supervisionServices} from '../../../services/supervision.services.ts';
import {killEventsServices} from '../../../services/kill-events.services.ts';
import {reimageServices} from '../../../services/reimage.services.mock.ts';

/** Intervalle de relecture de la salle. */
const REFRESH_MS = 10_000;

/** Poste avec ses processus, prêt à afficher. */
interface WorkstationView {
    workstation: Workstation;
    processes: ProcessInfo[];
}

/** Vue affichée : les postes en tuiles, ou le journal. */
type SupervisionTab = 'stations' | 'journal';

/** Salle chargée et listes reçues de l'API. */
interface SupervisionState {
    roomId: number | null;
    workstations: Ref<Workstation[]>;
    processes: Ref<ProcessInfo[]>;
    events: Ref<SupervisionEvent[]>;
}

//------------------ FUNCTIONS -------------------------------
/** Journal d'une salle : arrêts (backend) et masterisations (simulées), du plus récent au plus ancien. */
async function loadJournal(roomId: number | null): Promise<SupervisionEvent[]> {
    if (roomId === null) return [];
    const kills = await killEventsServices.findByRoom(roomId);
    return [...kills, ...reimageServices.findEvents(roomId)].sort((a, b) => b.at.getTime() - a.at.getTime());
}

/** Charge (ou relit) les postes de la salle, leurs processus et le journal. */
async function loadRoom(state: SupervisionState, roomId: number | null): Promise<void> {
    state.roomId = roomId;
    const workstations = roomId === null ? [] : await supervisionServices.findWorkstations(roomId);
    const [processes, events] = await Promise.all([
        supervisionServices.findProcesses(workstations.filter(item => item.status === 'online').map(item => item.id)),
        loadJournal(roomId),
    ]);
    state.workstations.value = workstations;
    state.processes.value = processes;
    state.events.value = events;
}

/** Associe à chaque poste ses processus ; un poste en masterisation (simulée) n'en montre aucun. */
function toViews(state: SupervisionState): WorkstationView[] {
    return state.workstations.value.map(item => {
        const reimaging = reimageServices.isReimaging(item.id);
        const workstation: Workstation = reimaging ? {...item, status: 'reimaging'} : item;
        const processes = reimaging ? [] : state.processes.value.filter(process => process.workstationId === item.id);
        return {workstation, processes};
    });
}

/**
 * Arrête des processus (un seul, ou le même sur plusieurs postes), puis relit la salle (RG-26).
 * @throws la première erreur (ex. processus protégé refusé par l'agent), après la relecture
 */
async function killAll(state: SupervisionState, processes: ProcessInfo[]): Promise<void> {
    const results = await Promise.allSettled(processes.map(supervisionServices.killProcess));
    await loadRoom(state, state.roomId);
    const failure = results.find(result => result.status === 'rejected');
    if (failure) throw failure.reason;
}

/**
 * Crée l'état de la supervision, relu toutes les 10 secondes tant que la page est visible.
 * @returns workstationViews, events et les actions loadRoom, killProcess, killEverywhere (RG-26),
 * reimage (RG-28), qui lèvent ApiError en cas d'échec
 */
export function useSupervision() {
    const state: SupervisionState = {roomId: null, workstations: ref([]), processes: ref([]), events: ref([])};
    const views = computed(() => toViews(state));
    // Relecture silencieuse : une erreur passagère est ignorée, la suivante réessaiera.
    const refresh = () => (document.hidden ? undefined : loadRoom(state, state.roomId).catch(() => undefined));
    const timer = window.setInterval(refresh, REFRESH_MS);
    onScopeDispose(() => window.clearInterval(timer));
    return {
        workstationViews: views,
        events: computed(() => state.events.value),
        loadRoom: (roomId: number | null) => loadRoom(state, roomId),
        killProcess: (process: ProcessInfo) => killAll(state, [process]),
        killEverywhere: (name: string) => killAll(state, state.processes.value.filter(process => process.name === name)),
        reimage: async (workstation: Workstation) => {
            await reimageServices.reimage(workstation);
            await loadRoom(state, state.roomId);
        },
    };
}

export type {WorkstationView, SupervisionTab};
