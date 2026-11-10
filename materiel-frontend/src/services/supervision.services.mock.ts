// MAQUETTE : aucun endpoint backend. À remplacer par supervision.services.ts quand l'API existera.
/**
 * Service simulé de la supervision : postes d'une salle, applications lancées, arrêt, masterisation
 * et journal des actions. Tout est généré en mémoire, de façon identique à chaque ouverture d'une salle.
 * Utilisé par : features/supervision
 * RG : RG-25, RG-26, RG-28 ; journal demandé en entretien 4 (entité KillEvent côté backend, sans route)
 */
import type {ProcessInfo, SupervisionEvent, Workstation} from '../models/supervision.model.ts';

/** Latence simulée, pour que les états de chargement soient visibles. */
const MOCK_LATENCY_MS = 300;
/** Nombre de postes générés par salle. */
const WORKSTATIONS_PER_ROOM = 8;
/** Durée simulée d'une masterisation. */
export const REIMAGE_DURATION_MS = 8000;
/** Applications simulées ; la dernière n'a pas d'icône dédiée (affichée par son initiale). */
const demoApplications = [
    'Navigateur web', 'Environnement de développement', 'Terminal', 'Traitement de texte', 'Tableur', 'Lecteur vidéo',
];
/** Nombre d'applications ouvertes sur chaque poste, selon son numéro. */
const openCounts = [2, 3, 1, 0, 4, 2, 3, 1];

//------------------ DATAS -----------------------------------
/** Processus par identifiant de poste, créés au premier affichage de la salle. */
const processesByWorkstation = new Map<number, ProcessInfo[]>();
/** Fin de masterisation par identifiant de poste (horodatage en millisecondes). */
const reimagingUntil = new Map<number, number>();
/** Journal de toutes les salles, le plus récent en premier. */
const events: SupervisionEvent[] = [];
const seededRooms = new Set<number>();
let nextEventId = 1;

//------------------ FUNCTIONS -------------------------------
/** Attend la latence simulée. */
function delay(): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, MOCK_LATENCY_MS));
}

const minutesAgo = (minutes: number): Date => new Date(Date.now() - minutes * 60_000);
/** Un poste par salle est éteint (le 6, le 7 ou le 8 selon la salle). */
const isOffline = (roomId: number, index: number): boolean => index === 6 + (roomId % 3);

/** Crée un poste de démonstration, avec son état du moment. */
function buildWorkstation(roomId: number, index: number): Workstation {
    const id = roomId * 100 + index;
    const reimaging = (reimagingUntil.get(id) ?? 0) > Date.now();
    return {
        id, roomId,
        name: `Poste ${index}`,
        traineeLabel: `Stagiaire ${index}`,
        macAddress: `00:1A:2B:${String(roomId % 100).padStart(2, '0')}:00:${String(index).padStart(2, '0')}`,
        status: isOffline(roomId, index) ? 'offline' : reimaging ? 'reimaging' : 'online',
    };
}

/** Crée les applications ouvertes d'un poste (aucune s'il est éteint). */
function buildProcesses(workstation: Workstation, index: number): ProcessInfo[] {
    if (workstation.status === 'offline') return [];
    return Array.from({length: openCounts[(index - 1) % openCounts.length] ?? 0}, (_, offset) => ({
        pid: workstation.id * 10 + offset,
        workstationId: workstation.id,
        name: demoApplications[(index + offset) % demoApplications.length] ?? 'Application',
        startedAt: minutesAgo(offset * 17 + index * 3 + 4),
    }));
}

/** Ajoute un événement au journal. */
function log(roomId: number, workstationName: string, processName: string | null, at = new Date()): void {
    events.unshift({id: nextEventId++, roomId, workstationName, kind: processName ? 'kill' : 'reimage', processName, at});
}

/** Quelques événements passés, la première fois qu'une salle est ouverte. */
function seedJournal(roomId: number): void {
    if (seededRooms.has(roomId)) return;
    seededRooms.add(roomId);
    log(roomId, 'Poste 5', null, minutesAgo(26 * 60));
    log(roomId, 'Poste 2', 'Lecteur vidéo', minutesAgo(125));
    log(roomId, 'Poste 7', 'Navigateur web', minutesAgo(48));
    events.sort((a, b) => b.at.getTime() - a.at.getTime());
}

export const supervisionServices = {

    /** Postes d'une salle, avec leur état ; crée leurs applications au premier affichage. */
    findWorkstations: async (roomId: number): Promise<Workstation[]> => {
        await delay();
        seedJournal(roomId);
        return Array.from({length: WORKSTATIONS_PER_ROOM}, (_, i) => {
            const workstation = buildWorkstation(roomId, i + 1);
            if (!processesByWorkstation.has(workstation.id)) {
                processesByWorkstation.set(workstation.id, buildProcesses(workstation, i + 1));
            }
            return workstation;
        });
    },

    /** Applications lancées sur les postes donnés (RG-25). */
    findProcesses: async (workstationIds: number[]): Promise<ProcessInfo[]> => {
        await delay();
        return workstationIds.flatMap(id => processesByWorkstation.get(id) ?? []).map(process => ({...process}));
    },

    /** Journal d'une salle, le plus récent en premier. */
    findEvents: async (roomId: number): Promise<SupervisionEvent[]> => {
        await delay();
        return events.filter(event => event.roomId === roomId).map(event => ({...event}));
    },

    /** Arrête une application (RG-26) et l'inscrit au journal. */
    killProcess: async (process: ProcessInfo, workstation: Workstation): Promise<void> => {
        await delay();
        const list = processesByWorkstation.get(process.workstationId) ?? [];
        processesByWorkstation.set(process.workstationId, list.filter(item => item.pid !== process.pid));
        log(workstation.roomId, workstation.name, process.name);
    },

    /** Lance la masterisation d'un poste (RG-28) : ses applications se ferment, il est indisponible un moment. */
    reimage: async (workstation: Workstation): Promise<void> => {
        await delay();
        processesByWorkstation.set(workstation.id, []);
        reimagingUntil.set(workstation.id, Date.now() + REIMAGE_DURATION_MS);
        log(workstation.roomId, workstation.name, null);
    },
};
