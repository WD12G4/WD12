/**
 * Forme JSON échangée avec /api/v1/applications (catalogue et liste noire) et conversion vers le modèle.
 * Source : ApplicationResponse, ApplicationBlacklistRequest (backend, package dtos.application).
 * RG : RG-27 (liste noire)
 */
import type {Application} from './application.model.ts';
import {iconFromDto} from './app-icon.dto.ts';

/** Réponse de l'API pour une application. */
export interface ApplicationDto {
    id: number;
    version: number;
    name: string;
    displayName: string | null;
    iconBase64: string | null;
    blacklisted: boolean;
    description: string | null;
}

/**
 * Convertit une application reçue de l'API.
 * @param dto application reçue
 */
export function applicationFromDto(dto: ApplicationDto): Application {
    return {
        id: dto.id,
        version: dto.version,
        name: dto.name,
        displayName: dto.displayName?.trim() || dto.name,
        icon: iconFromDto(dto.iconBase64),
        blacklisted: dto.blacklisted,
    };
}
