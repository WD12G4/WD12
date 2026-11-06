/**
 * Historique des lectures de la session (les plus récentes d'abord), avec l'action menée sur chacune.
 * Gardé en mémoire au niveau du module : il survit aux changements de page, pas au rechargement.
 * Utilisé par : features/scan/views/scan-view.vue
 */
import {ref} from 'vue';
import type {ScanOutcome, ScanVerdict} from '../../../models/scan.model.ts';

/** Ligne de l'historique. */
interface HistoryEntry {
    id: number;
    serialNumber: string;
    verdict: ScanVerdict;
    /** Action menée (« Relocalisé », « Pris en compte (en attente) »…), ou null. */
    note: string | null;
    at: Date;
}

/** Nombre de lignes gardées. */
const MAX_ENTRIES = 30;

//------------------ DATAS -----------------------------------
const entries = ref<HistoryEntry[]>([]);
let nextId = 1;

//------------------ FUNCTIONS -------------------------------
/** Ajoute une lecture en tête de l'historique et renvoie son identifiant. */
function record(outcome: ScanOutcome): number {
    const entry: HistoryEntry = {
        id: nextId++,
        serialNumber: outcome.material?.serialNumber ?? outcome.reading.serialNumber,
        verdict: outcome.verdict,
        note: null,
        at: outcome.at,
    };
    entries.value = [entry, ...entries.value].slice(0, MAX_ENTRIES);
    return entry.id;
}

/** Complète une ligne avec l'action menée (et le numéro définitif, s'il a été corrigé). */
function annotate(id: number, note: string, serialNumber?: string): void {
    entries.value = entries.value.map((entry) =>
        entry.id === id ? {...entry, note, serialNumber: serialNumber ?? entry.serialNumber} : entry);
}

/**
 * Accès à l'historique partagé.
 * @returns entries, record() et annotate()
 */
export function useScanHistory() {
    return {entries, record, annotate};
}

export type {HistoryEntry};
