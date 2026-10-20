package net.ent.etnc.models.commons;

import jakarta.persistence.GeneratedValue;
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
    @GeneratedValue
    @Getter
    @Setter
    private ID id;

    /**
     * Optimistic-lock version. permet de d'empecher de save un v1 alors qu'il y a un v2
     */
    @Version //peut se mettre sur nombres entiers ou datetime
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
