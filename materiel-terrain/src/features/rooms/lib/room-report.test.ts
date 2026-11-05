import {describe, expect, it} from 'vitest';
import type {Material} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {buildRoomReport, locateMaterial, roomState} from './room-report.ts';

const room10: Room = {id: 1, version: 0, number: 10, building: {id: 1, version: 0, number: 101}};
const room20: Room = {id: 3, version: 0, number: 20, building: {id: 2, version: 0, number: 102}};

/** Matériel de test, dans une salle ou en stock. */
function material(id: number, serialNumber: string, room: Room | null, archived = false): Material {
    return {id, version: 0, serialNumber, materialType: 'UC', endAvailabilityDate: null, room, archived};
}

const parc = [
    material(1, 'UC-DELL-001', room10),
    material(2, 'UC-DELL-002', room10),
    material(3, 'PRJ-EPSON-001', room20),
    material(4, 'SCR-LG-009', null),
    material(5, 'UC-HP-OLD-099', null, true),
];

describe('buildRoomReport', () => {
    it('confronte le matériel théoriquement sur place à celui réellement scanné', () => {
        const report = buildRoomReport(room10.id, parc, {
            'UC-DELL-001': '2026-11-05T01:00:00.000Z',
            'PRJ-EPSON-001': '2026-11-05T01:05:00.000Z',
            'SCR-LG-009': '2026-11-05T01:06:00.000Z',
            'UC-HP-OLD-099': '2026-11-05T01:07:00.000Z',
            'SS4SG0548': '2026-11-05T01:10:00.000Z',
        });

        expect(report.declared.map((line) => [line.serialNumber, line.status])).toEqual([
            ['UC-DELL-002', 'missing'],
            ['UC-DELL-001', 'present'],
        ]);
        expect(report.observed.map((line) => [line.serialNumber, line.status])).toEqual([
            ['SS4SG0548', 'unknown'],
            ['UC-HP-OLD-099', 'archived'],
            ['PRJ-EPSON-001', 'elsewhere'],
            ['SCR-LG-009', 'stock'],
            ['UC-DELL-001', 'present'],
        ]);
        expect(report.missing).toBe(1);
        expect(report.extra).toBe(4);
        expect(report.lastSeenAt?.toISOString()).toBe('2026-11-05T01:10:00.000Z');
    });

    it('retrouve un numéro scanné quelle que soit sa casse dans le parc', () => {
        const report = buildRoomReport(room10.id, [material(1, 'uc-dell-001', room10)], {'UC-DELL-001': '2026-11-05T01:00:00.000Z'});
        expect(report.observed[0]).toMatchObject({serialNumber: 'uc-dell-001', status: 'present'});
        expect(report.declared[0].status).toBe('present');
    });
});

describe('roomState', () => {
    it("distingue salle jamais reconnue, conforme et avec écarts", () => {
        expect(roomState(buildRoomReport(room10.id, parc, {}))).toBe('unchecked');
        const all = {'UC-DELL-001': '2026-11-05T01:00:00.000Z', 'UC-DELL-002': '2026-11-05T01:01:00.000Z'};
        expect(roomState(buildRoomReport(room10.id, parc, all))).toBe('conform');
        expect(roomState(buildRoomReport(room10.id, parc, {'UC-DELL-001': '2026-11-05T01:00:00.000Z'}))).toBe('gaps');
    });
});

describe('locateMaterial', () => {
    it("situe un matériel d'après son dernier scan", () => {
        const line = locateMaterial(parc[0], [
            {roomId: room10.id, at: new Date('2026-11-05T01:00:00.000Z')},
            {roomId: room20.id, at: new Date('2026-11-05T02:00:00.000Z')},
        ]);
        expect(line).toMatchObject({status: 'elsewhere', lastRoomId: room20.id});
    });

    it('signale un matériel jamais scanné : non constaté, ou en stock', () => {
        expect(locateMaterial(parc[0], []).status).toBe('missing');
        expect(locateMaterial(parc[3], []).status).toBe('stock');
    });
});
