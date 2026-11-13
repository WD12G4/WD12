package net.ent.etnc.dtos.supervision;

import java.time.LocalDateTime;

/**
 * Données d'un processus remonté par l'agent d'un poste.
 */
public record ProcessInfoDto(
        int pid,
        Long workstationId,
        String name,
        LocalDateTime startedAt,
        String icon
) {
    public ProcessInfoDto(int pid, Long workstationId, String name, LocalDateTime startedAt) {
        this(pid, workstationId, name, startedAt, null);
    }
}
