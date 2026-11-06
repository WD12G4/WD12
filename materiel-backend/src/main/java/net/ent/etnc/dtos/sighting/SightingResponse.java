package net.ent.etnc.dtos.sighting;

import java.time.Instant;

/**
 * Constat renvoyé par l'API : numéro scanné, salle, date et agent du dernier scan.
 */
public record SightingResponse(
        Long id,
        String serialNumber,
        Long roomId,
        Instant seenAt,
        String seenBy
) {}
