/**
 * Extraction du numéro de série dans le texte lu (Data Matrix, QR code, code-barres) ou saisi.
 * Fonctions pures, testées dans parse-scan.test.ts.
 *
 * Étiquette de référence (Data Matrix lu par l'iPhone) :
 *   SGY12.SS4SG0548
 *   SS4SG0548  2CF05DC90096 2020-057 1300157350 10/2024 7021-146027780
 * → numéro de série SS4SG0548 : il termine la 1re ligne (après le point) ET ouvre la 2e ligne.
 *   On y trouve aussi une adresse MAC probable (2CF05DC90096) et un numéro OTAN (NNO 7021-14-602-7780).
 */
import {type MaterialType, serialNumberMaxLength} from '../../../models/material.model.ts';
import type {ScanReading, ScanSource} from '../../../models/scan.model.ts';

//----------------- VALEURS ------------------------------
/** Fins de ligne, y compris le séparateur d'enregistrement (RS) utilisé par certaines étiquettes. */
const LINE_BREAKS = /[\r\n\u001e]+/;
/** Autres caractères de contrôle (GS, EOT…) : traités comme des espaces. */
const CONTROL_CHARS = /[\u0000-\u001f\u007f]/g;
/** 12 caractères hexadécimaux dont au moins une lettre : adresse MAC probable. */
const MAC_PATTERN = /^(?=.*[A-F])[0-9A-F]{12}$/i;
/** Numéro OTAN : classe (4 chiffres), pays (2), article (7), avec ou sans tirets. */
const NSN_PATTERN = /^(\d{4})-?(\d{2})-?(\d{3})-?(\d{4})$/;

/**
 * Classe OTAN (4 premiers chiffres du NNO) → type de matériel probable. Simple suggestion, à confirmer.
 * 7010 : configuration de système informatique ; 7021/7022 : unité centrale ; 6730 : projection.
 */
const typeByNsnClass: Readonly<Record<string, MaterialType>> = {
    '7010': 'UC',
    '7021': 'UC',
    '7022': 'UC',
    '6730': 'PROJECTOR',
};

//----------------- FONCTIONS ----------------------------
/** Supprime les doublons et les chaînes vides en gardant l'ordre. */
function unique(values: string[]): string[] {
    return [...new Set(values.filter((value) => value.length > 0))];
}

/** Premier mot d'une ligne. */
function firstWord(line: string | undefined): string {
    return (line ?? '').split(/\s+/)[0] ?? '';
}

/**
 * Candidats les plus probables, selon la forme des deux premières lignes.
 * @param lines lignes non vides du texte lu
 */
function preferredCandidates(lines: string[]): string[] {
    const first = firstWord(lines[0]);
    const afterDot = first.includes('.') ? first.slice(first.lastIndexOf('.') + 1) : '';
    const secondLead = firstWord(lines[1]);
    // « PRÉFIXE.SÉRIE » puis « SÉRIE … » : les deux lignes se confirment, le doute est levé.
    if (afterDot !== '' && afterDot === secondLead) {
        return [afterDot];
    }
    // Sinon : la partie après le point, puis le premier mot (code simple « UC-DELL-001 »), puis la 2e ligne.
    return [afterDot, first, secondLead];
}

/**
 * Met un numéro OTAN au format lisible 7021-14-602-7780.
 * @param token mot lu
 * @returns le NNO formaté, ou null si le mot n'en est pas un
 */
function formatNsn(token: string): string | null {
    const match = NSN_PATTERN.exec(token);
    return match ? match.slice(1).join('-') : null;
}

/**
 * Analyse le texte lu et en extrait le numéro de série et les données utiles.
 * @param raw texte brut du code, ou saisie manuelle
 * @param source origine de la lecture
 */
export function parseScan(raw: string, source: ScanSource): ScanReading {
    const lines = raw.split(LINE_BREAKS)
        .map((line) => line.replace(CONTROL_CHARS, ' ').trim())
        .filter((line) => line.length > 0);
    const words = lines.flatMap((line) => line.split(/\s+/));
    const candidates = unique([...preferredCandidates(lines), ...words])
        .filter((candidate) => candidate.length <= serialNumberMaxLength);
    const nsn = words.map(formatNsn).find((value) => value !== null) ?? null;

    return {
        raw,
        serialNumber: candidates[0] ?? '',
        candidates,
        macAddress: words.find((word) => MAC_PATTERN.test(word))?.toUpperCase() ?? null,
        nsn,
        typeHint: nsn ? typeByNsnClass[nsn.slice(0, 4)] ?? null : null,
        source,
    };
}
