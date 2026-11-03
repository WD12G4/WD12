/**
 * Accès à la ressource /api/v1/users (réservée à l'ADMIN).
 * Toutes les méthodes lèvent ApiError si l'API répond en erreur.
 * RG : RG-19, RG-20, RG-32
 */
import type {User, UserCreateInput, UserUpdateInput} from '../models/user.model.ts';
import {type UserDto, userFromDto, userToCreateRequest, userToUpdateRequest} from '../models/user.dto.ts';
import {type PageDto, pageContent} from '../models/page.dto.ts';
import {apiRequest} from './http-client.ts';

export const usersServices = {

    /**
     * Liste tous les comptes, triés par identifiant de connexion.
     * TODO(perf): une seule page large ; à paginer au-delà de 1000 comptes.
     */
    findAll: async (): Promise<User[]> => {
        const page = await apiRequest<PageDto<UserDto>>('/users?size=1000&sort=username');
        return pageContent(page).map(userFromDto);
    },

    /** Crée un compte. Lève ApiError 400 si l'identifiant est déjà pris ou le mot de passe trop court (RG-20). */
    create: async (input: UserCreateInput): Promise<User> => {
        const dto = await apiRequest<UserDto>('/users', {
            method: 'POST',
            body: JSON.stringify(userToCreateRequest(input)),
        });
        return userFromDto(dto);
    },

    /** Modifie le rôle, l'activation (RG-19) et éventuellement le mot de passe d'un compte. */
    update: async (id: number, input: UserUpdateInput): Promise<User> => {
        const dto = await apiRequest<UserDto>(`/users/${id}`, {
            method: 'PUT',
            body: JSON.stringify(userToUpdateRequest(input)),
        });
        return userFromDto(dto);
    },

    /** Supprime un compte. Suppression définitive côté backend. */
    delete: async (id: number): Promise<void> => {
        await apiRequest<void>(`/users/${id}`, {method: 'DELETE'});
    },
};
