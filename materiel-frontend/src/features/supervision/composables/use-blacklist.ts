/**
 * Liste noire des applications (RG-27, administrateur) : catalogue des applications vues sur les postes,
 * interdiction et autorisation. Le backend ferme une application interdite dès qu'un agent la remonte,
 * et enregistre l'arrêt dans le journal.
 * État partagé au niveau du module : l'onglet « Liste noire » et le bouton « Interdire » du panneau
 * d'un poste voient le même catalogue.
 * Utilisé par : blacklist-panel.vue, use-supervision-actions.ts
 * RG : RG-27
 */
import {computed, ref} from 'vue';
import type {Application} from '../../../models/application.model.ts';
import {applicationsServices} from '../../../services/applications.services.ts';

//------------------ DATAS -----------------------------------
const applications = ref<Application[]>([]);

//------------------ FUNCTIONS -------------------------------
/** Recharge le catalogue. */
async function loadApplications(): Promise<void> {
    applications.value = await applicationsServices.findAll();
}

/**
 * Interdit ou autorise une application, puis met à jour le catalogue.
 * @param app application
 * @param blacklisted vrai pour l'interdire
 */
async function setBlacklisted(app: Application, blacklisted: boolean): Promise<void> {
    const updated = await applicationsServices.setBlacklisted(app.id, blacklisted);
    applications.value = applications.value.map(item => (item.id === updated.id ? updated : item));
}

/**
 * Interdit l'application d'un processus, retrouvée par son nom (le backend l'enregistre sans tenir
 * compte de la casse). Le catalogue est relu si elle n'y est pas encore.
 * @param processName nom du processus remonté par l'agent
 */
async function forbid(processName: string): Promise<void> {
    const find = () => applications.value.find(app => app.name.toLowerCase() === processName.trim().toLowerCase());
    if (!find()) await loadApplications();
    const app = find();
    if (!app) throw new Error(`« ${processName} » n'est pas encore dans le catalogue des applications.`);
    await setBlacklisted(app, true);
}

/**
 * Accès au catalogue et à la liste noire.
 * @returns le catalogue (interdites d'abord), le nombre d'interdites et les actions
 */
export function useBlacklist() {
    const sorted = computed(() => [...applications.value].sort((a, b) =>
        Number(b.blacklisted) - Number(a.blacklisted) || a.displayName.localeCompare(b.displayName)));
    const forbiddenCount = computed(() => applications.value.filter(app => app.blacklisted).length);
    return {applications: sorted, forbiddenCount, loadApplications, setBlacklisted, forbid};
}
