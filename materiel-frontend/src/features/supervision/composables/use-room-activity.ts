/**
 * Résumé d'une salle supervisée et journal regroupé par jour (fonctions pures).
 * Utilisé par : room-summary.vue, supervision-journal.vue
 * RG : RG-25 ; journal demandé en entretien 4
 */
import type {SupervisionEvent} from '../../../models/supervision.model.ts';
import {dayLabel} from '../../../shared/composables/use-dates.ts';
import type {WorkstationView} from './use-supervision.ts';

/** Nombre maximal d'applications proposées à « Fermer partout ». */
const MAX_SHARED_APPS = 3;

/** Chiffres de la salle. */
interface RoomFigures {
    stations: number;
    online: number;
    openApps: number;
}

/** Application ouverte sur plusieurs postes. */
interface SharedApp {
    name: string;
    stations: number;
}

/** Événements d'un même jour. */
interface JournalDay {
    label: string;
    events: SupervisionEvent[];
}

/**
 * Postes, postes en ligne et applications ouvertes de la salle.
 * @param views postes et leurs applications
 */
export function roomFigures(views: WorkstationView[]): RoomFigures {
    return {
        stations: views.length,
        online: views.filter(view => view.workstation.status === 'online').length,
        openApps: views.reduce((total, view) => total + view.processes.length, 0),
    };
}

/**
 * Applications ouvertes sur au moins deux postes, les plus répandues d'abord.
 * @param views postes et leurs applications
 */
export function sharedApps(views: WorkstationView[]): SharedApp[] {
    const stationsByApp = new Map<string, number>();
    for (const view of views) {
        for (const name of new Set(view.processes.map(process => process.name))) {
            stationsByApp.set(name, (stationsByApp.get(name) ?? 0) + 1);
        }
    }
    return [...stationsByApp]
        .filter(([, stations]) => stations > 1)
        .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
        .slice(0, MAX_SHARED_APPS)
        .map(([name, stations]) => ({name, stations}));
}

/**
 * Regroupe les événements par jour (« Aujourd'hui », « Hier », date), dans l'ordre reçu.
 * @param events événements, du plus récent au plus ancien
 */
export function groupByDay(events: SupervisionEvent[]): JournalDay[] {
    const days: JournalDay[] = [];
    for (const event of events) {
        const label = dayLabel(event.at);
        const last = days.at(-1);
        if (last?.label === label) last.events.push(event);
        else days.push({label, events: [event]});
    }
    return days;
}

export type {RoomFigures, SharedApp, JournalDay};
