/**
 * Modèle de la supervision des postes de travail et des processus en temps réel.
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
/**
 * État d'un poste : allumé et joignable, éteint ou injoignable, en cours de masterisation.
 */
type WorkstationStatus = keyof typeof workstationStatusLabels;

/** Poste de travail supervisé. */
interface Workstation {
    id: number;
    roomId: number | null;
    /** Nom d'affichage (ex: « bly-manager (000000001415) »). */
    name: string;
    /** Statut ou libellé (ex: « Poste connecté », « Poste éteint / Agent déconnecté »). */
    traineeLabel: string;
    /** Adresse MAC de la machine (ex: « 00:00:00:00:14:15 »). */
    macAddress: string;
    status: WorkstationStatus;
    /** Vrai si l'agent est actuellement connecté en WebSocket. */
    online?: boolean;
    /** Nom d'hôte de la machine (ex: « bly-manager »). */
    hostname?: string;
}

/** Processus applicatif actif sur un poste supervisé (RG-25). */
interface ProcessInfo {
    pid: number;
    workstationId: number;
    /** Nom du processus (ex: « chrome », « bash », « python ») ou nom affiché. */
    name: string;
    /** Date/heure de démarrage. */
    startedAt: Date;
}

/** Événement du journal de supervision : quel poste, quoi, à quelle heure (entretien 4, 6:30). */
interface SupervisionEvent {
    id: number;
    roomId: number;
    workstationName: string;
    /** Application arrêtée (RG-26), ou poste masterisé (RG-28). */
    kind: 'kill' | 'reimage';
    /** Nom de l'application arrêtée ; null pour une masterisation. */
    processName: string | null;
    at: Date;
}

export {workstationStatusLabels};
export type {Workstation, WorkstationStatus, ProcessInfo, SupervisionEvent};
