package net.ent.etnc.repositories;

import net.ent.etnc.models.KillEvent;
import net.ent.etnc.repositories.commons.BaseRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Accès persistence pour les {@link KillEvent}.
 */
@NullMarked
@Repository
public interface KillEventRepository extends BaseRepository<KillEvent> {

    @Override
    @EntityGraph(attributePaths = {"uc", "uc.room", "uc.room.building"})
    Page<KillEvent> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"uc", "uc.room", "uc.room.building"})
    Optional<KillEvent> findById(Long id);
}
