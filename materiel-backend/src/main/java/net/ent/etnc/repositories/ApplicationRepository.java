package net.ent.etnc.repositories;

import net.ent.etnc.models.Application;
import net.ent.etnc.repositories.commons.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Référentiel de persistance pour les entités {@link Application}.
 */
@Repository
public interface ApplicationRepository extends BaseRepository<Application> {

    Optional<Application> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Application> findByBlacklistedTrue();

    Page<Application> findByBlacklisted(boolean blacklisted, Pageable pageable);
}
