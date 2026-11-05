/**
 * Parc en cache sur le téléphone : tous les matériels et toutes les salles, pour identifier sans réseau.
 * L'état est partagé au niveau du module ; il est rechargé depuis le stockage local au démarrage.
 * Utilisé par : use-sync-queue.ts, use-field-actions.ts, features/location, features/scan
 */
import {computed, ref} from 'vue';
import {isProvisional, type Material} from '../../../models/material.model.ts';
import {type MaterialDto, materialFromDto, materialToDto} from '../../../models/material.dto.ts';
import type {Room} from '../../../models/room.model.ts';
import {type RoomDto, roomFromDto, roomToDto} from '../../../models/room.dto.ts';
import {materialsServices} from '../../../services/materials.services.ts';
import {roomsServices} from '../../../services/rooms.services.ts';
import {readStored, writeStored} from '../../../services/local-store.ts';
import {serialKey} from '../../scan/lib/identify.ts';

/** Forme enregistrée dans le stockage local. */
interface StoredInventory {
    materials: MaterialDto[];
    rooms: RoomDto[];
    /** Date du dernier téléchargement complet (ISO), ou null. */
    syncedAt: string | null;
}

const STORE_KEY = 'inventory';

//------------------ DATAS -----------------------------------
const stored = readStored<StoredInventory | null>(STORE_KEY, null);
const materials = ref<Material[]>(stored?.materials.map(materialFromDto) ?? []);
const rooms = ref<Room[]>(stored?.rooms.map(roomFromDto) ?? []);
const syncedAt = ref<Date | null>(stored?.syncedAt ? new Date(stored.syncedAt) : null);

//------------------ COMPUTED --------------------------------
/** Index des matériels par numéro de série normalisé. */
const bySerial = computed(() => new Map(materials.value.map((material) => [serialKey(material.serialNumber), material])));

//------------------ FUNCTIONS -------------------------------
/** Enregistre le parc sur le téléphone. */
function save(): void {
    writeStored(STORE_KEY, {
        materials: materials.value.map(materialToDto),
        rooms: rooms.value.map(roomToDto),
        syncedAt: syncedAt.value?.toISOString() ?? null,
    } satisfies StoredInventory);
}

/** Cherche un matériel par numéro de série (insensible à la casse). */
function findBySerial(serialNumber: string): Material | null {
    return bySerial.value.get(serialKey(serialNumber)) ?? null;
}

/** Cherche une salle par identifiant. */
function roomById(id: number): Room | null {
    return rooms.value.find((room) => room.id === id) ?? null;
}

/** Ajoute ou remplace un matériel (réponse de l'API, ou matériel provisoire hors ligne). */
function upsertMaterial(material: Material): void {
    const index = materials.value.findIndex((existing) => existing.id === material.id);
    if (index >= 0) {
        materials.value.splice(index, 1, material);
    } else {
        materials.value.push(material);
    }
    save();
}

/** Retire un matériel du cache (matériel provisoire remplacé ou abandonné). */
function removeMaterial(id: number): void {
    materials.value = materials.value.filter((material) => material.id !== id);
    save();
}

/** Change localement la salle d'un matériel, en attendant la confirmation de l'API. */
function moveMaterial(id: number, room: Room | null): void {
    const material = materials.value.find((existing) => existing.id === id);
    if (material) upsertMaterial({...material, room});
}

/**
 * Télécharge le parc complet. Les matériels pris en compte hors ligne et pas encore envoyés sont conservés,
 * sauf si le serveur connaît déjà leur numéro (créé entre-temps) : jamais deux fiches pour un même numéro.
 * @throws ApiError si l'API répond en erreur ou ne répond pas
 */
async function download(): Promise<void> {
    const [apiMaterials, apiRooms] = await Promise.all([materialsServices.findAll(), roomsServices.findAll()]);
    const onServer = new Set(apiMaterials.map((material) => serialKey(material.serialNumber)));
    materials.value = [...apiMaterials, ...materials.value.filter((material) =>
        isProvisional(material) && !onServer.has(serialKey(material.serialNumber)))];
    rooms.value = apiRooms;
    syncedAt.value = new Date();
    save();
}

/**
 * Accès au parc partagé.
 * @returns le parc, sa date de synchronisation et ses opérations
 */
export function useInventory() {
    return {materials, rooms, syncedAt, findBySerial, roomById, upsertMaterial, removeMaterial, moveMaterial, download};
}
