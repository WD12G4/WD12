import {describe, expect, it} from 'vitest';
import {parseScan} from './parse-scan.ts';

/** Texte lu par l'iPhone sur l'étiquette Data Matrix de référence. */
const REFERENCE_LABEL = 'SGY12.SS4SG0548\nSS4SG0548  2CF05DC90096 2020-057 1300157350 10/2024 7021-146027780';

describe('parseScan', () => {
    it("extrait le numéro de série de l'étiquette de référence", () => {
        const reading = parseScan(REFERENCE_LABEL, 'camera');
        expect(reading.serialNumber).toBe('SS4SG0548');
        expect(reading.candidates[0]).toBe('SS4SG0548');
    });

    it('relève la MAC, le NNO et en déduit une unité centrale (classe 7021)', () => {
        const reading = parseScan(REFERENCE_LABEL, 'camera');
        expect(reading.macAddress).toBe('2CF05DC90096');
        expect(reading.nsn).toBe('7021-14-602-7780');
        expect(reading.typeHint).toBe('UC');
    });

    it('accepte les séparateurs de contrôle (GS, RS) et les fins de ligne Windows', () => {
        const reading = parseScan('SGY12.SS4SG0548\r\nSS4SG0548\u001d2CF05DC90096', 'camera');
        expect(reading.serialNumber).toBe('SS4SG0548');
        expect(reading.macAddress).toBe('2CF05DC90096');
    });

    it("garde un code simple tel quel (code-barres, saisie manuelle)", () => {
        const reading = parseScan('  UC-DELL-001 ', 'manual');
        expect(reading.serialNumber).toBe('UC-DELL-001');
        expect(reading.nsn).toBeNull();
        expect(reading.typeHint).toBeNull();
    });

    it('propose la partie après le point puis le texte entier pour une seule ligne', () => {
        const reading = parseScan('SGY12.SS4SG0548', 'manual');
        expect(reading.candidates.slice(0, 2)).toEqual(['SS4SG0548', 'SGY12.SS4SG0548']);
    });

    it("ne prend pas un nombre de 12 chiffres pour une adresse MAC", () => {
        expect(parseScan('ABC 123456789012', 'manual').macAddress).toBeNull();
    });

    it('renvoie un numéro vide pour un texte vide', () => {
        expect(parseScan(' \n ', 'manual').serialNumber).toBe('');
    });
});
