// MAQUETTE : postes simulés, pour présenter la masterisation (RG-28) sans vraie machine à réinstaller.
/**
 * Postes simulés : quatre par salle (PC-S01…), affichés à côté des vrais postes et marqués
 * « Simulé ». Ils ont de faux processus (fermables), un système et des applications installées, et
 * se masterisent par étapes (préparation, téléchargement, système, applications, redémarrage).
 * Tout reste en mémoire : la simulation repart de zéro au rechargement de la page.
 * Utilisé par : features/supervision (use-simulated-stations.ts)
 * RG : RG-25, RG-26, RG-28
 */
import type {ProcessInfo, ReimageProgress, SupervisionEvent, Workstation} from '../models/supervision.model.ts';
import type {InstalledConfig, ReimageConfig} from '../models/reimage.model.ts';
import {configFor, configSummary, describeConfig, processName} from './simulation-catalog.mock.ts';

/** Système et image de départ de chaque poste simulé d'une salle. */
const INITIAL_CONFIGS: [osId: string, profileId: string][] = [['windows-11', 'dev'], ['windows-11', 'dev'], ['ubuntu-24', 'dev'], ['windows-11', 'office']];
/** Stagiaires fictifs des postes simulés (identifiants de session), dans l'ordre des postes. */
const TRAINEES = ['j.martin', 'l.bernard', 'a.petit', 'm.durand'];
/** Durée de chaque étape (applications : par application), et décalage entre deux postes. */
const STEP_MS = {prepare: 3000, download: 9000, system: 9000, perApp: 1500, reboot: 3000};
const STAGGER_MS = 1800;
const MOCK_LATENCY_MS = 300;

/** Masterisation en cours d'un poste simulé. */
interface ReimageJob { startAt: number; config: ReimageConfig }

/** Poste simulé : poste affiché, configuration installée, processus, masterisation en cours. */
interface SimStation { workstation: Workstation; config: ReimageConfig; processes: ProcessInfo[]; job: ReimageJob | null }

//------------------ DATAS -----------------------------------
const rooms = new Map<number, SimStation[]>();
const events: SupervisionEvent[] = [];
let nextEventId = 1;

//------------------ FUNCTIONS -------------------------------
const wait = (): Promise<void> => new Promise(resolve => setTimeout(resolve, MOCK_LATENCY_MS));

/** Ouvre au hasard quelques applications installées, lancées il y a 0 à maxMinutes minutes. */
function openApps(station: SimStation, maxMinutes: number): ProcessInfo[] {
    const {osId, appIds} = station.config;
    const count = appIds.length === 0 ? 0 : 1 + Math.floor(Math.random() * Math.min(appIds.length, 5));
    return [...appIds].sort(() => Math.random() - 0.5).slice(0, count).map(appId => ({
        pid: 1000 + Math.floor(Math.random() * 30_000), workstationId: station.workstation.id,
        name: processName(appId, osId) ?? appId, startedAt: new Date(Date.now() - Math.random() * maxMinutes * 60_000),
    }));
}

/** Crée les postes simulés d'une salle ; identifiants négatifs, jamais ceux d'une vraie UC. */
function createStations(roomId: number): SimStation[] {
    const hex = (roomId % 256).toString(16).padStart(2, '0');
    return INITIAL_CONFIGS.map(([osId, profileId], index) => {
        const n = String(index + 1).padStart(2, '0');
        const workstation: Workstation = {
            id: -(roomId * 100 + index + 1), roomId, name: `PC-S${n}`, hostname: `pc-s${n}`, sessionUser: TRAINEES[index] ?? null,
            macAddress: `02:00:00:${hex}:00:${n}`, status: 'online', simulated: true, reimage: null,
        };
        const station: SimStation = {workstation, config: configFor(osId, profileId), processes: [], job: null};
        station.processes = openApps(station, 90);
        return station;
    });
}

function stationsOf(roomId: number): SimStation[] {
    if (!rooms.has(roomId)) rooms.set(roomId, createStations(roomId));
    return rooms.get(roomId) ?? [];
}

