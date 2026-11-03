/**
 * Accès aux routes d'authentification /api/v1/auth/.
 * Attention : ces routes se terminent toutes par « / » (login/, refresh/, logout/, me/).
 * Utilisé par : shared/composables/use-auth.ts
 */
import type {User} from '../models/user.model.ts';
import {type LoginRequestDto, type UserDto, userFromDto} from '../models/user.dto.ts';
import {apiRequest, apiTokenRequest} from './http-client.ts';

export const authServices = {

    /**
     * Connecte l'utilisateur. Le backend répond 200 sans corps :
     * le jeton est dans l'en-tête Authorization et le cookie de rafraîchissement est posé.
     * @returns le jeton d'accès, ou null si l'en-tête est absent
     * @throws ApiError 401 si les identifiants sont faux ou le compte désactivé (RG-19)
     */
    login: async (credentials: LoginRequestDto): Promise<string | null> => {
        return apiTokenRequest('/auth/login/', {
            method: 'POST',
            body: JSON.stringify(credentials),
        });
    },

    /**
     * Rafraîchit la session grâce au cookie HttpOnly (envoyé automatiquement par le navigateur).
     * @returns le nouveau jeton d'accès
     * @throws ApiError 401 si la session a expiré
     */
    refresh: async (): Promise<string | null> => {
        return apiTokenRequest('/auth/refresh/', {method: 'POST'});
    },

    /** Récupère le profil de l'utilisateur connecté. */
    me: async (): Promise<User> => {
        const dto = await apiRequest<UserDto>('/auth/me/');
        return userFromDto(dto);
    },

    /** Déconnecte l'utilisateur : le backend révoque le jeton de rafraîchissement et efface le cookie. */
    logout: async (): Promise<void> => {
        await apiRequest<void>('/auth/logout/', {method: 'POST'});
    },
};
