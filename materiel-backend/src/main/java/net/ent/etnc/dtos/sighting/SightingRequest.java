package net.ent.etnc.dtos.sighting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Données d'entrée d'un constat : numéro scanné dans une salle.
 */
public record SightingRequest(
        @NotBlank(message = "Le numéro de série doit être renseigné")
        @Size(max = 100, message = "La taille du numéro de série ne doit pas dépasser 100 caractères")
        String serialNumber,

        @NotNull(message = "La salle doit être renseignée")
        Long roomId,

        /* Date du scan s'il a été fait hors ligne, sinon null (heure du serveur). */
        Instant seenAt,

        /* Numéro lu puis corrigé par l'agent : son constat est retiré. */
        @Size(max = 100, message = "La taille du numéro remplacé ne doit pas dépasser 100 caractères")
        String replaces
) {}
