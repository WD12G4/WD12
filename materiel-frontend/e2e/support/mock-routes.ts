/**
 * Routes de la fausse API : matériel, remontées, supervision, applications. Chaque route reçoit l'état en
 * mémoire et la requête, et renvoie la réponse JSON. Les règles du backend dont les tests dépendent sont
 * reproduites : doublon de numéro de série refusé, archivage, auteur des remontées et des arrêts,
 * fermeture des applications interdites (liste noire).
 * Utilisé par : mock-api.ts
 */
import type {KillEventJson, MaterialJson, MockState, ReportJson} from './mock-data.ts';

//------------------ TYPES -----------------------------------
/** Requête reçue : état, paramètres du chemin, paramètres d'adresse, corps JSON, en-têtes. */
export interface Context {
    state: MockState; params: string[]; query: URLSearchParams;
    body: Record<string, unknown> | null; headers: Record<string, string>;
}
/** Réponse : code HTTP (200 par défaut) et corps JSON. */
export interface Reply { status?: number; body: unknown }
/** Une route : méthode, chemin (sans /api/v1) et traitement. */
export type ApiRoute = [method: string, pattern: RegExp, handler: (context: Context) => Reply];

//------------------ FUNCTIONS -------------------------------
/** Page de résultats, comme celles de Spring Data (tout tient sur une page). */
export const asPage = <T>(content: T[]) => ({content, number: 0, size: content.length, totalElements: content.length, totalPages: 1, first: true, last: true});
const notFound = (what: string): Reply => ({status: 404, body: {error: `${what} introuvable`}});
const byId = <T extends {id: number}>(list: T[], id: string): T | undefined => list.find(item => item.id === Number(id));

/** UC d'un poste supervisé : même identifiant que le poste, numéro de série entre parenthèses dans son nom. */
function ucOf(state: MockState, workstationId: number): MaterialJson {
    const workstation = state.workstations.find(candidate => candidate.id === workstationId);
    return {id: workstationId, version: 0, serialNumber: /\(([^)]+)\)/.exec(workstation?.name ?? '')?.[1] ?? `UC-${workstationId}`,
        materialType: 'UC', endAvailabilityDate: null, room: state.rooms.find(room => room.id === workstation?.roomId) ?? null, archived: false};
}

/** Arrêt journalisé, attribué au compte connecté, comme SupervisionServiceImpl. */
function killEvent(state: MockState, processName: string, workstationId: number): KillEventJson {
    return {id: state.killEvents.length + 1, version: 0, processName, uc: ucOf(state, workstationId), killDateTime: '2026-11-13T09:30:00',
        result: true, user: state.me};
}

//------------------ ROUTES ----------------------------------
export const materialRoutes: ApiRoute[] = [
    // Comme MaterialTypesController : l'enum EMaterialType, dans son ordre, avec les libellés.
    ['GET', /^\/material-types\/$/, () => ({body: [
        {code: 'UC', libelle: 'Unité centrale'}, {code: 'SCREEN', libelle: 'Écran'},
        {code: 'PROJECTOR', libelle: 'Projecteur'}, {code: 'PERIPHERAL', libelle: 'Périphérique'},
    ]})],
    ['GET', /^\/materials$/, ({state}) => ({body: asPage(state.materials)})],
    ['GET', /^\/materials\/(\d+)$/, ({state, params}) => {
        const material = byId(state.materials, params[0]);
        return material ? {body: material} : notFound('Matériel');
    }],
    ['POST', /^\/materials$/, ({state, body}) => {
        const input = body as {serialNumber: string; materialType: string; endAvailabilityDate: string | null; roomId: number | null};
        if (state.materials.some(material => material.serialNumber === input.serialNumber)) {
            return {status: 400, body: {error: 'Un matériel avec ce numéro de série existe déjà.'}};
        }
        const room = state.rooms.find(candidate => candidate.id === input.roomId) ?? null;
        const created = {id: state.materials.length + 1, version: 0, serialNumber: input.serialNumber, materialType: input.materialType,
            endAvailabilityDate: input.endAvailabilityDate, room, archived: false};
        state.materials.push(created);
        return {status: 201, body: created};
    }],
    ['DELETE', /^\/materials\/(\d+)$/, ({state, params}) => {
        // Comme le backend : supprimer, c'est archiver, et l'archivage libère la salle (RG-10).
        const material = byId(state.materials, params[0]);
        if (!material) return notFound('Matériel');
        Object.assign(material, {archived: true, room: null, version: material.version + 1});
        return {status: 204, body: null};
    }],
];

