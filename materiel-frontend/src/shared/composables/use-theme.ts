/**
 * Thème de l'application (esprit « Liquid Glass ») : Givre, Nuit, Aurore ou Lagon.
 * Le thème est l'attribut data-theme de <html> ; style.css définit les couleurs de chacun.
 * Au chargement, index.html choisit Givre ou Nuit selon le réglage du système.
 * Le choix reste EN MÉMOIRE (pas de stockage navigateur, cadre du projet) : un rechargement
 * revient au thème du système.
 * Utilisé par : theme-switch.vue
 */
import {ref} from 'vue';

//------------------ VALEURS ---------------------------------
/** Thèmes proposés, dans l'ordre d'affichage. */
const themes = [
    {id: 'givre', label: 'Givre', description: 'Verre clair, reflets bleus'},
    {id: 'nuit', label: 'Nuit', description: 'Verre fumé sur fond noir'},
    {id: 'aurore', label: 'Aurore', description: 'Verre teinté, aurores violettes'},
    {id: 'lagon', label: 'Lagon', description: 'Verre aqua, lumière de lagon'},
] as const;

/** Identifiant d'un thème. */
type ThemeId = (typeof themes)[number]['id'];

//------------------ DATAS -----------------------------------
const isTheme = (value: string | undefined): value is ThemeId => themes.some(theme => theme.id === value);
const initial = document.documentElement.dataset.theme;
const currentTheme = ref<ThemeId>(isTheme(initial) ? initial : 'givre');

//------------------ FUNCTIONS -------------------------------
/**
 * Applique un thème à toute la page.
 * @param id thème choisi
 */
function setTheme(id: ThemeId): void {
    currentTheme.value = id;
    document.documentElement.dataset.theme = id;
}

/**
 * Accès au thème partagé.
 * @returns la liste des thèmes, le thème courant et setTheme()
 */
export function useTheme() {
    return {themes, currentTheme, setTheme};
}

export type {ThemeId};
