/**
 * Modèle de la supervision des postes d'une salle.
 * MAQUETTE : aucune API backend ; la démonstration prévue par le client se fera sur une VM
 * (entretien 2, ~3:06).
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
    id: number;
    roomId: number;
    /** Ex. « Poste 1 ». */
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
