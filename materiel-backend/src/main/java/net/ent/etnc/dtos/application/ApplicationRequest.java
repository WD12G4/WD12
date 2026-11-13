package net.ent.etnc.dtos.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationRequest(
        @NotBlank(message = "Le nom de l'application doit être renseigné")
        @Size(max = 150, message = "La taille du nom ne doit pas dépasser 150 caractères")
        String name,

        @Size(max = 150, message = "Le nom d'affichage ne doit pas dépasser 150 caractères")
        String displayName,

        String iconBase64,

        Boolean blacklisted,

        @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
        String description
) {
}
