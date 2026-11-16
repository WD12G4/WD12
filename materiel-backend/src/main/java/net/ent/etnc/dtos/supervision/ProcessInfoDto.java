package net.ent.etnc.dtos.supervision;

import java.time.LocalDateTime;

/**
 * Données d'un processus remonté par l'agent d'un poste.
 * name : nom du processus remonté par l'agent (sert à l'arrêt et à la liste noire).
 * displayName : nom affiché, celui de l'application dans le catalogue, ou le nom du processus à défaut.
 */
public record ProcessInfoDto(
        int pid,
        Long workstationId,
        String name,
        String displayName,
        LocalDateTime startedAt,
        String icon
) {
    public ProcessInfoDto(int pid, Long workstationId, String name, LocalDateTime startedAt) {
        this(pid, workstationId, name, name, startedAt, null);
    }
}
