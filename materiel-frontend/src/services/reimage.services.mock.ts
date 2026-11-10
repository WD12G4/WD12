// MAQUETTE : la masterisation n'a pas encore de commande côté serveur (PXE/WDS à définir).
/**
 * Masterisation simulée d'un poste (RG-28) : le poste apparaît « en masterisation » un moment, et
 * l'action est inscrite dans un journal local (perdu au rechargement de la page).
 * Utilisé par : features/supervision
 * RG : RG-28
 */
import type {SupervisionEvent, Workstation} from '../models/supervision.model.ts';

/** Durée simulée d'une masterisation. */
const REIMAGE_DURATION_MS = 8000;
/** Latence simulée, pour que l'état d'envoi soit visible. */
const MOCK_LATENCY_MS = 300;

//------------------ DATAS -----------------------------------
/** Fin de masterisation par identifiant de poste (horodatage en millisecondes). */
const reimagingUntil = new Map<number, number>();
/** Masterisations simulées, la plus récente en premier. */
const reimageEvents: SupervisionEvent[] = [];
let nextId = 1;

export const reimageServices = {

    /** Vrai si le poste est en cours de masterisation (simulée). */
    isReimaging: (workstationId: number): boolean => (reimagingUntil.get(workstationId) ?? 0) > Date.now(),

    /** Masterisations simulées d'une salle, la plus récente en premier. */
    findEvents: (roomId: number): SupervisionEvent[] => reimageEvents.filter(event => event.roomId === roomId),

    /** Lance la masterisation simulée d'un poste. */
    reimage: async (workstation: Workstation): Promise<void> => {
        await new Promise(resolve => setTimeout(resolve, MOCK_LATENCY_MS));
        reimagingUntil.set(workstation.id, Date.now() + REIMAGE_DURATION_MS);
        reimageEvents.unshift({
            id: `reimage-${nextId++}`, roomId: workstation.roomId, workstationName: workstation.name,
            kind: 'reimage', processName: null, succeeded: true, at: new Date(),
        });
    },
};
