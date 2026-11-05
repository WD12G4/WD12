/**
 * Compte rendu d'une salle : ce que le parc déclare (théoriquement sur place) face à ce qui a été scanné
 * dans la salle (réellement sur place). Simple lecture : rien n'est vérifié ni modifié ici.
 * Fonctions pures, testées dans room-report.test.ts.
 * Utilisé par : features/rooms
 * RG : RG-06 (archivé), RG-07 (sans salle = en stock)
 */
import type {Material} from '../../../models/material.model.ts';
import {serialKey} from '../../scan/lib/identify.ts';

/**
 * Situation d'une ligne :
 * present = déclaré ici et scanné ici ; missing = déclaré ici, pas scanné ici ;
 * elsewhere = scanné ici, déclaré dans une autre salle ; stock = scanné ici, déclaré en stock ;
 * unknown = scanné ici, absent du parc ; archived = scanné ici, archivé.
 */
type LineStatus = 'present' | 'missing' | 'elsewhere' | 'stock' | 'unknown' | 'archived';

/** Un matériel dans le compte rendu. */
interface ReportLine {
    /** Numéro du parc, ou numéro lu pour un matériel non recensé. */
    serialNumber: string;
    material: Material | null;
    status: LineStatus;
    /** Dernier scan dans la salle, ou null s'il n'y a pas été scanné. */
    seenAt: Date | null;
}

/** Compte rendu complet d'une salle. */
interface RoomReport {
    /** Théoriquement sur place : déclarés dans la salle selon le parc (hors archivés). */
    declared: ReportLine[];
    /** Réellement sur place : scannés dans la salle. */
    observed: ReportLine[];
    /** Déclarés ici mais pas scannés ici. */
    missing: number;
    /** Scannés ici mais pas déclarés ici : déplacés, en stock, non recensés, archivés. */
    extra: number;
    /** Dernier scan dans la salle, ou null si elle n'a jamais été reconnue. */
    lastSeenAt: Date | null;
}

/** État d'une salle dans la liste : jamais reconnue, conforme, ou avec écarts. */
type RoomState = 'unchecked' | 'conform' | 'gaps';

/** Numéros scannés dans une salle, avec la date du dernier scan (ISO) : { "SS4SG0548": "2026-11-05T…" }. */
type SeenInRoom = Record<string, string>;

/** Ordre d'affichage : les écarts d'abord. */
const STATUS_ORDER: LineStatus[] = ['unknown', 'archived', 'elsewhere', 'stock', 'missing', 'present'];

function byStatusThenSerial(a: ReportLine, b: ReportLine): number {
    return STATUS_ORDER.indexOf(a.status) - STATUS_ORDER.indexOf(b.status)
        || a.serialNumber.localeCompare(b.serialNumber);
}

/** Situation d'un matériel scanné dans la salle. */
function observedStatus(material: Material | null, roomId: number): LineStatus {
    if (material === null) return 'unknown';
    if (material.archived) return 'archived';
    if (material.room === null) return 'stock';
    return material.room.id === roomId ? 'present' : 'elsewhere';
}

/**
 * Construit le compte rendu d'une salle.
 * @param roomId salle décrite
 * @param materials parc en cache
 * @param seenInRoom numéros scannés dans cette salle
 */
export function buildRoomReport(roomId: number, materials: Material[], seenInRoom: SeenInRoom): RoomReport {
    const bySerial = new Map(materials.map((material) => [serialKey(material.serialNumber), material]));
    const seenAt = (serial: string) => (seenInRoom[serialKey(serial)] ? new Date(seenInRoom[serialKey(serial)]) : null);

    const declared = materials
        .filter((material) => !material.archived && material.room?.id === roomId)
        .map((material): ReportLine => {
            const at = seenAt(material.serialNumber);
            return {serialNumber: material.serialNumber, material, status: at ? 'present' : 'missing', seenAt: at};
        })
        .sort(byStatusThenSerial);
    const observed = Object.entries(seenInRoom)
        .map(([key, at]): ReportLine => {
            const material = bySerial.get(key) ?? null;
            const status = observedStatus(material, roomId);
            return {serialNumber: material?.serialNumber ?? key, material, status, seenAt: new Date(at)};
        })
        .sort(byStatusThenSerial);
    const times = observed.map((line) => line.seenAt!.getTime());

    return {
        declared,
        observed,
        missing: declared.filter((line) => line.status === 'missing').length,
        extra: observed.filter((line) => line.status !== 'present').length,
        lastSeenAt: times.length > 0 ? new Date(Math.max(...times)) : null,
    };
}

/**
 * État d'une salle pour la liste.
 * @param report compte rendu de la salle
 */
export function roomState(report: RoomReport): RoomState {
    if (report.lastSeenAt === null) return 'unchecked';
    return report.missing + report.extra === 0 ? 'conform' : 'gaps';
}

/** Un scan d'un matériel dans une salle. */
interface Sighting {
    roomId: number;
    at: Date;
}

/**
 * Situation d'un matériel cherché, d'après son dernier scan : sur place, déclaré ailleurs,
 * ou jamais scanné (non constaté, ou en stock s'il n'a pas de salle).
 * @param material matériel du parc
 * @param sightings ses scans, toutes salles confondues
 */
export function locateMaterial(material: Material, sightings: Sighting[]): ReportLine & {lastRoomId: number | null} {
    const last = [...sightings].sort((a, b) => b.at.getTime() - a.at.getTime())[0] ?? null;
    let status: LineStatus;
    if (last !== null) status = observedStatus(material, last.roomId);
    else if (material.archived) status = 'archived';
    else status = material.room === null ? 'stock' : 'missing';
    return {serialNumber: material.serialNumber, material, status, seenAt: last?.at ?? null, lastRoomId: last?.roomId ?? null};
}

export type {LineStatus, ReportLine, RoomReport, RoomState, SeenInRoom, Sighting};
