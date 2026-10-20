package net.ent.etnc.models.enums;

import lombok.Getter;

public enum EMaterialType {
    UC("Poste Utilisateur"),
    SCREEN("Écran"),
    PROJECTOR("Projecteur"),
    PERIPHERAL("Périphérique");

    @Getter
    private final String libelle;

    EMaterialType(final String libelle) {
        this.libelle = libelle;
    }
}
