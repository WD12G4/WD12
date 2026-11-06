/**
 * Identification d'une lecture par rapport au parc connu et à la salle où se trouve l'agent.
 * Fonctions pures, testées dans identify.test.ts.
 * RG : RG-06 (un matériel archivé ne peut pas être affecté), RG-07 (sans salle = en stock)
 */
import type {Material} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import type {ScanReading, ScanVerdict} from '../../../models/scan.model.ts';

/** Recherche d'un matériel par numéro de série (fournie par use-inventory). */
type SerialLookup = (serialNumber: string) => Material | null;

/** Résultat de l'identification. */
interface Identification {
    verdict: ScanVerdict;
    material: Material | null;
}

/**
 * Clé de recherche d'un numéro de série : sans espaces autour, en majuscules.
 * Les étiquettes sont en majuscules ; une saisie manuelle peut ne pas l'être.
 * @param serialNumber numéro à normaliser
 */
export function serialKey(serialNumber: string): string {
    return serialNumber.trim().toUpperCase();
}

/**
 * Cherche le premier candidat connu du parc.
 * @param reading lecture analysée
 * @param lookup recherche par numéro de série
 */
function findMaterial(reading: ScanReading, lookup: SerialLookup): Material | null {
    for (const candidate of reading.candidates) {
        const material = lookup(candidate);
        if (material) return material;
    }
    return null;
}

/**
 * Compare la lecture au parc : conforme, déplacé, en stock, archivé, identifié ou inconnu.
 * @param reading lecture analysée
 * @param lookup recherche par numéro de série
 * @param currentRoom salle où se trouve l'agent, ou null s'il n'en a pas choisi
 */
export function identify(reading: ScanReading, lookup: SerialLookup, currentRoom: Room | null): Identification {
    const material = findMaterial(reading, lookup);
    if (material === null) return {verdict: 'unknown', material};
    if (material.archived) return {verdict: 'archived', material};
    if (currentRoom === null) return {verdict: 'known', material};
    if (material.room === null) return {verdict: 'stock', material};
    return {verdict: material.room.id === currentRoom.id ? 'conform' : 'moved', material};
}

export type {SerialLookup, Identification};
