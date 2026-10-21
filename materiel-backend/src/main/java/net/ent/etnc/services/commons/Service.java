package net.ent.etnc.services.commons;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

/**
 * Generic CRUD service contract.
 *
 * @param <T>  entity type
 * @param <ID> identifier type
 */
public interface Service<T, ID> {
    /**
     * Creates an entity.
     */
    T create(T entity);

    /**
     * Updates an entity.
     */
    T update(T entity);

    /**
     * Finds an entity.
     */
    Optional<T> findById(ID id);

    /**
     * Lists entities.
     */
    Page<T> findAll(Pageable pageable);

    /**
     * Lists filtered entities.
     */
    Page<T> findAll(Specification<T> specification, Pageable pageable);

    /**
     * Deletes an entity by identifier.
     */
    void deleteById(ID id);

    /**
     * Checks entity existence.
     */
    boolean existsById(ID id);

    /**
     * Counts persisted entities.
     */
    long count();
}
