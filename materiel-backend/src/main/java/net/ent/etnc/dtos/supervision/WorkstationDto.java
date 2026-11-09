package net.ent.etnc.dtos.supervision;

/**
 * Données d'un poste de travail pour la supervision.
 */
public record WorkstationDto(
        Long id,
        Long roomId,
        String name,
        String traineeLabel,
        String macAddress,
        boolean online,
        String hostname
) {
    public WorkstationDto(Long id, Long roomId, String name, String traineeLabel, String macAddress, boolean online) {
        this(id, roomId, name, traineeLabel, macAddress, online, null);
    }
}
