/**
 * Thème visuel, appliqué par l'attribut data-theme de <html> (règles dans style.css) :
 * - « classique » : le thème actuel ;
 * - « tactique » : essai inspiré des sites industriels, à montrer au client (accent vert vif réservé
 *   aux actions, coins coupés, étiquettes et boutons en police monospace).
 * Le choix n'est pas mémorisé (le cadre interdit le stockage dans le navigateur) : ?theme=tactique dans l'adresse
 * l'active dès l'ouverture, ce qui permet aussi de partager le lien.
 * Utilisé par : shared/components/app-nav.vue
 */
import {ref, watch} from 'vue';

export type ThemeName = 'classique' | 'tactique';

/** Thème demandé dans l'adresse (?theme=tactique), sinon le classique. */
function initialTheme(): ThemeName {
    return new URLSearchParams(window.location.search).get('theme') === 'tactique' ? 'tactique' : 'classique';
}

/** Thème courant, partagé par toute l'application. */
const theme = ref<ThemeName>(initialTheme());

watch(theme, name => {
    document.documentElement.dataset.theme = name;
}, {immediate: true});

/**
 * Accès au thème courant (modifiable : le changer l'applique aussitôt).
 * @returns theme
 */
export function useTheme() {
    return {theme};
}
