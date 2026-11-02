package net.ent.etnc.dtos.room;

import net.ent.etnc.dtos.building.BuildingResponse;

/**
 * Données d'une salle renvoyées par l'API.
 */
public record RoomResponse(Long id, Long version, Integer number, BuildingResponse building) {
}
