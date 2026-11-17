/**
 * OUTIL DE DÉMONSTRATION : compte de démonstration avec lequel un administrateur teste les autres rôles sans
 * se déconnecter de Keycloak. Le compte choisi part dans l'en-tête X-Demo-Account (client HTTP, branché par
 * use-auth.ts) ; le backend ne l'accepte que pour un administrateur.
 * Il est gardé dans sessionStorage (onglet en cours) pour survivre à un rechargement : ce n'est qu'un
 * identifiant, pas un secret. C'est le seul stockage navigateur du front : il disparaît avec cet outil.
 * Utilisé par : use-auth.ts, use-keycloak-session.ts
 * RG : RG-48 (démonstration : changer de rôle sans se reconnecter)
 */
import {ref} from 'vue';
import type {User} from '../../models/user.model.ts';
import {authServices} from '../../services/auth.services.ts';

//------------------ DATAS -----------------------------------
/** Clé sessionStorage du compte de démonstration choisi. */
const DEMO_ACCOUNT_KEY = 'gestion-parc.demo-account';
/** Compte de démonstration avec lequel l'administrateur teste, ou null (son propre compte). */
const demoAccount = ref<string | null>(readDemoAccount());

//------------------ FUNCTIONS -------------------------------
/** Compte de démonstration gardé pour l'onglet (sessionStorage peut être indisponible : null). */
function readDemoAccount(): string | null {
    try {
        return sessionStorage.getItem(DEMO_ACCOUNT_KEY);
    } catch {
        return null;
    }
}

/**
 * Retient le compte de démonstration pour l'onglet, ou l'oublie (null).
 * @param username compte de démonstration, ou null pour revenir à son propre compte
 */
function saveDemoAccount(username: string | null): void {
    demoAccount.value = username;
    try {
        if (username) sessionStorage.setItem(DEMO_ACCOUNT_KEY, username);
        else sessionStorage.removeItem(DEMO_ACCOUNT_KEY);
    } catch {
        // Stockage indisponible : le choix vaut jusqu'au prochain rechargement.
    }
}

/**
 * Demande au backend le profil d'un compte de démonstration (en-tête X-Demo-Account). Seul un administrateur
 * peut le faire : pour un autre compte, le backend ignore la demande et renvoie son propre profil, et le
 * choix est annulé.
 * @param username compte de démonstration
 * @param signedInAs compte de la connexion Keycloak : le choisir revient au compte réel
 * @returns le profil obtenu si c'est bien celui demandé, sinon null
 * @throws ApiError si le profil ne peut pas être chargé (réseau…)
 */
async function requestDemoProfile(username: string, signedInAs: string | undefined): Promise<User | null> {
    const previous = demoAccount.value;
    saveDemoAccount(username === signedInAs ? null : username);
    try {
        const profile = await authServices.me();
        if (profile.username === username) return profile;
        saveDemoAccount(previous);
        return null;
    } catch (error) {
        saveDemoAccount(previous);
        throw error;
    }
}

/**
 * Accès au compte de démonstration, partagé par toute l'application.
 * @returns le compte choisi, son enregistrement et la demande du profil correspondant
 */
export function useDemoAccount() {
    return {demoAccount, saveDemoAccount, requestDemoProfile};
}
