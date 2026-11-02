package net.ent.etnc.models.commons;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;

/**
 * Base class for versioned persistent entities with an assignable identifier.
 *
 * @param <ID> identifier type
 */
@MappedSuperclass
public abstract class AbstractPersistableWithIdSetter<ID extends Serializable> implements Persistable<ID> {

    /**
     * Technical identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    @Setter
    private ID id;

    /**
     * Optimistic-lock version. Permet d'empêcher d'écraser une v2 avec une v1.
     */
    @Version
    @Getter
    @Setter
    private Long version;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNew() {
        return id == null;
    }
}
