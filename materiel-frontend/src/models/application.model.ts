/**
 * Modèle d'une application du catalogue : chaque processus remonté par un agent y est enregistré par le
 * backend (nom du processus, icône), et l'administrateur peut l'interdire (liste noire).
 * Une application interdite est fermée automatiquement sur les postes dès qu'elle est détectée.
 * RG : RG-27 (liste noire)
 */

/** Application du catalogue. */
interface Application {
    id: number;
    /** Version pour le verrouillage optimiste, renvoyée par l'API. */
    version: number;
    /** Nom du processus (ex. « chrome.exe »), unique. */
    name: string;
    /** Nom lisible (ex. « Chrome »), ou le nom du processus à défaut. */
    displayName: string;
    /** Icône (image PNG en data URI, envoyée par les agents Windows), ou null. */
    icon: string | null;
    /** Vrai si l'application est interdite. */
    blacklisted: boolean;
}

export type {Application};
