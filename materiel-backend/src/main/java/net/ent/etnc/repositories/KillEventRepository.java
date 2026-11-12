package net.ent.etnc.repositories;

import net.ent.etnc.models.KillEvent;
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
 * Accès persistence pour les {@link KillEvent}.
 */
@NullMarked
@Repository
public interface KillEventRepository extends BaseRepository<KillEvent> {

    @Override
    @EntityGraph(attributePaths = {"uc", "uc.room", "uc.room.building", "user"})
    Page<KillEvent> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"uc", "uc.room", "uc.room.building", "user"})
    Optional<KillEvent> findById(Long id);

    @EntityGraph(attributePaths = {"uc", "uc.room", "uc.room.building", "user"})
    @Query("SELECT k FROM KillEvent k WHERE k.user.id = :userId")
    Page<KillEvent> findByUser_Id(@Param("userId") Long userId, Pageable pageable);
}
