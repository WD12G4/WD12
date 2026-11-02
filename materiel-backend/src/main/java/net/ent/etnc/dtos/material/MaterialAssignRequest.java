package net.ent.etnc.dtos.material;

/**
 * Données d'entrée pour affecter ou désaffecter un matériel à une salle.
 */
public record MaterialAssignRequest(
        Long roomId
) {}
