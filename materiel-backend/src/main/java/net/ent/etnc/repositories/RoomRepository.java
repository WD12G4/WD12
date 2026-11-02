package net.ent.etnc.repositories;

import net.ent.etnc.models.Building;
import net.ent.etnc.models.Room;
import net.ent.etnc.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Accès persistence pour les {@link Room}.
 */
@Repository
public interface RoomRepository extends BaseRepository<Room> {

    Optional<Room> findByNumberAndBuilding(Integer number, Building building);
}