const findStation = (workstationId: number): SimStation | undefined =>
    [...rooms.values()].flat().find(station => station.workstation.id === workstationId);

/** Avancement d'une masterisation à l'instant présent ; null quand elle est terminée. */
function progressOf(job: ReimageJob): ReimageProgress | null {
    const apps = job.config.appIds.length;
    const steps: [string, number][] = [['Préparation', STEP_MS.prepare], ["Téléchargement de l'image", STEP_MS.download],
        ['Installation du système', STEP_MS.system], ['Installation des applications', Math.max(apps, 1) * STEP_MS.perApp],
        ['Redémarrage', STEP_MS.reboot]];
    const total = steps.reduce((sum, [, ms]) => sum + ms, 0);
    let elapsed = Date.now() - job.startAt;
    if (elapsed < 0) return {phase: 'En attente', percent: 0};
    if (elapsed >= total) return null;
    const percent = Math.round((elapsed / total) * 100);
    for (const [label, ms] of steps) {
        if (elapsed < ms) {
            const app = Math.min(apps, Math.floor(elapsed / STEP_MS.perApp) + 1);
            return {phase: label === 'Installation des applications' && apps > 0 ? `${label} (${app}/${apps})` : label, percent};
        }
        elapsed -= ms;
    }
    return null;
}

/** Termine la masterisation d'un poste si elle est finie : nouvelle configuration, applications rouvertes. */
function settle(station: SimStation): ReimageProgress | null {
    if (!station.job) return null;
    const progress = progressOf(station.job);
    if (progress) return progress;
    station.config = station.job.config;
    station.job = null;
    station.processes = openApps(station, 0.5);
    return null;
}

function addEvent(station: SimStation, event: Pick<SupervisionEvent, 'kind' | 'processName' | 'detail'>): void {
    events.unshift({
        id: `sim-${nextEventId++}`, roomId: station.workstation.roomId, workstationName: station.workstation.name,
        succeeded: true, at: new Date(), simulated: true, ...event,
    });
}

export const simulationServices = {
    /** Postes simulés d'une salle, avec leur masterisation en cours (pendant laquelle aucune session n'est ouverte). */
    findWorkstations: (roomId: number): Workstation[] => stationsOf(roomId).map(station => {
        const reimage = settle(station);
        return {...station.workstation, status: reimage ? 'reimaging' : 'online', reimage, sessionUser: reimage ? null : station.workstation.sessionUser};
    }),

    /** Processus des postes simulés d'une salle (aucun pendant une masterisation). */
    findProcesses: (roomId: number): ProcessInfo[] => stationsOf(roomId).flatMap(station => (settle(station) ? [] : station.processes)),

    /** Actions sur les postes simulés d'une salle, la plus récente en premier. */
    findEvents: (roomId: number): SupervisionEvent[] => events.filter(event => event.roomId === roomId),

    /** Système, image et applications installées d'un poste simulé, ou null pour un vrai poste. */
    describe: (workstationId: number): InstalledConfig | null => {
        const station = findStation(workstationId);
        return station ? describeConfig(station.config) : null;
    },

    /** Vrai tant qu'une masterisation simulée est en cours (l'écran se met alors à jour chaque seconde). */
    hasActiveReimage: (): boolean => [...rooms.values()].flat().some(station => station.job !== null),

    /** Ferme une application d'un poste simulé (RG-26), inscrite au journal. */
    kill: async (process: ProcessInfo): Promise<void> => {
        await wait();
        const station = findStation(process.workstationId);
        if (!station) return;
        station.processes = station.processes.filter(item => item.pid !== process.pid);
        addEvent(station, {kind: 'kill', processName: process.name, detail: null});
    },

    /** Lance la masterisation simulée de postes (RG-28), en décalé, inscrite au journal. */
    reimage: async (workstationIds: number[], config: ReimageConfig): Promise<void> => {
        await wait();
        workstationIds.map(findStation).filter(station => station !== undefined).forEach((station, index) => {
            station.job = {startAt: Date.now() + index * STAGGER_MS, config};
            addEvent(station, {kind: 'reimage', processName: null, detail: configSummary(config)});
        });
    },
};
