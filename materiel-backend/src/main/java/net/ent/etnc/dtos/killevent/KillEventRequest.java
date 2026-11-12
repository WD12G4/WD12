package net.ent.etnc.dtos.killevent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Données d'entrée pour la création d'un kill event.
 */
public record KillEventRequest(
        @NotBlank(message = "Le nom du processus doit être renseigné")
        @Size(max = 100, message = "La taille du nom du processus ne doit pas dépasser 100 caractères")
        String processName,

        @NotNull(message = "L'UC doit être renseignée")
        Long ucId,

        @NotNull(message = "L'heure et la date doivent être renseignées")
        @PastOrPresent(message = "La date/heure d'arrêt d'un processus ne peut pas être après le jour/l'heure actuelle.")
        LocalDateTime killDateTime,

        Boolean result,

        Long userId
) {
    public KillEventRequest(String processName, Long ucId, LocalDateTime killDateTime) {
        this(processName, ucId, killDateTime, true, null);
    }

    public KillEventRequest(String processName, Long ucId, LocalDateTime killDateTime, Boolean result) {
        this(processName, ucId, killDateTime, result, null);
    }

    @Override
    public Boolean result() {
        return result != null ? result : true;
    }
}
