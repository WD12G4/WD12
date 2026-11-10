/**
 * Fonctions pures sur les dates : formatage français et calcul d'échéance.
 * Inspiré de use-due-date.ts du projet du cours.
 * Utilisé par : features/materials, features/home, features/reports, features/supervision.
 * RG : RG-24 (alerte d'expiration groupée), RG-05 (date de fin non passée)
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
 * Formate une date et une heure en français (ex. 03/11/2026 14:05).
 * @param date date à formater
 */
export function formatDateTime(date: Date): string {
    return date.toLocaleString('fr-FR', {dateStyle: 'short', timeStyle: 'short'});
}

/**
 * Nombre de jours calendaires entre aujourd'hui et une date (négatif si elle est passée).
 * On compare des jours UTC pour ignorer l'heure et les changements d'heure, comme dans le cours.
 * @param date date d'échéance
 * @param today date du jour (paramètre pour pouvoir tester)
 */
export function daysUntil(date: Date, today = new Date()): number {
    const dueDay = Date.UTC(date.getFullYear(), date.getMonth(), date.getDate());
    const currentDay = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());
    return (dueDay - currentDay) / DAY_MS;
}

/**
 * Indique si une date arrive à échéance dans les « days » prochains jours (passée incluse).
 * @param date date de fin de disponibilité, ou null
 * @param days horizon en jours (EXPIRY_WARNING_DAYS par défaut)
 */
export function isExpiringSoon(date: Date | null, days: number = EXPIRY_WARNING_DAYS): boolean {
    return date !== null && daysUntil(date) <= days;
}

/**
 * Convertit une Date en 'AAAA-MM-JJ' (heure locale), pour remplir un <input type="date">.
 * toISOString() n'est pas utilisé : il convertit en UTC et peut donner la veille.
 * @param date date à convertir
 */
export function toIsoDate(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
}

/**
 * Date du jour au format 'AAAA-MM-JJ', pour l'attribut min d'un <input type="date"> (RG-05).
 */
export function todayIsoDate(): string {
    return toIsoDate(new Date());
}

/**
 * Heure seule (ex. 10:12), pour le journal de supervision.
 * @param date date à formater
 */
export function formatTime(date: Date): string {
    return date.toLocaleTimeString('fr-FR', {hour: '2-digit', minute: '2-digit'});
}

/**
 * Temps écoulé depuis une date, en clair (ex. « depuis 20 min », « depuis 1 h 05 »).
 * @param date début (lancement d'une application)
 * @param now instant de référence (paramètre pour pouvoir tester)
 */
export function formatElapsed(date: Date, now = new Date()): string {
    const minutes = Math.max(0, Math.floor((now.getTime() - date.getTime()) / 60_000));
    if (minutes < 1) return 'à l’instant';
    if (minutes < 60) return `depuis ${minutes} min`;
    const rest = minutes % 60;
    return `depuis ${Math.floor(minutes / 60)} h${rest > 0 ? ` ${String(rest).padStart(2, '0')}` : ''}`;
}

/**
 * Libellé d'un jour : « Aujourd'hui », « Hier », sinon la date (ex. « lundi 9 novembre »).
 * @param date jour à décrire
 * @param today date du jour (paramètre pour pouvoir tester)
 */
export function dayLabel(date: Date, today = new Date()): string {
    const days = daysUntil(date, today);
    if (days === 0) return 'Aujourd’hui';
    if (days === -1) return 'Hier';
    return date.toLocaleDateString('fr-FR', {weekday: 'long', day: 'numeric', month: 'long'});
}
