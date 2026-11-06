import {describe, expect, it} from 'vitest';
import type {Material} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {identify, serialKey} from './identify.ts';
import {parseScan} from './parse-scan.ts';

const room10: Room = {id: 1, version: 0, number: 10, building: {id: 1, version: 0, number: 101}};
const room20: Room = {id: 3, version: 0, number: 20, building: {id: 2, version: 0, number: 102}};

/** Matériel de test, placé dans une salle ou en stock. */
function material(serialNumber: string, room: Room | null, archived = false): Material {
    return {id: 1, version: 0, serialNumber, materialType: 'UC', endAvailabilityDate: null, room, archived};
}

/** Recherche sur une petite liste, insensible à la casse comme use-inventory. */
function lookupIn(materials: Material[]) {
    return (serial: string) => materials.find((m) => serialKey(m.serialNumber) === serialKey(serial)) ?? null;
}

const LABEL = 'SGY12.SS4SG0548\nSS4SG0548  2CF05DC90096 2020-057 1300157350 10/2024 7021-146027780';

describe('identify', () => {
    it('signale un matériel absent du parc', () => {
        expect(identify(parseScan(LABEL, 'camera'), lookupIn([]), room10).verdict).toBe('unknown');
    });

    it('reconnaît un matériel dans la salle courante', () => {
        const result = identify(parseScan(LABEL, 'camera'), lookupIn([material('SS4SG0548', room10)]), room10);
        expect(result.verdict).toBe('conform');
        expect(result.material?.serialNumber).toBe('SS4SG0548');
    });

    it('signale un matériel déclaré dans une autre salle', () => {
        expect(identify(parseScan(LABEL, 'camera'), lookupIn([material('SS4SG0548', room20)]), room10).verdict)
            .toBe('moved');
    });

    it('signale un matériel déclaré en stock', () => {
        expect(identify(parseScan(LABEL, 'camera'), lookupIn([material('SS4SG0548', null)]), room10).verdict)
            .toBe('stock');
    });

    it('signale un matériel archivé, quelle que soit la salle', () => {
        expect(identify(parseScan(LABEL, 'camera'), lookupIn([material('SS4SG0548', null, true)]), room10).verdict)
            .toBe('archived');
    });

    it("identifie sans verdict d'emplacement si aucune salle n'est choisie", () => {
        expect(identify(parseScan(LABEL, 'camera'), lookupIn([material('SS4SG0548', room20)]), null).verdict)
            .toBe('known');
    });

    it("trouve le matériel par un candidat secondaire (saisie en minuscules)", () => {
        const result = identify(parseScan('sgy12.ss4sg0548', 'manual'), lookupIn([material('SS4SG0548', room10)]), room10);
        expect(result.verdict).toBe('conform');
    });
});
