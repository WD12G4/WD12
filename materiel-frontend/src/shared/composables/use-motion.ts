/**
 * Effets de mouvement discrets : inclinaison 3D d'une carte au survol, et respect du réglage
 * « réduire les animations » du système (accessibilité macOS, iOS, Windows).
 * L'inclinaison écrit des variables CSS (--tilt-x, --tilt-y, --glow-x, --glow-y) lues par
 * style.css : le composant n'a aucune transformation à calculer.
 * Utilisé par : overview-tile.vue, stats-panel.vue, animated-number.vue
 */

/** Inclinaison maximale, en degrés : assez pour donner de la profondeur, pas assez pour gêner la lecture. */
const MAX_TILT_DEG = 5;

/** Vrai si l'utilisateur a demandé moins d'animations dans les réglages du système. */
export function prefersReducedMotion(): boolean {
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

/**
 * Incline la carte vers le pointeur et place le reflet sous lui.
 * Seulement à la souris ou au stylet : au doigt, la carte ne bouge pas.
 * @param event mouvement du pointeur sur la carte
 */
export function tiltTowardsPointer(event: PointerEvent): void {
    if (event.pointerType === 'touch' || prefersReducedMotion()) return;
    const card = event.currentTarget as HTMLElement;
    const rect = card.getBoundingClientRect();
    const x = (event.clientX - rect.left) / rect.width;
    const y = (event.clientY - rect.top) / rect.height;
    card.style.setProperty('--tilt-x', `${((0.5 - y) * 2 * MAX_TILT_DEG).toFixed(2)}deg`);
    card.style.setProperty('--tilt-y', `${((x - 0.5) * 2 * MAX_TILT_DEG).toFixed(2)}deg`);
    card.style.setProperty('--glow-x', `${(x * 100).toFixed(1)}%`);
    card.style.setProperty('--glow-y', `${(y * 100).toFixed(1)}%`);
}

/**
 * Remet la carte à plat quand le pointeur la quitte.
 * @param event sortie du pointeur
 */
export function resetTilt(event: PointerEvent): void {
    const card = event.currentTarget as HTMLElement;
    for (const name of ['--tilt-x', '--tilt-y', '--glow-x', '--glow-y']) card.style.removeProperty(name);
}
