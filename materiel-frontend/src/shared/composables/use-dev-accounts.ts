/**
 * OUTIL DE DÉMONSTRATION : comptes de démonstration (un par rôle), déclarés dans Keycloak
 * (docker/keycloak/gestion-parc-realm.json) et dans la base (docker/initdb/02-data.sql) avec le même
 * identifiant.
 * - Changement de compte sans se déconnecter : un administrateur connecté teste avec le rôle d'un autre
 *   compte (actAs de use-auth.ts, en-tête X-Demo-Account vérifié par le backend).
 * - Sinon (compte connecté non administrateur, connexion « par rôle ») : la page de Keycloak s'ouvre avec
 *   l'identifiant déjà rempli, il ne reste qu'à taper le mot de passe (le front ne connaît aucun mot de passe).
 * Chargé seulement en mode démonstration (user-card.vue, login-view.vue) : sinon le build le retire.
 * Utilisé par : dev-user-switch.vue, demo-login.vue
 */
import {roleLabels, type Role} from '../../models/user.model.ts';
import {router} from '../../router/index.ts';
import type {SelectOption} from './use-listbox.ts';
import {useAuth} from './use-auth.ts';
import {useFlashMessage} from './use-flash-message.ts';
import {useTeachingRoom} from './use-teaching-room.ts';

/** Compte de démonstration (identifiant identique dans Keycloak et dans la base). */
interface DemoAccount {
    username: string;
    role: Role;
}

//------------------ DATAS -----------------------------------
export const demoAccounts: readonly DemoAccount[] = [
    {username: 'admin', role: 'ADMIN'},
    {username: 'manager', role: 'MANAGER'},
    {username: 'user', role: 'USER'},
];

/** Options de la liste : un compte par rôle, désigné par son rôle (c'est lui qu'on veut tester). */
export const demoAccountOptions: SelectOption<string>[] = demoAccounts.map(account => ({
    value: account.username,
    label: roleLabels[account.role],
    group: 'Changer de compte',
}));

const {login, actAs, hasRole} = useAuth();
const {show} = useFlashMessage();
const {needsRoomChoice} = useTeachingRoom();

//------------------ FUNCTIONS -------------------------------
/**
 * Change de compte de démonstration sans se déconnecter (administrateur connecté). Si la page affichée est
 * interdite au nouveau rôle, retour au tableau de bord ; un formateur sans salle choisit d'abord sa salle.
 * Pour un compte connecté non administrateur, la page de Keycloak s'ouvre avec cet identifiant, puis ramène
 * sur la page en cours.
 * @param username identifiant du compte de démonstration
 * @returns vrai si le changement a réussi (ou si Keycloak s'ouvre)
 */
export async function switchAccount(username: string): Promise<boolean> {
    const account = demoAccounts.find(candidate => candidate.username === username);
    if (!account) return false;
    let switched: boolean;
    try {
        switched = await actAs(account.username);
    } catch {
        show(`Changement impossible vers le compte de démonstration « ${username} ».`, 'error');
        return false;
    }
    if (!switched) {
        await login(router.currentRoute.value.fullPath, account.username);
        return true;
    }
    const allowed = router.currentRoute.value.meta.roles.some(hasRole);
    if (!allowed) await router.replace({name: 'home'});
    // Formateur sans salle dans l'adresse : même passage obligatoire qu'après une vraie connexion.
    else if (needsRoomChoice.value) await router.replace({name: 'room-choice', query: {redirect: router.currentRoute.value.fullPath}});
    const role = roleLabels[account.role];
    show(allowed ? `Vous testez en tant que ${role} (« ${username} »).` : `Vous testez en tant que ${role} : page non autorisée, retour au tableau de bord.`);
    return true;
}
