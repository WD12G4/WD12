package net.ent.etnc.repositories;

import net.ent.etnc.models.Material;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.repositories.commons.BaseRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Accès persistence pour les {@link Material}.
 */
@NullMarked
@Repository
public interface MaterialRepository extends BaseRepository<Material> {

    @Override
    @EntityGraph(attributePaths = {"room", "room.building"})
    Page<Material> findAll(final Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"room", "room.building"})
    Page<Material> findAll(final Specification<Material> spec, final Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"room", "room.building"})
    List<Material> findAll();

    @Override
    @EntityGraph(attributePaths = {"room", "room.building"})
    Optional<Material> findById(final Long id);

    boolean existsBySerialNumber(final String serialNumber);

    @EntityGraph(attributePaths = {"room", "room.building"})
    Optional<Material> findBySerialNumber(final String serialNumber);

    @EntityGraph(attributePaths = {"room", "room.building"})
    List<Material> findByRoomIdAndMaterialTypeAndArchivedFalse(final Long roomId, final EMaterialType materialType);

    @EntityGraph(attributePaths = {"room", "room.building"})
    List<Material> findByMaterialTypeAndArchivedFalse(final EMaterialType materialType);
}
