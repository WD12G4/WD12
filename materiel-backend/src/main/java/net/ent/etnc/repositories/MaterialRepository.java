package net.ent.etnc.repositories;

import net.ent.etnc.models.Material;
import net.ent.etnc.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

/**
 * Accès persistence pour les {@link Material}.
 */
@Repository
public interface MaterialRepository extends BaseRepository<Material> {

    boolean existsBySerialNumber(String serialNumber);
}
