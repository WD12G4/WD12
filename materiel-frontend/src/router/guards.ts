/**
 * Garde de route : restaure la session, renvoie vers la connexion si besoin,
 * refuse les pages réservées à un rôle supérieur, et impose au formateur le choix de sa salle.
 * Utilisé par : router/index.ts
 * RG : RG-14 à RG-18 (droits par rôle), RG-31, RG-32
 *
 * Ce contrôle évite d'afficher des pages inutilisables : la vraie sécurité reste
 * assurée par le backend (@PreAuthorize).
 */
import type {RouteLocationNormalized, RouteLocationRaw, Router} from 'vue-router';
import {useAuth} from '../shared/composables/use-auth.ts';
import {useFlashMessage} from '../shared/composables/use-flash-message.ts';
import {TEACHING_ROOM_QUERY, useTeachingRoom} from '../shared/composables/use-teaching-room.ts';

const {isAuthenticated, restoreSession, hasRole, onSessionExpired} = useAuth();
const {show} = useFlashMessage();
const {isTrainer, roomIdFromQuery, setTeachingRoomId} = useTeachingRoom();

/**
 * Décide si la navigation est permise.
 * @param to route demandée
 * @param from route quittée (sa salle de cours est recopiée, voir checkTeachingRoom)
 * @returns true pour laisser passer, ou la route vers laquelle rediriger
 */
async function checkAccess(to: RouteLocationNormalized, from: RouteLocationNormalized): Promise<true | RouteLocationRaw> {
    // Au premier chargement, le jeton n'est pas encore en mémoire : on le récupère via le cookie.
    await restoreSession();

    if (to.meta.public) {
        // Un utilisateur déjà connecté n'a rien à faire sur la page de connexion.
        return isAuthenticated.value ? {name: 'home'} : true;
    }
    if (!isAuthenticated.value) {
        // La page demandée est gardée : après la connexion, Keycloak y ramène (navigation.ts, afterLoginHref).
        return to.fullPath === '/' ? {name: 'login'} : {name: 'login', query: {redirect: to.fullPath}};
    }
    if (!to.meta.roles.some(hasRole)) {
        show("Vous n'avez pas accès à cette page.", 'error');
        return {name: 'home'};
    }
    return checkTeachingRoom(to, from);
}

/**
 * Salle de cours du formateur, dans l'adresse (?salle=) : recopiée depuis la page quittée quand la
 * page demandée ne l'a pas (liens du menu, filtres). Sans salle : choix de la salle d'abord, puis
 * retour à la page demandée (?redirect=). Les autres rôles n'ont pas de salle à choisir.
 * @param to route demandée
 * @param from route quittée
 * @returns true pour laisser passer, ou la route vers laquelle rediriger
 */
function checkTeachingRoom(to: RouteLocationNormalized, from: RouteLocationNormalized): true | RouteLocationRaw {
    const roomId = roomIdFromQuery(to.query) ?? roomIdFromQuery(from.query);
    setTeachingRoomId(roomId);
    if (!isTrainer.value) return to.name === 'room-choice' ? {name: 'home'} : true;
    if (roomId === null) {
        if (to.name === 'room-choice') return true;
        return to.fullPath === '/' ? {name: 'room-choice'} : {name: 'room-choice', query: {redirect: to.fullPath}};
    }
    if (roomIdFromQuery(to.query) !== null) return true;
    return {path: to.path, query: {...to.query, [TEACHING_ROOM_QUERY]: String(roomId)}, hash: to.hash};
}

/** Dernière page demandée, même si sa navigation n'est pas finie : on y revient après reconnexion. */
let requestedPath = '/';
/** Vrai dès que la redirection vers la connexion est lancée (plusieurs requêtes peuvent échouer ensemble). */
let redirectingToLogin = false;

/**
 * Session expirée : rechargement complet de la page de connexion, et non une navigation interne.
 * Une navigation interne pouvait croiser celle en cours (transition de page) : l'adresse passait à
 * /connexion mais l'ancienne page restait affichée. Le rechargement efface aussi l'écran et les
 * données de la session terminée. Le message est affiché par login-view.vue (?session=expiree).
 */
function redirectToLogin(router: Router): void {
    if (redirectingToLogin) return;
    redirectingToLogin = true;
    window.location.assign(router.resolve({name: 'login', query: {redirect: requestedPath, session: 'expiree'}}).href);
}

/**
 * Installe la garde sur le router et la redirection en cas de session expirée.
 * Les redirections utilisent des objets { name } : jamais de chemin en chaîne.
 * @param router instance créée dans index.ts
 */
export function installGuards(router: Router): void {
    router.beforeEach((to, from) => {
        if (!to.meta.public) requestedPath = to.fullPath;
        return checkAccess(to, from);
    });
    onSessionExpired(() => redirectToLogin(router));
}
