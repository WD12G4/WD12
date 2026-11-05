/**
 * Fonctions pures sur les dates, reprises de materiel-frontend (formatage français, échéance).
 * Utilisé par : features/scan, features/inventory
 * RG : RG-05 (date de fin non passée)
 */

/** Nombre de millisecondes dans un jour. */
const DAY_MS = 86_400_000;

/** Délai à partir duquel un matériel est considéré « bientôt expiré ». */
export const EXPIRY_WARNING_DAYS = 30;

/**
 * Formate une date en français (ex. 03/11/2026).
 * @param date date à formater
 * @param emptyLabel texte affiché si la date est absente
 */
export function formatDate(date: Date | null, emptyLabel = 'Aucune'): string {
    return date ? date.toLocaleDateString('fr-FR') : emptyLabel;
}

/**
 * Formate une heure en français (ex. 14:05).
 * @param date date à formater
 */
export function formatTime(date: Date): string {
    return date.toLocaleTimeString('fr-FR', {hour: '2-digit', minute: '2-digit'});
}

/**
 * Nombre de jours calendaires entre aujourd'hui et une date (négatif si elle est passée).
 * @param date date d'échéance
 * @param today date du jour (paramètre pour pouvoir tester)
 */
export function daysUntil(date: Date, today = new Date()): number {
    const dueDay = Date.UTC(date.getFullYear(), date.getMonth(), date.getDate());
    const currentDay = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
    return (dueDay - currentDay) / DAY_MS;
}

/**
 * Date du jour au format 'AAAA-MM-JJ', pour l'attribut min d'un <input type="date"> (RG-05).
 */
export function todayIsoDate(): string {
    const today = new Date();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${today.getFullYear()}-${month}-${day}`;
}
