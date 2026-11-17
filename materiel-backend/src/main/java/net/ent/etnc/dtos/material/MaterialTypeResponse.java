package net.ent.etnc.dtos.material;

import net.ent.etnc.models.enums.EMaterialType;

/**
 * Données d'un type de matériel (code et libellé) renvoyées par l'API.
 */
public record MaterialTypeResponse(
        String code,
        String libelle
) {
    public static MaterialTypeResponse from(final EMaterialType type) {
        return new MaterialTypeResponse(type.name(), type.getLibelle());
    }
}
