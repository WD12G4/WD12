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

/** Noms des systèmes remontés par l'agent. */
const workstationOsLabels = {
    windows: 'Windows',
    linux: 'Linux',
    macos: 'macOS',
} as const;

//----------------- TYPES --------------------------------
/** État d'un poste : agent connecté, agent déconnecté (poste éteint), masterisation en cours (simulée). */
type WorkstationStatus = keyof typeof workstationStatusLabels;

/** Système d'un poste, remonté par l'agent : décide du logo affiché (os-logo.vue). */
type WorkstationOs = keyof typeof workstationOsLabels;

/** Avancement d'une masterisation simulée : étape en cours et pourcentage global. */
interface ReimageProgress {
    /** Étape en clair (« Installation des applications (3/8) »). */
    phase: string;
    /** De 0 à 100. */
    percent: number;
}

/**
 * Poste de travail d'une salle.
 * Le stagiaire installé au poste est l'utilisateur de la session ouverte, remonté par l'agent (RG-30) :
 * pas de compte stagiaire à créer ni à attribuer, chacun est reconnu sur la machine où il se connecte.
 */
interface Workstation {
    /** Identifiant de l'UC (matériel), comme dans KillEvent. */
    id: number;
    roomId: number | null;
    /** Nom affiché : nom d'hôte, ou « Poste N » sans agent (le n° de série est à part). */
    name: string;
    /** N° de série de l'UC, ou null (poste simulé). */
    serialNumber: string | null;
    /** Système remonté par l'agent, ou null s'il est inconnu (agent non connecté). */
    os: WorkstationOs | null;
    /** Nom d'hôte remonté par l'agent, ou null s'il n'est pas connecté. */
    hostname: string | null;
    /** Identifiant de la session ouverte sur le poste (le stagiaire), ou null : aucune session ou agent déconnecté. */
    sessionUser: string | null;
    /** Adresse MAC remontée par l'agent (RG-29), ou null si elle est inconnue. */
    macAddress: string | null;
    status: WorkstationStatus;
    /** Vrai pour un poste simulé (démonstration, simulation.services.mock.ts), faux pour un vrai poste. */
    simulated: boolean;
    /** Masterisation simulée en cours, ou null. */
    reimage: ReimageProgress | null;
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
    /** Vrai pour une action sur un poste simulé. */
    simulated: boolean;
    /** Précision affichée (masterisation : « Ubuntu 24.04 LTS · Développement · 7 applications »), ou null. */
    detail: string | null;
}

export {workstationStatusLabels, workstationOsLabels};
export type {Workstation, WorkstationStatus, WorkstationOs, ReimageProgress, ProcessInfo, SupervisionEvent};
