package net.ent.etnc.dtos.material;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.ent.etnc.models.enums.EMaterialType;

import java.time.LocalDate;

/**
 * Données d'entrée pour la création ou mise à jour d'un matériel.
 */
public record MaterialRequest(
        @NotBlank(message = "Le numéro de série doit être renseigné")
        @Size(max = 100, message = "La taille du numéro de série ne doit pas dépasser 100 caractères")
        String serialNumber,
        @NotNull(message = "Le type de matériel doit être renseigné")
        EMaterialType materialType,
        LocalDate endAvailabilityDate,
        Long roomId,
        boolean archived
) {
}
