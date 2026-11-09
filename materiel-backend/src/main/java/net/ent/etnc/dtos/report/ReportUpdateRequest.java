package net.ent.etnc.dtos.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Données d'entrée pour la modification d'un signalement par son créateur.
 */
public record ReportUpdateRequest(
        @NotBlank(message = "La description doit être renseignée")
        @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
        String description
) {
}
