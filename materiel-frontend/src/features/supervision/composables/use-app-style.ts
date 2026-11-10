/**
 * Apparence des applications dans la supervision : icône et couleur façon icônes iOS, selon le nom.
 * Une application inconnue est affichée par son initiale, sur fond gris.
 * TODO(question): les vrais noms viendront de l'agent du poste (ex. « chrome.exe ») : table à adapter.
 * Utilisé par : app-glyph.vue
 */
import type {IconName} from '../../../shared/components/app-icon.vue';

/** Couleur de l'icône (dégradés --app-* de style.css). */
type AppTone = 'blue' | 'indigo' | 'graphite' | 'sky' | 'teal' | 'neutral';

/** Icône et couleur d'une application. */
interface AppStyle {
    /** Icône, ou null si l'application n'en a pas (son initiale est affichée). */
    icon: IconName | null;
    tone: AppTone;
    letter: string;
}

/** Applications connues. « Masterisation » sert au journal (poste réinstallé, RG-28). */
const knownApps: Record<string, { icon: IconName; tone: AppTone }> = {
    'Navigateur web': {icon: 'globe', tone: 'blue'},
    'Environnement de développement': {icon: 'code', tone: 'indigo'},
    'Terminal': {icon: 'terminal', tone: 'graphite'},
    'Traitement de texte': {icon: 'document', tone: 'sky'},
    'Tableur': {icon: 'table', tone: 'teal'},
    'Masterisation': {icon: 'refresh', tone: 'neutral'},
};

/**
 * Icône et couleur d'une application, d'après son nom.
 * @param name nom affiché de l'application
 */
export function appStyle(name: string): AppStyle {
    const known = knownApps[name];
    return {icon: known?.icon ?? null, tone: known?.tone ?? 'neutral', letter: name.charAt(0).toUpperCase()};
}

export type {AppStyle};
