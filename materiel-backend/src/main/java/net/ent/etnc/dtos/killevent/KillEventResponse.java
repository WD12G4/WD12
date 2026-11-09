package net.ent.etnc.dtos.killevent;

import net.ent.etnc.dtos.material.MaterialResponse;

import java.time.LocalDateTime;

/**
 * Données d'un kill event renvoyées par l'API.
 */
public record KillEventResponse(
        Long id,
        Long version,
        String processName,
        MaterialResponse uc,
        LocalDateTime killDateTime,
        Boolean result
) {
    public KillEventResponse(Long id, Long version, String processName, MaterialResponse uc, LocalDateTime killDateTime) {
        this(id, version, processName, uc, killDateTime, true);
    }
}
