package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.sighting.SightingResponse;
import net.ent.etnc.models.Sighting;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Assembler des {@link Sighting} vers leur DTO de réponse. Pas de conversion inverse :
 * le service crée ou met à jour le constat à partir du numéro et de la salle.
 */
@Component
public class SightingDtoAssembler {

    /**
     * Transforme un constat en DTO de réponse.
     */
    public SightingResponse toDto(Sighting entity) {
        if (entity == null) return null;
        return new SightingResponse(entity.getId(), entity.getSerialNumber(), entity.getRoom().getId(),
                entity.getSeenAt(), entity.getSeenBy());
    }

    /**
     * Transforme une collection de constats en liste de DTOs.
     */
    public List<SightingResponse> toDtoList(Collection<Sighting> entities) {
        if (entities == null) return List.of();
        return entities.stream().map(this::toDto).toList();
    }
}
