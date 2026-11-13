/**
 * Icône d'application reçue de l'API (agent Windows : image PNG en data URI).
 * Seules les images en base64 sont gardées : la valeur vient d'un poste du parc, elle n'est pas fiable.
 * Utilisé par : supervision.dto.ts, application.dto.ts
 */

/** Image en data URI : type image courant, puis base64 uniquement. */
const ICON_PATTERN = /^data:image\/(png|x-icon|vnd\.microsoft\.icon|jpeg|gif|webp);base64,[A-Za-z0-9+/]+=*$/;

/**
 * Renvoie l'icône si c'est une image en data URI valide, sinon null.
 * @param value icône reçue (null si l'agent n'en a pas envoyé)
 */
export function iconFromDto(value: string | null | undefined): string | null {
    return value && ICON_PATTERN.test(value) ? value : null;
}
