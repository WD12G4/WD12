package net.ent.etnc.repositories;

import net.ent.etnc.models.Room;
import net.ent.etnc.repositories.commons.BaseRepository;
import org.springframework.stereotype.Repository;

/**
 * Accès persistence pour les {@link Room}.
 */
@Repository
public interface RoomRepository extends BaseRepository<Room> {

}
