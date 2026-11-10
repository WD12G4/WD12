/**
 * Modèle de la supervision des postes d'une salle (API /api/v1/supervision et agent de supervision).
 * Un poste est une UC de la salle ; il est en ligne quand son agent est connecté au backend.
 * RG : RG-25 (processus lancés), RG-26 (arrêt), RG-28 (masterisation), RG-29 (identification), RG-30
 */

//----------------- VALEURS ------------------------------
/** Libellés des états d'un poste. */
const workstationStatusLabels = {
    online: 'En ligne',
    offline: 'Hors ligne',
    reimaging: 'Masterisation…',
} as const;

//----------------- TYPES --------------------------------
/** État d'un poste : agent connecté, agent déconnecté (poste éteint), masterisation en cours (simulée). */
type WorkstationStatus = keyof typeof workstationStatusLabels;

/**
 * Poste de travail d'une salle.
 * TODO(question): l'agent n'identifie pas le stagiaire installé au poste (RG-30). À proposer au client.
 */
interface Workstation {
    /** Identifiant de l'UC (matériel), comme dans KillEvent. */
    id: number;
    roomId: number | null;
    /** Nom donné par le backend : « nom d'hôte (n° de série) », ou « Poste N (n° de série) » sans agent. */
    name: string;
    /** Nom d'hôte remonté par l'agent, ou null s'il n'est pas connecté. */
    hostname: string | null;
    /** Adresse MAC remontée par l'agent (RG-29), ou null si elle est inconnue. */
    macAddress: string | null;
    status: WorkstationStatus;
}

/** Processus lancé par l'utilisateur du poste ; l'agent écarte les processus système (RG-25). */
interface ProcessInfo {
    pid: number;
    workstationId: number;
    /** Nom du processus remonté par l'agent (ex. « chrome.exe », « code »). */
    name: string;
    startedAt: Date;
}

/** Événement du journal de supervision : quel poste, quoi, à quelle heure (entretien 4, 6:30). */
interface SupervisionEvent {
    /** Clé unique (« kill-12 » pour un KillEvent, « reimage-3 » pour une masterisation simulée). */
    id: string;
    /** Salle de l'UC, ou null si elle est en stock. */
    roomId: number | null;
    workstationName: string;
    /** Application arrêtée (RG-26), ou poste masterisé (RG-28). */
    kind: 'kill' | 'reimage';
    /** Nom de l'application visée ; null pour une masterisation. */
    processName: string | null;
    /** Faux si l'agent n'a pas pu arrêter l'application (processus protégé, déjà fermé…). */
    succeeded: boolean;
    at: Date;
}

export {workstationStatusLabels};
export type {Workstation, WorkstationStatus, ProcessInfo, SupervisionEvent};
