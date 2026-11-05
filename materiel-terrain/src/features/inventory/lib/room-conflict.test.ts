import {describe, expect, it} from 'vitest';
import type {Material} from '../../../models/material.model.ts';
import type {Room} from '../../../models/room.model.ts';
import {checkRoom} from './room-conflict.ts';

const room = (id: number): Room => ({id, version: 0, number: id * 10, building: {id: 1, version: 0, number: 101}});

/** Matériel tel que relu sur le serveur. */
function server(roomId: number | null, archived = false): Material {
    return {
        id: 1, version: 3, serialNumber: 'SS4SG0548', materialType: 'UC',
        endAvailabilityDate: null, room: roomId === null ? null : room(roomId), archived,
    };
}

describe('checkRoom', () => {
    it("envoie quand le serveur n'a pas bougé depuis ce que l'agent a vu", () => {
        expect(checkRoom(server(1), 1, 2)).toBe('apply');
        expect(checkRoom(server(null), null, 2)).toBe('apply');
    });

    it('signale un conflit si quelqu\'un a déplacé le matériel entre-temps', () => {
        // L'agent l'a vu en salle 1 et le met en salle 2 ; un autre l'a mis en salle 3 entre-temps.
        expect(checkRoom(server(3), 1, 2)).toBe('conflict');
        // Mis en stock par un autre.
        expect(checkRoom(server(null), 1, 2)).toBe('conflict');
    });

    it("ne refait rien si le serveur est déjà dans l'état voulu", () => {
        expect(checkRoom(server(2), 1, 2)).toBe('already-done');
        expect(checkRoom(server(null), 1, null)).toBe('already-done');
    });

    it('signale un conflit si le matériel a été archivé entre-temps', () => {
        expect(checkRoom(server(null, true), null, 2)).toBe('conflict');
    });

    it('envoie sans comparer une action enregistrée sans salle de référence', () => {
        expect(checkRoom(server(3), undefined, 2)).toBe('apply');
    });
});
