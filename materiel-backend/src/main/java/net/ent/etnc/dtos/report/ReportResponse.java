package net.ent.etnc.dtos.report;

import net.ent.etnc.dtos.auth.UserSummaryResponse;
import net.ent.etnc.dtos.material.MaterialResponse;

import java.time.LocalDateTime;

/**
 * Données d'un signalement renvoyées par l'API.
 */
public record ReportResponse(
        Long id,
        Long version,
        String description,
        LocalDateTime reportDateTime,
        UserSummaryResponse user,
        MaterialResponse material,
        LocalDateTime handleDateTime,
        boolean handled
) {
}
