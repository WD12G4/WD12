/**
 * Modèle de la supervision des postes d'une salle.
 * MAQUETTE : aucune API backend ; la démonstration prévue par le client se fera sur une VM
 * (entretien 2, ~3:06).
 * RG : RG-25 (processus lancés), RG-26 (arrêt), RG-28 (masterisation), RG-29 (identification), RG-30
 */

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
}

/** Processus lancé par l'utilisateur du poste (les processus système ne sont pas listés, RG-25). */
interface ProcessInfo {
    pid: number;
    workstationId: number;
    /** Nom affiché (ex. « Navigateur web », « IDE »). */
    name: string;
    startedAt: Date;
}

export type {Workstation, ProcessInfo};
