// MAQUETTE : catalogue de la masterisation simulée (pas de serveur de déploiement PXE/WDS/FOG).
/**
 * Systèmes, images de base et applications proposés par l'assistant de masterisation, et lecture du
 * catalogue (applications d'un système, résumé d'une configuration).
 * Utilisé par : simulation.services.mock.ts, features/supervision (assistant de masterisation)
 * RG : RG-28
 */
import type {AppOption, ImageProfile, InstalledConfig, OsOption, ReimageConfig} from '../models/reimage.model.ts';

//------------------ DATAS -----------------------------------
export const osOptions: OsOption[] = [
    {id: 'windows-11', label: 'Windows 11 Éducation', version: '24H2', family: 'windows', imageSize: '22 Go'},
    {id: 'ubuntu-24', label: 'Ubuntu', version: '24.04 LTS', family: 'linux', imageSize: '6 Go'},
    {id: 'debian-12', label: 'Debian', version: '12.8', family: 'linux', imageSize: '4 Go'},
];

export const imageProfiles: ImageProfile[] = [
    {id: 'office', label: 'Bureautique', description: 'Navigateur, traitement de texte, tableur',
        appIds: ['firefox', 'word', 'excel', 'libreoffice']},
    {id: 'dev', label: 'Développement', description: 'Éditeurs de code, Java, Python, Docker',
        appIds: ['firefox', 'vscode', 'intellij', 'terminal', 'git', 'jdk', 'python', 'docker']},
    {id: 'network', label: 'Réseau et sécurité', description: 'Analyse réseau, simulation Cisco, SSH',
        appIds: ['firefox', 'terminal', 'wireshark', 'packettracer', 'putty']},
    {id: 'blank', label: 'Système seul', description: 'Aucune application ajoutée', appIds: []},
];

export const appOptions: AppOption[] = [
    {id: 'firefox', label: 'Firefox', windows: 'firefox.exe', linux: 'firefox'},
    {id: 'chrome', label: 'Google Chrome', windows: 'chrome.exe', linux: 'chrome'},
    {id: 'word', label: 'Microsoft Word', windows: 'WINWORD.EXE', linux: null},
    {id: 'excel', label: 'Microsoft Excel', windows: 'EXCEL.EXE', linux: null},
    {id: 'libreoffice', label: 'LibreOffice', windows: 'soffice.exe', linux: 'soffice.bin'},
    {id: 'vscode', label: 'Visual Studio Code', windows: 'Code.exe', linux: 'code'},
    {id: 'intellij', label: 'IntelliJ IDEA', windows: 'idea64.exe', linux: 'idea'},
    {id: 'terminal', label: 'Terminal', windows: 'WindowsTerminal.exe', linux: 'gnome-terminal-server'},
    {id: 'git', label: 'Git', windows: 'git-bash.exe', linux: 'gitk'},
    {id: 'jdk', label: 'Java (JDK 21)', windows: 'java.exe', linux: 'java'},
    {id: 'python', label: 'Python 3.12', windows: 'python.exe', linux: 'python3'},
    {id: 'docker', label: 'Docker', windows: 'Docker Desktop.exe', linux: 'docker'},
    {id: 'wireshark', label: 'Wireshark', windows: 'Wireshark.exe', linux: 'wireshark'},
    {id: 'packettracer', label: 'Cisco Packet Tracer', windows: 'PacketTracer.exe', linux: 'PacketTracer'},
    {id: 'putty', label: 'PuTTY', windows: 'putty.exe', linux: null},
    {id: 'notepadpp', label: 'Notepad++', windows: 'notepad++.exe', linux: null},
    {id: 'gedit', label: 'Éditeur de texte', windows: null, linux: 'gedit'},
];

//------------------ FUNCTIONS -------------------------------
/** Système d'après son identifiant (le premier si inconnu). */
export function findOs(osId: string): OsOption {
    return osOptions.find(os => os.id === osId) ?? osOptions[0];
}

/** Libellé complet d'un système (« Ubuntu 24.04 LTS »). */
export function osLabel(osId: string): string {
    const os = findOs(osId);
    return `${os.label} ${os.version}`;
}

/** Applications disponibles sur un système. */
export function appsForOs(osId: string): AppOption[] {
    const family = findOs(osId).family;
    return appOptions.filter(app => app[family] !== null);
}

/** Nom du processus d'une application sur un système, ou null si elle n'y existe pas. */
export function processName(appId: string, osId: string): string | null {
    return appOptions.find(app => app.id === appId)?.[findOs(osId).family] ?? null;
}

/** Configuration d'une image de base sur un système : ses applications disponibles sur ce système. */
export function configFor(osId: string, profileId: string): ReimageConfig {
    const available = new Set(appsForOs(osId).map(app => app.id));
    const appIds = (imageProfiles.find(profile => profile.id === profileId)?.appIds ?? []).filter(id => available.has(id));
    return {osId, profileId, appIds};
}

/** Système, image et applications d'une configuration, en clair (panneau d'un poste simulé). */
export function describeConfig(config: ReimageConfig): InstalledConfig {
    const image = imageProfiles.find(profile => profile.id === config.profileId)?.label ?? 'Image personnalisée';
    const apps = config.appIds.map(id => appOptions.find(app => app.id === id)?.label ?? id);
    return {system: osLabel(config.osId), image, apps};
}

/** Résumé d'une configuration (« Ubuntu 24.04 LTS · Développement · 7 applications »). */
export function configSummary(config: ReimageConfig): string {
    const profile = imageProfiles.find(item => item.id === config.profileId)?.label ?? 'Image personnalisée';
    const count = config.appIds.length;
    return `${osLabel(config.osId)} · ${profile} · ${count} application${count > 1 ? 's' : ''}`;
}
