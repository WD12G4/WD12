/**
 * OUTIL DE DÉMONSTRATION : comptes de démonstration créés par docker/initdb/02-data.sql (base locale
 * et VM), changement de compte sans se déconnecter, connexion en un clic.
 * Chargé seulement en mode démonstration (user-card.vue, mindef-connect-view.vue) : sinon le build le retire.
 * Utilisé par : dev-user-switch.vue, demo-login.vue
 */
import {roleLabels, type Role} from '../../models/user.model.ts';
import {router} from '../../router/index.ts';
import type {SelectOption} from './use-listbox.ts';
import {useAuth} from './use-auth.ts';
import {useFlashMessage} from './use-flash-message.ts';
import {useTeachingRoom} from './use-teaching-room.ts';

/** Compte de démonstration (mots de passe de docker/initdb/02-data.sql). */
interface DemoAccount {
    username: string;
    password: string;
    role: Role;
}

//------------------ DATAS -----------------------------------
export const demoAccounts: readonly DemoAccount[] = [
    {username: 'admin', password: 'admin123', role: 'ADMIN'},
    {username: 'manager', password: 'manager123', role: 'MANAGER'},
    {username: 'user', password: 'user123', role: 'USER'},
];

/** Options de la liste : un compte par rôle, désigné par son rôle (c'est lui qu'on veut tester). */
export const demoAccountOptions: SelectOption<string>[] = demoAccounts.map(account => ({
    value: account.username,
    label: roleLabels[account.role],
    group: 'Changer de compte',
}));

const {login, hasRole} = useAuth();
const {show} = useFlashMessage();
const {needsRoomChoice} = useTeachingRoom();

//------------------ FUNCTIONS -------------------------------
/**
 * Se connecte avec un autre compte de démonstration. La session en cours n'est remplacée qu'en cas
 * de succès. Si la page affichée est interdite au nouveau rôle, retour au tableau de bord ; un formateur
 * sans salle choisit d'abord sa salle.
 * @param username identifiant du compte de démonstration
 * @returns vrai si le changement a réussi
 */
export async function switchAccount(username: string): Promise<boolean> {
    const account = demoAccounts.find(candidate => candidate.username === username);
    if (!account) return false;
    try {
        await login(account.username, account.password);
    } catch {
        show(`Connexion impossible avec le compte de démonstration « ${username} ».`, 'error');
        return false;
    }
    const allowed = router.currentRoute.value.meta.roles.some(hasRole);
    if (!allowed) await router.replace({name: 'home'});
    // Formateur sans salle dans l'adresse : même passage obligatoire qu'après une vraie connexion.
    else if (needsRoomChoice.value) await router.replace({name: 'room-choice', query: {redirect: router.currentRoute.value.fullPath}});
    const role = roleLabels[account.role];
    show(allowed ? `Vous testez en tant que ${role} (« ${username} »).` : `Vous testez en tant que ${role} : page non autorisée, retour au tableau de bord.`);
    return true;
}
