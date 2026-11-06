package net.ent.etnc.repositories;

import net.ent.etnc.models.Room;
import net.ent.etnc.models.Sighting;
import net.ent.etnc.repositories.commons.BaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Accès persistence pour les {@link Sighting}.
 */
@Repository
public interface SightingRepository extends BaseRepository<Sighting> {

    Optional<Sighting> findBySerialNumberAndRoom(String serialNumber, Room room);

    /**
     * Supprime tous les constats d'une salle.
     *
     * @return nombre de constats supprimés
     */
    @Modifying
    @Query("delete from Sighting s where s.room.id = :roomId")
    int deleteByRoomId(@Param("roomId") Long roomId);
}
