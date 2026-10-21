package net.ent.etnc.dtos.room;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Données d'entrée pour la création ou mise à jour d'une salle.
 */
public record RoomRequest(
        @NotNull(message = "L'identifiant du bâtiment doit être renseigné")
        Long buildingId,

        @NotNull(message = "Le numéro de salle doit être renseigné")
        @Min(value = 0, message = "Le numéro de salle doit être positif")
        @Max(value = 999, message = "Le numéro de salle doit être inférieur à 999")
        Integer number
) {}
