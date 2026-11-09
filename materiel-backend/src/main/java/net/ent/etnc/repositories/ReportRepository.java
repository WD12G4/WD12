package net.ent.etnc.repositories;

import net.ent.etnc.models.Report;
import net.ent.etnc.repositories.commons.BaseRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Accès persistance pour les {@link Report}.
 */
@NullMarked
@Repository
public interface ReportRepository extends BaseRepository<Report> {

    @Override
    @EntityGraph(attributePaths = {"user", "material", "material.room", "material.room.building"})
    Page<Report> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"user", "material", "material.room", "material.room.building"})
    Optional<Report> findById(Long id);

    @EntityGraph(attributePaths = {"user", "material", "material.room", "material.room.building"})
    @Query("SELECT r FROM Report r WHERE r.material.id = :materialId")
    Page<Report> findByMaterial_Id(@Param("materialId") Long materialId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "material", "material.room", "material.room.building"})
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId")
    Page<Report> findByUser_Id(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "material", "material.room", "material.room.building"})
    Page<Report> findByHandled(boolean handled, Pageable pageable);
}
