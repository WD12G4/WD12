package net.ent.etnc.dtos.supervision;

/**
 * Réponse suite à une demande d'arrêt de processus.
 */
public record KillProcessResponse(
        boolean success,
        String message,
        Long killEventId
) {
}
