package net.ent.etnc.dtos.supervision;

/**
 * Données d'un poste de travail pour la supervision.
 * os : système remonté par l'agent (« windows », « linux », « osx »), ou null si l'agent n'est pas connecté.
 */
public record WorkstationDto(
        Long id,
        Long roomId,
        String name,
        String traineeLabel,
        String macAddress,
        boolean online,
        String hostname,
        String username,
        String os
) {
    public WorkstationDto(Long id, Long roomId, String name, String traineeLabel, String macAddress, boolean online) {
        this(id, roomId, name, traineeLabel, macAddress, online, null, null, null);
    }

    public WorkstationDto(Long id, Long roomId, String name, String traineeLabel, String macAddress, boolean online, String hostname) {
        this(id, roomId, name, traineeLabel, macAddress, online, hostname, null, null);
    }

    public WorkstationDto(Long id, Long roomId, String name, String traineeLabel, String macAddress, boolean online, String hostname, String username) {
        this(id, roomId, name, traineeLabel, macAddress, online, hostname, username, null);
    }
}
