package net.ent.etnc.repositories;

import net.ent.etnc.models.Material;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Accès persistence pour les {@link Material}.
 */
@Repository
public interface MaterialRepository extends BaseRepository<Material> {

    boolean existsBySerialNumber(String serialNumber);

    Optional<Material> findBySerialNumber(String serialNumber);

    List<Material> findByRoomIdAndMaterialTypeAndArchivedFalse(Long roomId, EMaterialType materialType);

    List<Material> findByMaterialTypeAndArchivedFalse(EMaterialType materialType);
}
