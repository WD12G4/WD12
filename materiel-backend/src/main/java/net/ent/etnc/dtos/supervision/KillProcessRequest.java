package net.ent.etnc.dtos.supervision;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requête d'arrêt d'un processus sur un poste.
 */
public record KillProcessRequest(
        @NotNull(message = "L'identifiant du poste doit être renseigné")
        Long workstationId,

        @NotNull(message = "Le PID du processus doit être renseigné")
        Integer pid,

        @NotBlank(message = "Le nom du processus doit être renseigné")
        String processName
) {
}
