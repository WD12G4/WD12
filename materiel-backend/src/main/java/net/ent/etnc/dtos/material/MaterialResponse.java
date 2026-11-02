package net.ent.etnc.dtos.material;

import net.ent.etnc.dtos.room.RoomResponse;
import net.ent.etnc.models.enums.EMaterialType;

import java.time.LocalDate;

/**
 * Données d'un matériel renvoyées par l'API.
 */
public record MaterialResponse(
        Long id,
        Long version,
        String serialNumber,
        EMaterialType materialType,
        LocalDate endAvailabilityDate,
        RoomResponse room,
        boolean archived
) {}
