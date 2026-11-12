/**
 * Modèle de la masterisation simulée (RG-28) : système, image de base et applications à installer
 * sur des postes simulés. Aucune vraie masterisation : voir simulation.services.mock.ts.
 * RG : RG-28
 */

//----------------- TYPES --------------------------------
/** Famille de système : décide du nom des processus et des applications disponibles. */
type OsFamily = 'windows' | 'linux';

/** Système proposé. */
interface OsOption {
    id: string;
    label: string;
    version: string;
    family: OsFamily;
    /** Taille de l'image, affichée (« 22 Go »). */
    imageSize: string;
}

/** Image de base : un profil qui pré-coche des applications. */
interface ImageProfile {
    id: string;
    label: string;
    description: string;
    /** Applications pré-cochées (celles absentes du système choisi sont ignorées). */
    appIds: string[];
}

/** Application installable, avec le nom de son processus selon le système (null : indisponible). */
interface AppOption {
    id: string;
    label: string;
    windows: string | null;
    linux: string | null;
}

/** Masterisation demandée dans l'assistant. */
interface ReimageConfig {
    osId: string;
    profileId: string;
    appIds: string[];
}

/** Configuration installée sur un poste simulé, en clair. */
interface InstalledConfig {
    system: string;
    image: string;
    /** Noms des applications installées. */
    apps: string[];
}

export type {OsFamily, OsOption, ImageProfile, AppOption, ReimageConfig, InstalledConfig};
