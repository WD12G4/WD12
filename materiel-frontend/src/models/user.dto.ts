/**
 * Forme JSON échangée avec /api/v1/users et /api/v1/auth/me/, et conversions vers le modèle.
 * Source : UserResponse, UserCreateRequest et UserUpdateRequest (backend, package dtos.user).
 */
import type {Role, User, UserCreateInput, UserUpdateInput} from './user.model.ts';

/** Réponse de l'API pour un compte. */
export interface UserDto {
    id: number;
    version: number;
    username: string;
    role: Role;
    enabled: boolean;
}

/** Corps attendu par POST /api/v1/users. */
export interface UserCreateRequestDto {
    username: string;
    password: string;
    role: Role;
}

/** Corps attendu par PUT /api/v1/users/{id}. Un champ absent n'est pas modifié. */
export interface UserUpdateRequestDto {
    password?: string;
    role: Role;
    enabled: boolean;
}

/**
 * Convertit la réponse de l'API en modèle front.
 * @param dto compte reçu de l'API
 */
export function userFromDto(dto: UserDto): User {
    return {...dto};
}

/**
 * Convertit la saisie de création en corps de requête.
 * @param input saisie du formulaire de création
 */
export function userToCreateRequest(input: UserCreateInput): UserCreateRequestDto {
    return {...input, username: input.username.trim()};
}

/**
 * Convertit la saisie de modification en corps de requête.
 * Le mot de passe n'est envoyé que s'il a été saisi : sinon le backend le garderait vide.
 * @param input saisie du formulaire de modification
 */
export function userToUpdateRequest(input: UserUpdateInput): UserUpdateRequestDto {
    const request: UserUpdateRequestDto = {role: input.role, enabled: input.enabled};
    if (input.password.length > 0) {
        request.password = input.password;
    }
    return request;
}
