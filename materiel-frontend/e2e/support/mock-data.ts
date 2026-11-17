/**
 * Données de la fausse API des tests navigateur : un état en mémoire, neuf pour chaque test.
 * Les objets ont la forme JSON des réponses du backend (DTO) : le front les lit comme les vraies.
 * Utilisé par : mock-api.ts, et les tests qui ajoutent leurs propres données (addRoom, addMaterial).
 */

//------------------ TYPES -----------------------------------
export type Role = 'ADMIN' | 'MANAGER' | 'USER';
export interface BuildingJson { id: number; version: number; number: number }
export interface RoomJson { id: number; version: number; number: number; building: BuildingJson }
export interface MaterialJson {
    id: number; version: number; serialNumber: string; materialType: string;
    endAvailabilityDate: string | null; room: RoomJson | null; archived: boolean;
}
export interface UserJson { id: number; version: number; username: string; role: Role; enabled: boolean }
export interface WorkstationJson {
    id: number; roomId: number; name: string; traineeLabel: string; macAddress: string;
    online: boolean; hostname: string; username: string; os: string;
}
export interface ProcessJson { pid: number; workstationId: number; name: string; startedAt: string; icon: string | null }
export interface ApplicationJson {
    id: number; version: number; name: string; displayName: string; iconBase64: string | null; blacklisted: boolean;
}
export interface KillEventJson {
    id: number; version: number; processName: string; uc: MaterialJson; killDateTime: string; result: boolean; user: UserJson | null;
}
export interface ReportJson {
    id: number; version: number; description: string; reportDateTime: string; user: UserJson | null;
    material: MaterialJson; handleDateTime: string | null; handled: boolean;
}
/** Requête reçue par la fausse API, pour vérifier ce que le front a envoyé. */
export interface MockRequest { method: string; path: string; query: string; body: Record<string, unknown> | null }

/** État complet de la fausse API. */
export interface MockState {
    me: UserJson; buildings: BuildingJson[]; rooms: RoomJson[]; materials: MaterialJson[]; users: UserJson[];
    workstations: WorkstationJson[]; processes: ProcessJson[]; applications: ApplicationJson[];
    killEvents: KillEventJson[]; reports: ReportJson[]; requests: MockRequest[];
}

//------------------ VALEURS ---------------------------------
/** Icône PNG de 2 × 2 pixels, au format data:, comme celles que remontent les agents. */
export const PNG = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAFklEQVR4nGP8z8DwnwEIGBmhDAYGBgAkBgP/0XGaSAAAAABJRU5ErkJggg==';

//------------------ FUNCTIONS -------------------------------
/**
 * État de départ : bâtiment 101, salle 10 avec l'UC « UC-001 », trois comptes (un par rôle), deux postes
 * supervisés en ligne dans la salle 10, quatre applications dont une interdite (liste noire).
 * @param role rôle du compte connecté
 */
export function createState(role: Role): MockState {
    const building = {id: 1, version: 0, number: 101};
    const room = {id: 1, version: 0, number: 10, building};
    const users: UserJson[] = [
        {id: 1, version: 0, username: 'admin', role: 'ADMIN', enabled: true},
        {id: 2, version: 0, username: 'user', role: 'USER', enabled: true},
        {id: 3, version: 0, username: 'manager', role: 'MANAGER', enabled: true},
    ];
    return {
        me: users.find(user => user.role === role) ?? users[0],
        buildings: [building],
        rooms: [room],
        materials: [{id: 1, version: 0, serialNumber: 'UC-001', materialType: 'UC', endAvailabilityDate: null, room, archived: false}],
        users,
        workstations: [
            {id: 11, roomId: 1, name: 'win-01 (SN-WIN-01)', traineeLabel: '', macAddress: '00:11:22:33:44:55', online: true, hostname: 'win-01', username: 'sch.martin', os: 'windows'},
            {id: 12, roomId: 1, name: 'lin-02 (SN-LIN-02)', traineeLabel: '', macAddress: '00:11:22:33:44:66', online: true, hostname: 'lin-02', username: 'sch.durand', os: 'linux'},
        ],
        processes: [
            {pid: 101, workstationId: 11, name: 'chrome.exe', startedAt: '2026-11-13T08:00:00', icon: PNG},
            {pid: 102, workstationId: 11, name: 'steam.exe', startedAt: '2026-11-13T08:10:00', icon: PNG},
            {pid: 103, workstationId: 12, name: 'firefox', startedAt: '2026-11-13T08:20:00', icon: null},
            {pid: 104, workstationId: 12, name: 'steam', startedAt: '2026-11-13T08:21:00', icon: 'javascript:alert(1)'},
        ],
        applications: [
            {id: 1, version: 0, name: 'chrome.exe', displayName: 'Chrome', iconBase64: PNG, blacklisted: false},
            {id: 2, version: 0, name: 'steam.exe', displayName: 'Steam', iconBase64: PNG, blacklisted: false},
            {id: 3, version: 0, name: 'firefox', displayName: 'Firefox', iconBase64: null, blacklisted: false},
            {id: 4, version: 0, name: 'minecraft.exe', displayName: 'Minecraft', iconBase64: null, blacklisted: true},
        ],
        killEvents: [],
        reports: [],
        requests: [],
    };
}

/**
 * Ajoute une salle, et son bâtiment s'il n'existe pas encore.
 * @returns la salle créée
 */
export function addRoom(state: MockState, id: number, number: number, buildingNumber: number): RoomJson {
    let building = state.buildings.find(candidate => candidate.number === buildingNumber);
    if (!building) {
        building = {id: state.buildings.length + 1, version: 0, number: buildingNumber};
        state.buildings.push(building);
    }
    const room = {id, version: 0, number, building};
    state.rooms.push(room);
    return room;
}

/**
 * Ajoute un matériel non archivé.
 * @param room salle du matériel, ou null pour le stock
 * @param endAvailabilityDate fin de disponibilité 'AAAA-MM-JJ', ou null
 */
export function addMaterial(state: MockState, serialNumber: string, materialType: string, room: RoomJson | null,
                            endAvailabilityDate: string | null = null): MaterialJson {
    const material = {id: state.materials.length + 1, version: 0, serialNumber, materialType, endAvailabilityDate, room, archived: false};
    state.materials.push(material);
    return material;
}
