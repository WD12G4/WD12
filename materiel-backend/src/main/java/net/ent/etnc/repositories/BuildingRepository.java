package net.ent.etnc.repositories;

import net.ent.etnc.models.Building;
import net.ent.etnc.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Accès persistence pour les {@link Building}.
 */
@Repository
public interface BuildingRepository extends BaseRepository<Building> {

    Optional<Building> findByNumber(Integer number);
}
