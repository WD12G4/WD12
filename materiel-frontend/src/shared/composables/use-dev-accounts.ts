/**
 * OUTIL DE DÉMONSTRATION : comptes de démonstration (un par rôle), déclarés dans Keycloak
 * (docker/keycloak/gestion-parc-realm.json) et dans la base (docker/initdb/02-data.sql) avec le même
 * identifiant. Changement de compte et connexion « par rôle » : la page de Keycloak s'ouvre avec
 * l'identifiant déjà rempli, il ne reste qu'à taper le mot de passe (le front ne connaît aucun mot de passe).
 * Chargé seulement en mode démonstration (user-card.vue, login-view.vue) : sinon le build le retire.
 * Utilisé par : dev-user-switch.vue, demo-login.vue
 */
import {roleLabels, type Role} from '../../models/user.model.ts';
import {router} from '../../router/index.ts';
import type {SelectOption} from './use-listbox.ts';
import {useAuth} from './use-auth.ts';

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

const {login} = useAuth();

//------------------ FUNCTIONS -------------------------------
/**
 * Change de compte de démonstration : la page de Keycloak s'ouvre avec cet identifiant, puis ramène
 * sur la page en cours. Si elle est interdite au nouveau rôle, la garde renvoie au tableau de bord ;
 * un formateur sans salle choisit d'abord sa salle (guards.ts).
 * @param username identifiant du compte de démonstration
 * @returns faux si le compte est inconnu (sinon la page de l'application est quittée)
 */
export async function switchAccount(username: string): Promise<boolean> {
    const account = demoAccounts.find(candidate => candidate.username === username);
    if (!account) return false;
    await login(router.currentRoute.value.fullPath, account.username);
    return true;
}
