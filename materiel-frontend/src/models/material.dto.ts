/**
 * Forme JSON échangée avec /api/v1/materials et conversions vers le modèle.
 * Source : MaterialResponse, MaterialRequest et MaterialAssignRequest (backend, package dtos.material).
 */
import type {Material, MaterialInput, MaterialType} from './material.model.ts';
import {type RoomDto, roomFromDto} from './room.dto.ts';

/** Réponse de l'API pour un matériel. Les dates sont des chaînes 'AAAA-MM-JJ'. */
export interface MaterialDto {
    id: number;
    version: number;
    serialNumber: string;
    materialType: MaterialType;
    endAvailabilityDate: string | null;
    room: RoomDto | null;
    archived: boolean;
}

/** Corps attendu par POST et PUT /api/v1/materials. */
export interface MaterialRequestDto {
    serialNumber: string;
    materialType: MaterialType;
    endAvailabilityDate: string | null;
    roomId: number | null;
    archived: boolean;
}

/** Corps attendu par PATCH /api/v1/materials/{id}/room. */
export interface MaterialAssignRequestDto {
    /** null = remettre le matériel en stock (RG-07). */
    roomId: number | null;
}

/**
 * Convertit une date 'AAAA-MM-JJ' en Date locale.
 * new Date('2026-11-03') serait interprétée en UTC et pourrait afficher la veille :
 * on construit donc la date à partir de ses composants.
 * @param value date reçue de l'API
 */
function parseLocalDate(value: string): Date {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
}

/**
 * Convertit la réponse de l'API en modèle front.
 * @param dto matériel reçu de l'API
 * @returns le matériel, avec la date convertie en Date
 */
export function materialFromDto(dto: MaterialDto): Material {
    return {
        ...dto,
        endAvailabilityDate: dto.endAvailabilityDate ? parseLocalDate(dto.endAvailabilityDate) : null,
        room: dto.room ? roomFromDto(dto.room) : null,
    };
}

/**
 * Convertit la saisie du formulaire en corps de requête.
 * Le numéro de série est nettoyé des espaces (RG-01) ; une date vidée part à null (RG-04 : facultative).
 * @param input saisie du formulaire
 */
export function materialToRequest(input: MaterialInput): MaterialRequestDto {
    return {...input, serialNumber: input.serialNumber.trim(), endAvailabilityDate: input.endAvailabilityDate || null};
}
