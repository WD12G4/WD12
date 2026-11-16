/**
 * Accord d'un mot avec un nombre, à la place des « (s) » : « 1 poste », « 3 postes ».
 * En français, 0 et 1 restent au singulier.
 * Utilisé par : features/home, features/materials, features/buildings, features/supervision.
 */

/**
 * Mot accordé avec un nombre.
 * @param count nombre
 * @param singular mot au singulier
 * @param pluralForm mot au pluriel (par défaut, le singulier suivi d'un « s »)
 */
export function plural(count: number, singular: string, pluralForm = `${singular}s`): string {
    return count > 1 ? pluralForm : singular;
}

/**
 * Nombre suivi du mot accordé, ex. « 3 matériels ».
 * @param count nombre
 * @param singular mot au singulier
 * @param pluralForm mot au pluriel (par défaut, le singulier suivi d'un « s »)
 */
export function countLabel(count: number, singular: string, pluralForm?: string): string {
    return `${count} ${plural(count, singular, pluralForm)}`;
}
