// MAQUETTE : aucun endpoint backend. À remplacer par supervision.services.ts quand l'API existera.
/**
 * Service simulé de la supervision : postes d'une salle, processus lancés, arrêt et masterisation.
 * Les postes et processus sont générés en mémoire, de façon identique à chaque ouverture d'une salle.
 * Utilisé par : features/supervision
 * RG : RG-25, RG-26, RG-28
 */
import type {ProcessInfo, Workstation} from '../models/supervision.model.ts';

/** Latence simulée, pour que les états de chargement soient visibles. */
const MOCK_LATENCY_MS = 300;
/** Nombre de postes générés par salle. */
const WORKSTATIONS_PER_ROOM = 4;
/** Applications « métier » simulées (logiciels de base installés sur un poste). */
const demoApplications = ['Navigateur web', 'Environnement de développement', 'Terminal', 'Traitement de texte'];

//------------------ DATAS -----------------------------------
/** Processus par identifiant de poste, créés au premier affichage de la salle. */
const processesByWorkstation = new Map<number, ProcessInfo[]>();

//------------------ FUNCTIONS -------------------------------
/** Attend la latence simulée. */
function delay(): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, MOCK_LATENCY_MS));
}

/** Identifiant de poste unique, déduit de la salle et du numéro du poste. */
function workstationId(roomId: number, index: number): number {
    return roomId * 100 + index;
}

/** Crée un poste de démonstration. */
function buildWorkstation(roomId: number, index: number): Workstation {
    const suffix = String(index).padStart(2, '0');
    return {
        id: workstationId(roomId, index),
        roomId,
        name: `Poste ${index}`,
        traineeLabel: `Stagiaire ${index}`,
        macAddress: `00:1A:2B:${String(roomId % 100).padStart(2, '0')}:00:${suffix}`,
    };
}

/** Crée les processus d'un poste : deux applications, choisies selon le numéro du poste. */
function buildProcesses(id: number): ProcessInfo[] {
    return [0, 1].map(offset => ({
        pid: id * 10 + offset,
        workstationId: id,
        name: demoApplications[(id + offset) % demoApplications.length],
        startedAt: new Date(Date.now() - (offset + 1) * 20 * 60_000),
    }));
}

export const supervisionServices = {

    /** Postes d'une salle, avec la liste initiale de leurs processus. */
    findWorkstations: async (roomId: number): Promise<Workstation[]> => {
        await delay();
        const workstations = Array.from({length: WORKSTATIONS_PER_ROOM}, (_, i) => buildWorkstation(roomId, i + 1));
        for (const workstation of workstations) {
            if (!processesByWorkstation.has(workstation.id)) {
                processesByWorkstation.set(workstation.id, buildProcesses(workstation.id));
            }
        }
        return workstations;
    },

    /** Processus lancés sur les postes donnés (RG-25). */
    findProcesses: async (workstationIds: number[]): Promise<ProcessInfo[]> => {
        await delay();
        return workstationIds.flatMap(id => processesByWorkstation.get(id) ?? []).map(process => ({...process}));
    },

    /** Arrête un processus (RG-26). */
    killProcess: async (process: ProcessInfo): Promise<void> => {
        await delay();
        const list = processesByWorkstation.get(process.workstationId) ?? [];
        processesByWorkstation.set(process.workstationId, list.filter(item => item.pid !== process.pid));
    },

    /** Lance la masterisation d'un poste : en démonstration, ses processus sont simplement fermés (RG-28). */
    reimage: async (workstation: Workstation): Promise<void> => {
        await delay();
        processesByWorkstation.set(workstation.id, []);
    },
};
