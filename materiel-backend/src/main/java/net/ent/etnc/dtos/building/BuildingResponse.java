package net.ent.etnc.dtos.building;

/**
 * Données d'un bâtiment renvoyées par l'API.
 */
public record BuildingResponse(Long id, Long version, Integer number) {
}
