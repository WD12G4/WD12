/**
 * Dates avec heure échangées avec l'API (LocalDateTime côté backend, sans fuseau horaire).
 * Convention : le backend tourne en UTC (conteneur Docker, CI) ; ses dates sont donc lues en UTC,
 * puis affichées à l'heure locale du navigateur.
 * TODO(question): à confirmer avec l'équipe backend (Instant ou OffsetDateTime lèveraient le doute ;
 * en développement sur Mac, lancer la JVM en UTC : -Duser.timezone=UTC).
 * Utilisé par : report.dto.ts, kill-event.dto.ts, supervision.dto.ts
 */

/**
 * Convertit une date-heure de l'API (« 2026-11-10T14:53:42.098 ») en Date.
 * @param value date-heure reçue, en UTC sans indication de fuseau
 */
export function parseApiDateTime(value: string): Date {
    return new Date(/[zZ]|[+-]\d\d:\d\d$/.test(value) ? value : `${value}Z`);
}
