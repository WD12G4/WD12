package net.ent.etnc.services.impl;

import net.ent.etnc.models.Building;
import net.ent.etnc.models.Room;
import net.ent.etnc.repositories.RoomRepository;
import net.ent.etnc.services.BuildingService;
import net.ent.etnc.services.RoomService;
import net.ent.etnc.services.commons.AbstractService;
import net.ent.etnc.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service des {@link Room}.
 */
@Service
public class RoomServiceImpl extends AbstractService<Room, RoomRepository> implements RoomService {

    /**
     * Service concernant les {@link Building}.
     */
    private final BuildingService buildingService;

    /**
     * Constructeur du service.
     */
    @Autowired
    public RoomServiceImpl(final RoomRepository repository, final BuildingService buildingService) {
        super(repository);
        this.buildingService = buildingService;
    }

    @Override
    @Transactional
    public Room create(final Room room) {
        validateRoom(room);
        if (findByNumberAndBuilding(room.getNumber(), room.getBuilding()).isPresent()) {
            throw new ServiceException(String.format("La salle numéro %s éxiste déjà dans le bâtiment %s.", room.getNumber(), room.getBuilding().getNumber()));
        }
        return super.create(room);
    }

    private void validateRoom(final Room room) {
        if (room.getNumber() == null || room.getNumber() < 0) {
            throw new ServiceException("La numéro de la salle doit être positif.");
        }
        if (room.getNumber() > 999) {
            throw new ServiceException("La numéro de la salle doit être inférieur à 999.");
        }
    }
    
    @Override
    public Room update(final Room room) {
        validateRoom(room);
        return super.update(room);
    }

    @Override
    public Optional<Room> findByNumberAndBuilding(final Integer number, final Building building) {
        return this.repository.findByNumberAndBuilding(number, building);
    }
}
