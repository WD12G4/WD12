package net.ent.etnc.dtos.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Données d'entrée pour la création d'un signalement.
 */
public record ReportCreateRequest(
        @NotBlank(message = "La description doit être renseignée")
        @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
        String description,

        @NotNull(message = "Le matériel doit être renseigné")
        Long materialId
) {
}
