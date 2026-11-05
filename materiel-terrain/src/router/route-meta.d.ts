/**
 * Typage des métadonnées de route (`meta`), lues par la garde de guards.ts.
 * Rendre `roles` obligatoire force chaque route à déclarer qui peut y accéder.
 */
import 'vue-router';
import type {Role} from '../models/user.model.ts';

declare module 'vue-router' {
    interface RouteMeta {
        /**
         * Rôles autorisés. Grâce à l'héritage (RG-15), ['USER'] = tout utilisateur connecté,
         * ['MANAGER'] = gestionnaire ou administrateur. Tableau vide pour une route publique.
         */
        roles: readonly Role[];
        /** Vrai si la page est accessible sans être connecté (page de connexion). */
        public?: boolean;
    }
}
