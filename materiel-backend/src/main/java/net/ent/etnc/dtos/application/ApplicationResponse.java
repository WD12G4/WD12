package net.ent.etnc.dtos.application;

public record ApplicationResponse(
        Long id,
        Long version,
        String name,
        String displayName,
        String iconBase64,
        boolean blacklisted
) {
}
