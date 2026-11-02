package net.ent.etnc.services.commons;

import net.ent.etnc.models.commons.AbstractPersistableWithIdSetter;
import net.ent.etnc.repositories.commons.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Generic transactional CRUD implementation.
 *
 * @param <T> entity type
 * @param <R> repository type
 */
public abstract class AbstractService<T extends AbstractPersistableWithIdSetter<Long>, R extends BaseRepository<T>>
        implements Service<T, Long> {

    /**
     * Resource repository.
     */
    protected final R repository;

    /**
     * Creates the service.
     */
    protected AbstractService(R repository) {
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public T create(T entity) {
        return repository.saveAndFlush(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public T update(T entity) {
        if (entity.getId() == null || !repository.existsById(entity.getId())) {
            throw new ResourceNotFoundException("Ressource introuvable : " + entity.getId());
        }
        return repository.saveAndFlush(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(final Pageable pageable) {
        return repository.findAll(pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(final Specification<T> specification, final Pageable pageable) {
        return repository.findAll(specification, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteById(final Long id) {
        repository.deleteById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsById(final Long id) {
        return repository.existsById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public long count() {
        return repository.count();
    }
}