export const reportRoutes: ApiRoute[] = [
    ['GET', /^\/reports$/, ({state, query}) => {
        const userId = query.get('userId');
        const handled = query.get('handled');
        const reports = state.reports.filter(report => (!userId || report.user?.id === Number(userId))
            && (handled === null || String(report.handled) === handled));
        return {body: asPage(reports)};
    }],
    ['GET', /^\/reports\/(\d+)$/, ({state, params}) => {
        const report = byId(state.reports, params[0]);
        return report ? {body: report} : notFound('Signalement');
    }],
    ['POST', /^\/reports$/, ({state, body}) => {
        // Comme le backend : l'auteur et la date sont fixés par le serveur.
        const input = body as {description: string; materialId: number};
        const material = byId(state.materials, String(input.materialId));
        if (!material) return notFound('Matériel');
        const created: ReportJson = {id: state.reports.length + 1, version: 0, description: input.description,
            reportDateTime: '2026-11-16T10:00:00', user: state.me, material, handleDateTime: null, handled: false};
        state.reports.push(created);
        return {status: 201, body: created};
    }],
    ['PATCH', /^\/reports\/(\d+)\/handle$/, ({state, params, query}) => {
        const report = byId(state.reports, params[0]);
        if (!report) return notFound('Signalement');
        const handled = query.get('handled') !== 'false';
        Object.assign(report, {handled, handleDateTime: handled ? '2026-11-16T11:00:00' : null, version: report.version + 1});
        return {body: report};
    }],
];

export const supervisionRoutes: ApiRoute[] = [
    ['GET', /^\/supervision\/rooms\/(\d+)\/workstations$/, ({state, params}) => (
        {body: state.workstations.filter(workstation => workstation.roomId === Number(params[0]))})],
    ['GET', /^\/supervision\/processes$/, ({state, query}) => {
        // Comme le backend : une application interdite est fermée (absente de la liste) et l'arrêt journalisé.
        const forbidden = new Set(state.applications.filter(app => app.blacklisted).map(app => app.name.toLowerCase()));
        for (const process of state.processes.filter(candidate => forbidden.has(candidate.name.toLowerCase()))) {
            state.killEvents.unshift(killEvent(state, process.name, process.workstationId));
        }
        state.processes = state.processes.filter(process => !forbidden.has(process.name.toLowerCase()));
        // Le nom affiché vient du catalogue des applications, comme dans SupervisionServiceImpl.
        const ids = query.getAll('workstationIds').map(Number);
        return {body: state.processes.filter(process => ids.includes(process.workstationId)).map(process => ({...process,
            displayName: state.applications.find(app => app.name.toLowerCase() === process.name.toLowerCase())?.displayName ?? process.name}))};
    }],
    ['POST', /^\/supervision\/kill$/, ({state, body}) => {
        const input = body as {workstationId: number; pid: number; processName: string};
        state.killEvents.unshift(killEvent(state, input.processName, input.workstationId));
        state.processes = state.processes.filter(process => process.pid !== input.pid);
        return {body: {success: true, message: 'ok', killEventId: state.killEvents.length}};
    }],
    ['GET', /^\/killevents$/, ({state}) => ({body: asPage(state.killEvents)})],
];

export const applicationRoutes: ApiRoute[] = [
    ['GET', /^\/applications$/, ({state}) => ({body: asPage(state.applications)})],
    ['PATCH', /^\/applications\/(\d+)\/blacklist$/, ({state, params, body}) => {
        const app = byId(state.applications, params[0]);
        if (!app) return notFound('Application');
        Object.assign(app, {blacklisted: (body as {blacklisted: boolean}).blacklisted, version: app.version + 1});
        return {body: app};
    }],
];
