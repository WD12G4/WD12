/**
 * Forme JSON échangée avec /api/v1/auth/, et conversion vers le modèle.
 * Source : UserResponse et LoginRequest (backend, packages dtos.user et dtos.auth).
 */
import type {Role, User} from './user.model.ts';

/** Réponse de l'API pour un compte (GET /auth/me/). */
export interface UserDto {
    id: number;
    version: number;
    username: string;
    role: Role;
    enabled: boolean;
}

/** Corps attendu par POST /api/v1/auth/login/. */
export interface LoginRequestDto {
    username: string;
    password: string;
}

/**
 * Convertit la réponse de l'API en modèle front.
 * @param dto compte reçu de l'API
 */
export function userFromDto(dto: UserDto): User {
    return {...dto};
}
