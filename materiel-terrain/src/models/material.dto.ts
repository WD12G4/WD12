/**
 * Forme JSON échangée avec /api/v1/materials et conversions vers le modèle.
 * Source : MaterialResponse, MaterialRequest et MaterialAssignRequest (backend, package dtos.material).
 */
import type {Material, MaterialInput, MaterialType} from './material.model.ts';
import {type RoomDto, roomFromDto, roomToDto} from './room.dto.ts';

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

/** Corps attendu par POST /api/v1/materials. */
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
 * new Date('2026-11-03') serait interprétée en UTC et pourrait afficher la veille.
 * @param value date reçue de l'API
 */
function parseLocalDate(value: string): Date {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
}

/**
 * Convertit une Date en 'AAAA-MM-JJ' (heure locale) ; toISOString() pourrait donner la veille.
 * @param date date à convertir
 */
function formatLocalDate(date: Date): string {
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${date.getFullYear()}-${month}-${day}`;
}

/**
 * Convertit la réponse de l'API en modèle front.
 * @param dto matériel reçu de l'API
 */
export function materialFromDto(dto: MaterialDto): Material {
    return {
        ...dto,
        endAvailabilityDate: dto.endAvailabilityDate ? parseLocalDate(dto.endAvailabilityDate) : null,
        room: dto.room ? roomFromDto(dto.room) : null,
    };
}

/**
 * Convertit un matériel en forme JSON, pour le garder dans le cache du téléphone.
 * @param material matériel à enregistrer
 */
export function materialToDto(material: Material): MaterialDto {
    return {
        ...material,
        endAvailabilityDate: material.endAvailabilityDate ? formatLocalDate(material.endAvailabilityDate) : null,
        room: material.room ? roomToDto(material.room) : null,
    };
}

/**
 * Convertit la saisie de prise en compte en corps de requête.
 * Le numéro de série est nettoyé des espaces (RG-01) ; un matériel trouvé sur le terrain n'est pas archivé.
 * @param input saisie du formulaire
 */
export function materialToRequest(input: MaterialInput): MaterialRequestDto {
    return {...input, serialNumber: input.serialNumber.trim(), archived: false};
}
