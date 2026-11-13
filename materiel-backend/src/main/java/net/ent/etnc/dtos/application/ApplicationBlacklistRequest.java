package net.ent.etnc.dtos.application;

import jakarta.validation.constraints.NotNull;

public record ApplicationBlacklistRequest(
        @NotNull(message = "Le statut blacklisté doit être renseigné")
        Boolean blacklisted
) {
}
