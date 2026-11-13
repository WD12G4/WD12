/**
 * Accès à la route d'authentification /api/v1/auth/me/ (route terminée par « / »).
 * La connexion, le renouvellement du jeton et la déconnexion passent par Keycloak (services/keycloak.ts).
 * Utilisé par : shared/composables/use-auth.ts
 */
import type {User} from '../models/user.model.ts';
import {type UserDto, userFromDto} from '../models/user.dto.ts';
import {apiRequest} from './http-client.ts';

export const authServices = {

    /**
     * Récupère le profil du compte identifié par le jeton Keycloak.
     * @throws ApiError 401 si l'application ne connaît pas ce compte ou s'il est désactivé (RG-19)
     */
    me: async (): Promise<User> => {
        const dto = await apiRequest<UserDto>('/auth/me/');
        return userFromDto(dto);
    },
};
