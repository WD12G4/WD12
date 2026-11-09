package net.ent.etnc.services;

import net.ent.etnc.models.Building;
import net.ent.etnc.models.Room;
import net.ent.etnc.services.commons.Service;

import java.util.Optional;

/**
 * Interface de service des {@link Room}.
 */
public interface RoomService extends Service<Room, Long> {

    Optional<Room> findByNumberAndBuilding(final Integer number, final Building building);
}
