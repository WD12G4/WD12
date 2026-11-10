/**
 * Données de la supervision d'une salle : postes (UC réelles), applications ouvertes (simulées,
 * regroupées par poste) et journal : arrêts enregistrés par le backend (KillEvent) et masterisations
 * simulées. Après une masterisation, les postes sont rechargés quand elle doit être finie.
 * Utilisé par : supervision-view.vue
 * RG : RG-25, RG-26, RG-28 ; journal demandé en entretien 4
 */
import {computed, onScopeDispose, ref, type Ref} from 'vue';
import type {ProcessInfo, SupervisionEvent, Workstation} from '../../../models/supervision.model.ts';
import {REIMAGE_DURATION_MS, supervisionServices} from '../../../services/supervision.services.mock.ts';
import {killEventsServices} from '../../../services/kill-events.services.ts';

/** Poste avec ses applications ouvertes, prêt à afficher. */
interface WorkstationView {
    workstation: Workstation;
    processes: ProcessInfo[];
}

/** Vue affichée : les postes en tuiles, ou le journal. */
type SupervisionTab = 'stations' | 'journal';

/** Salle chargée et listes reçues du service. */
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
    return [...kills, ...supervisionServices.findReimageEvents(roomId)].sort((a, b) => b.at.getTime() - a.at.getTime());
}

/** Recharge ce qui change après une action : applications ouvertes et journal. */
async function reloadActivity(state: SupervisionState): Promise<void> {
    const ids = state.workstations.value.map(workstation => workstation.id);
    const [processes, events] = await Promise.all([supervisionServices.findProcesses(ids), loadJournal(state.roomId)]);
    state.processes.value = processes;
    state.events.value = events;
}

/** Charge les postes d'une salle (aucun si roomId vaut null), puis leurs applications et le journal. */
async function loadRoom(state: SupervisionState, roomId: number | null): Promise<void> {
    state.roomId = roomId;
    state.workstations.value = roomId === null ? [] : await supervisionServices.findWorkstations(roomId);
    await reloadActivity(state);
}

/** Associe à chaque poste ses applications. */
function toViews(state: SupervisionState): WorkstationView[] {
    return state.workstations.value.map(workstation => ({
        workstation,
        processes: state.processes.value.filter(process => process.workstationId === workstation.id),
    }));
}

/**
 * Arrête des applications (une seule, ou la même sur plusieurs postes), enregistre chaque arrêt
 * (KillEvent), puis recharge (RG-26). Le journal est rechargé même si un enregistrement échoue.
 */
async function killAll(state: SupervisionState, processes: ProcessInfo[]): Promise<void> {
    try {
        await Promise.all(processes.map(async process => {
            await supervisionServices.killProcess(process);
            await killEventsServices.create(process.name, process.workstationId);
        }));
    } finally {
        await reloadActivity(state);
    }
}

/**
 * Crée l'état de la supervision.
 * @returns workstationViews, events et les actions loadRoom, killProcess, killEverywhere (RG-26),
 * reimage (RG-28), qui lèvent ApiError en cas d'échec
 */
export function useSupervision() {
    const state: SupervisionState = {roomId: null, workstations: ref([]), processes: ref([]), events: ref([])};
    let reimageTimer = 0;
    onScopeDispose(() => window.clearTimeout(reimageTimer));

    /** Masterise un poste, puis recharge les postes quand la masterisation doit être finie. */
    async function reimage(workstation: Workstation): Promise<void> {
        await supervisionServices.reimage(workstation);
        await loadRoom(state, state.roomId);
        window.clearTimeout(reimageTimer);
        // En cas d'échec de ce rechargement, la salle se recharge au prochain choix ou à la prochaine action.
        reimageTimer = window.setTimeout(() => loadRoom(state, state.roomId).catch(() => undefined), REIMAGE_DURATION_MS + 200);
    }

    return {
        workstationViews: computed(() => toViews(state)),
        events: computed(() => state.events.value),
        loadRoom: (roomId: number | null) => loadRoom(state, roomId),
        killProcess: (process: ProcessInfo) => killAll(state, [process]),
        killEverywhere: (name: string) => killAll(state, state.processes.value.filter(process => process.name === name)),
        reimage,
    };
}

export type {WorkstationView, SupervisionTab};
