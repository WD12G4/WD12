/**
 * Apparence de l'application (esprit « Liquid Glass ») : thème (Givre, Nuit, Aurore, Lagon, Sauge),
 * disposition du menu (barre du haut ou latérale) et affichage du contenu (bloc central ou pleine
 * largeur, sans cadre), combinables. Attributs de <html> : data-theme, data-layout, data-frame ;
 * style.css les décrit.
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
    {id: 'sauge', label: 'Sauge', description: 'Verre dépoli, verts et tons de terre'},
] as const;

/** Dispositions du menu. */
const layouts = [
    {id: 'top', label: 'Barre du haut'},
    {id: 'sidebar', label: 'Barre latérale'},
] as const;

/** Affichage du contenu : dans un bloc central en verre, ou en pleine largeur sans cadre. */
const frames = [
    {id: 'card', label: 'Bloc central'},
    {id: 'open', label: 'Pleine largeur'},
] as const;

/** Identifiant d'un thème. */
type ThemeId = (typeof themes)[number]['id'];
/** Identifiant d'une disposition. */
type LayoutId = (typeof layouts)[number]['id'];
/** Identifiant d'un affichage du contenu. */
type FrameId = (typeof frames)[number]['id'];

//------------------ DATAS -----------------------------------
const isTheme = (value: string | undefined): value is ThemeId => themes.some(theme => theme.id === value);
const initial = document.documentElement.dataset.theme;
const currentTheme = ref<ThemeId>(isTheme(initial) ? initial : 'givre');
const currentLayout = ref<LayoutId>('top');
const currentFrame = ref<FrameId>('card');

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
 * Applique une disposition du menu à toute la page.
 * @param id disposition choisie
 */
function setLayout(id: LayoutId): void {
    currentLayout.value = id;
    document.documentElement.dataset.layout = id;
    // Les éléments placés selon le menu (pastille de la rubrique, listes ouvertes) se replacent.
    window.dispatchEvent(new Event('resize'));
}

/**
 * Applique un affichage du contenu à toute la page.
 * @param id affichage choisi
 */
function setFrame(id: FrameId): void {
    currentFrame.value = id;
    document.documentElement.dataset.frame = id;
}

/**
 * Accès à l'apparence partagée.
 * @returns choix proposés, choix courants, setTheme(), setLayout() et setFrame()
 */
export function useTheme() {
    return {themes, layouts, frames, currentTheme, currentLayout, currentFrame, setTheme, setLayout, setFrame};
}

export type {ThemeId, LayoutId, FrameId};
