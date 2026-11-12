/**
 * Liens du menu principal, filtrés selon le rôle de l'utilisateur connecté.
 * Utilisé par : App.vue, app-nav.vue
 * RG : RG-16, RG-17, RG-31, RG-32 (un lien n'apparaît que si la page est accessible),
 * RG-23 (« Remontées » porte le nombre de remontées à traiter du gestionnaire)
 */
import {computed} from 'vue';
import type {Role} from '../../models/user.model.ts';
import type {RouteNames, RouteTarget} from '../../router/route-types.ts';
import type {IconName} from '../components/app-icon.vue';
import {useAuth} from './use-auth.ts';
import {useUnreadReports} from '../../features/reports/composables/use-unread-reports.ts';

/** Lien du menu. */
interface NavLink {
    to: RouteTarget;
    label: string;
    /** Icône affichée devant le libellé. */
    icon: IconName;
    /** Rôle minimal pour voir le lien (héritage des rôles, RG-15). */
    minRole: Role;
    /** Pages de la rubrique : le lien reste surligné sur ses fiches et formulaires. */
    section: readonly RouteNames[];
    /** Lien d'action : bouton mis en avant à droite de la barre du haut. */
    primary?: boolean;
    /** Nombre affiché en pastille rouge à côté du libellé (absent ou 0 : pas de pastille). */
    count?: number;
}

/** Tous les liens, dans l'ordre d'affichage. Les rôles reprennent meta.roles des routes. */
const allLinks: readonly NavLink[] = [
    {to: {name: 'home'}, label: 'Tableau de bord', icon: 'dashboard', minRole: 'USER', section: ['home']},
    {
        to: {name: 'materials'}, label: 'Matériel', icon: 'material', minRole: 'USER',
        section: ['materials', 'material-new', 'material-detail', 'material-edit'],
    },
    {to: {name: 'rooms'}, label: 'Salles', icon: 'room', minRole: 'USER', section: ['rooms', 'room-detail']},
    {to: {name: 'buildings'}, label: 'Bâtiments', icon: 'building', minRole: 'USER', section: ['buildings']},
    {
        to: {name: 'report-new'}, label: 'Signaler un problème', icon: 'alert', minRole: 'USER',
        section: ['report-new'], primary: true,
    },
    {to: {name: 'reports'}, label: 'Remontées', icon: 'inbox', minRole: 'USER', section: ['reports', 'report-document']},
    {to: {name: 'supervision'}, label: 'Supervision', icon: 'activity', minRole: 'USER', section: ['supervision']},
    {to: {name: 'users'}, label: 'Comptes', icon: 'users', minRole: 'ADMIN', section: ['users']},
];

/**
 * Liens visibles pour l'utilisateur connecté.
 * @returns un computed avec les liens autorisés (vide si personne n'est connecté), compteurs compris
 */
export function useNavLinks() {
    const {hasRole} = useAuth();
    const {unreadCount} = useUnreadReports();
    const visibleLinks = computed(() => allLinks
        .filter(link => hasRole(link.minRole))
        .map(link => (link.to.name === 'reports' ? {...link, count: unreadCount.value} : link)));
    return {visibleLinks};
}

export type {NavLink};
