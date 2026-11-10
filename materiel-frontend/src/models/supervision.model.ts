/**
 * Modèle de la supervision des postes d'une salle.
 * Un poste est une UC de la salle (matériel réel). Ses applications ouvertes, son état et la
 * masterisation restent simulés en attendant l'agent de supervision (branche feature/supervision-agent) ;
 * les arrêts d'applications sont enregistrés par le backend (KillEvent).
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
 * TODO(question): comment l'agent du poste signale-t-il son état ? À définir avec la démonstration sur VM.
 */
type WorkstationStatus = keyof typeof workstationStatusLabels;

/** Poste de travail d'une salle. */
interface Workstation {
    /** Identifiant de l'UC (matériel), comme dans KillEvent et l'agent de supervision. */
    id: number;
    roomId: number;
    /** Numéro de série de l'UC (ex. « UC-DELL-001 »). */
    name: string;
    /**
     * Libellé du stagiaire installé au poste (rôle « stagiaire » sans connexion, RG-30).
     * TODO(question): nom saisi, numéro, ou utilisateur de session récupéré ? À proposer au client.
     */
    traineeLabel: string;
    /**
     * Adresse MAC, identifiant du poste (RG-29).
     * TODO(question): MAC ou IP fixe ? Le client indique que les IP des salles sont fixes.
     */
    macAddress: string;
    status: WorkstationStatus;
}

/** Processus lancé par l'utilisateur du poste (les processus système ne sont pas listés, RG-25). */
interface ProcessInfo {
    pid: number;
    workstationId: number;
    /** Nom affiché (ex. « Navigateur web », « Environnement de développement »). */
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
    /** Nom de l'application arrêtée ; null pour une masterisation. */
    processName: string | null;
    at: Date;
}

export {workstationStatusLabels};
export type {Workstation, WorkstationStatus, ProcessInfo, SupervisionEvent};
