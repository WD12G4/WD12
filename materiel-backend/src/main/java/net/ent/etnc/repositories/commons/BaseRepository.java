package net.ent.etnc.repositories.commons;

import net.ent.etnc.models.commons.AbstractPersistableWithIdSetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Shared repository contract.
 *
 * @param <T> entity type
 */
@NoRepositoryBean
public interface BaseRepository<T extends AbstractPersistableWithIdSetter<Long>>
        extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
}
