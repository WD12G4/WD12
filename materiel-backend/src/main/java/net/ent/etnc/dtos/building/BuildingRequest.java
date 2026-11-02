package net.ent.etnc.dtos.building;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Données d'entrée pour la création ou mise à jour d'un bâtiment.
 */
public record BuildingRequest(
        @NotNull(message = "Le numéro de bâtiment doit être renseigné")
        @Min(value = 0, message = "Le numéro de bâtiment doit être positif")
        @Max(value = 999, message = "Le numéro de bâtiment doit être inférieur à 999")
        Integer number
) {}
