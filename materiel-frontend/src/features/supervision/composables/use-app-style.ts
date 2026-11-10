/**
 * Apparence des applications dans la supervision : icône et couleur façon icônes iOS, d'après le
 * nom remonté par l'agent : processus (« chrome.exe », « code »…) ou, sous macOS, application
 * (« Google Chrome », « Visual Studio Code »…). Une application inconnue est
 * affichée par son initiale, sur fond gris.
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

/** Familles d'applications reconnues, par nom de processus (sans « .exe », en minuscules). */
const families: { icon: IconName; tone: AppTone; names: string[] }[] = [
    {icon: 'globe', tone: 'blue', names: ['chrome', 'chromium', 'firefox', 'msedge', 'brave', 'opera', 'safari', 'google chrome', 'microsoft edge', 'brave browser', 'arc']},
    {icon: 'code', tone: 'indigo', names: ['code', 'idea64', 'idea', 'pycharm64', 'pycharm', 'eclipse', 'devenv', 'rider64', 'studio64', 'netbeans64',
        'visual studio code', 'intellij idea', 'intellij idea ce', 'pycharm ce', 'webstorm', 'xcode', 'android studio']},
    {icon: 'terminal', tone: 'graphite', names: ['cmd', 'powershell', 'pwsh', 'windowsterminal', 'bash', 'zsh', 'sh', 'gnome-terminal-server', 'konsole', 'xterm',
        'terminal', 'iterm2', 'iterm', 'warp']},
    {icon: 'document', tone: 'sky', names: ['winword', 'soffice', 'soffice.bin', 'notepad', 'notepad++', 'gedit', 'acrord32',
        'microsoft word', 'pages', 'textedit', 'libreoffice']},
    {icon: 'table', tone: 'teal', names: ['excel', 'microsoft excel', 'numbers']},
    // Journal : poste réinstallé (RG-28)
    {icon: 'refresh', tone: 'neutral', names: ['masterisation']},
];

/**
 * Icône et couleur d'une application, d'après le nom de son processus.
 * @param name nom du processus (ou « Masterisation » pour le journal)
 */
export function appStyle(name: string): AppStyle {
    const key = name.trim().toLowerCase().replace(/\.exe$/, '');
    const family = families.find(candidate => candidate.names.includes(key));
    return {icon: family?.icon ?? null, tone: family?.tone ?? 'neutral', letter: name.trim().charAt(0).toUpperCase()};
}

export type {AppStyle};
