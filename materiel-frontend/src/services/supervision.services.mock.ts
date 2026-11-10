// MAQUETTE PARTIELLE : à remplacer par supervision.services.ts quand l'agent de supervision sera fusionné
// (branche feature/supervision-agent : /api/v1/supervision).
/**
 * Supervision des postes : les postes sont les vraies UC de la salle (API /materials) ; leurs
 * applications ouvertes, leur état et la masterisation sont simulés en mémoire. Les arrêts sont
 * enregistrés pour de vrai par kill-events.services.ts ; les masterisations, simulées, gardent un
 * journal local (perdu au rechargement de la page).
 * Utilisé par : features/supervision
 * RG : RG-25, RG-28
 */
import type {Material} from '../models/material.model.ts';
import type {ProcessInfo, SupervisionEvent, Workstation} from '../models/supervision.model.ts';
import {materialsServices} from './materials.services.ts';

/** Latence simulée des parties simulées, pour que les états de chargement soient visibles. */
const MOCK_LATENCY_MS = 300;
/** Durée simulée d'une masterisation. */
export const REIMAGE_DURATION_MS = 8000;
/** Applications simulées ; la dernière n'a pas d'icône dédiée (affichée par son initiale). */
const demoApplications = [
    'Navigateur web', 'Environnement de développement', 'Terminal', 'Traitement de texte', 'Tableur', 'Lecteur vidéo',
];
/** Nombre d'applications ouvertes sur chaque poste, selon son rang dans la salle. */
const openCounts = [2, 3, 1, 0, 4, 2, 3, 1];

//------------------ DATAS -----------------------------------
/** Processus par identifiant de poste, créés au premier affichage de la salle. */
const processesByWorkstation = new Map<number, ProcessInfo[]>();
/** Fin de masterisation par identifiant de poste (horodatage en millisecondes). */
const reimagingUntil = new Map<number, number>();
/** Masterisations simulées, la plus récente en premier. */
const reimageEvents: SupervisionEvent[] = [];
let nextReimageId = 1;

//------------------ FUNCTIONS -------------------------------
/** Attend la latence simulée. */
function delay(): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, MOCK_LATENCY_MS));
}

/** Poste correspondant à une UC ; le stagiaire et l'adresse MAC sont simulés. */
function toWorkstation(uc: Material, roomId: number, index: number): Workstation {
    const reimaging = (reimagingUntil.get(uc.id) ?? 0) > Date.now();
    return {
        id: uc.id, roomId,
        name: uc.serialNumber,
        traineeLabel: `Stagiaire ${index}`,
        macAddress: `00:1A:2B:${String(roomId % 100).padStart(2, '0')}:00:${String(uc.id % 100).padStart(2, '0')}`,
        status: reimaging ? 'reimaging' : 'online',
    };
}

/** Crée les applications ouvertes simulées d'un poste. */
function buildProcesses(workstation: Workstation, index: number): ProcessInfo[] {
    return Array.from({length: openCounts[(index - 1) % openCounts.length] ?? 0}, (_, offset) => ({
        pid: workstation.id * 10 + offset,
        workstationId: workstation.id,
        name: demoApplications[(index + offset) % demoApplications.length] ?? 'Application',
        startedAt: new Date(Date.now() - (offset * 17 + index * 3 + 4) * 60_000),
    }));
}

export const supervisionServices = {

    /** Postes d'une salle : ses UC non archivées (réelles), avec leurs applications simulées. */
    findWorkstations: async (roomId: number): Promise<Workstation[]> => {
        const materials = await materialsServices.findAll();
        const ucs = materials.filter(material => material.materialType === 'UC' && !material.archived && material.room?.id === roomId);
        return ucs.map((uc, i) => {
            const workstation = toWorkstation(uc, roomId, i + 1);
            if (!processesByWorkstation.has(uc.id)) processesByWorkstation.set(uc.id, buildProcesses(workstation, i + 1));
            return workstation;
        });
    },

    /** Applications lancées sur les postes donnés (simulées, RG-25). */
    findProcesses: async (workstationIds: number[]): Promise<ProcessInfo[]> => {
        await delay();
        return workstationIds.flatMap(id => processesByWorkstation.get(id) ?? []).map(process => ({...process}));
    },

    /** Ferme une application sur le poste (simulé ; l'arrêt est enregistré par kill-events.services.ts). */
    killProcess: async (process: ProcessInfo): Promise<void> => {
        await delay();
        const list = processesByWorkstation.get(process.workstationId) ?? [];
        processesByWorkstation.set(process.workstationId, list.filter(item => item.pid !== process.pid));
    },

    /** Masterisations simulées d'une salle, la plus récente en premier. */
    findReimageEvents: (roomId: number): SupervisionEvent[] => reimageEvents.filter(event => event.roomId === roomId),

    /** Lance la masterisation simulée d'un poste (RG-28) : ses applications se ferment un moment. */
    reimage: async (workstation: Workstation): Promise<void> => {
        await delay();
        processesByWorkstation.set(workstation.id, []);
        reimagingUntil.set(workstation.id, Date.now() + REIMAGE_DURATION_MS);
        reimageEvents.unshift({
            id: `reimage-${nextReimageId++}`, roomId: workstation.roomId, workstationName: workstation.name,
            kind: 'reimage', processName: null, at: new Date(),
        });
    },
};
