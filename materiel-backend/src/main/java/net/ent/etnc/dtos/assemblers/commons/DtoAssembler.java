package net.ent.etnc.dtos.assemblers.commons;

import java.util.Collection;
import java.util.List;

/**
 * Contrat générique pour assembler les DTOs et les entités.
 *
 * @param <E>    Type de l'entité
 * @param <REQ>  Type du DTO de requête
 * @param <RESP> Type du DTO de réponse
 */
public interface DtoAssembler<E, REQ, RESP> {

    /**
     * Transforme une entité en DTO de réponse.
     */
    RESP toDto(E entity);

    /**
     * Transforme un DTO de requête en nouvelle entité.
     */
    E toEntity(REQ request);

    /**
     * Met à jour une entité existante à partir d'un DTO de requête.
     */
    void updateEntity(E entity, REQ request);

    /**
     * Transforme une collection d'entités en liste de DTOs.
     */
    default List<RESP> toDtoList(Collection<E> entities) {
        if (entities == null) return List.of();
        return entities.stream().map(this::toDto).toList();
    }
}
