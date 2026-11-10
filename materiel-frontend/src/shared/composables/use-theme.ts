/**
 * Thème visuel, appliqué par l'attribut data-theme de <html> (règles dans style.css) :
 * - « verre » (par défaut sur cette branche d'essai) : barre de menu flottante en haut, surfaces en
 *   verre dépoli sur un fond clair aux reflets colorés, boutons en pilule, accent bleu (esprit Apple) ;
 * - « classique » : le thème de frontBase_ESL (barre latérale, accent violet).
 * Le choix n'est pas mémorisé (le cadre interdit le stockage dans le navigateur) : ?theme=classique dans
 * l'adresse ouvre directement le thème classique, ce qui permet aussi de partager le lien.
 * Utilisé par : theme-switch.vue, use-nav-indicator.ts
 */
import {ref, watch} from 'vue';

export type ThemeName = 'verre' | 'classique';

/** Thème demandé dans l'adresse (?theme=classique), sinon le verre. */
function initialTheme(): ThemeName {
    return new URLSearchParams(window.location.search).get('theme') === 'classique' ? 'classique' : 'verre';
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
