package net.ent.etnc.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.models.Building;
import net.ent.etnc.models.Room;
import net.ent.etnc.repositories.RoomRepository;
import net.ent.etnc.services.BuildingService;
import net.ent.etnc.services.RoomService;
import net.ent.etnc.services.commons.AbstractService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service des {@link Room}.
 */
@Slf4j
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
        log.debug("Demande de création d'une salle : numéro={}, bâtiment_id={}",
                room.getNumber(), room.getBuilding() != null ? room.getBuilding().getId() : null);

        validateRoom(room);
        if (findByNumberAndBuilding(room.getNumber(), room.getBuilding()).isPresent()) {
            log.warn("Échec création salle : la salle numéro {} existe déjà dans le bâtiment {}",
                    room.getNumber(), room.getBuilding().getNumber());
            throw new ServiceException(String.format("La salle numéro %s existe déjà dans le bâtiment %s.",
                    room.getNumber(), room.getBuilding().getNumber()));
        }

        Room created = super.create(room);
        log.info("Salle créée avec succès [id={}, number={}, buildingNumber={}]",
                created.getId(), created.getNumber(), created.getBuilding().getNumber());
        return created;
    }

    private void validateRoom(final Room room) {
        if (room.getBuilding() == null || room.getBuilding().getId() == null) {
            log.warn("Validation rejetée : bâtiment non renseigné pour la salle.");
            throw new ServiceException("Le bâtiment doit être renseigné.");
        }

        Building building = buildingService.findById(room.getBuilding().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bâtiment introuvable avec l'id : " + room.getBuilding().getId()));
        room.setBuilding(building);

        if (room.getNumber() == null || room.getNumber() < 0) {
            log.warn("Validation rejetée : numéro de salle invalide ({})", room.getNumber());
            throw new ServiceException("Le numéro de la salle doit être positif.");
        }
        if (room.getNumber() > 999) {
            log.warn("Validation rejetée : numéro de salle supérieur à 999 ({})", room.getNumber());
            throw new ServiceException("Le numéro de la salle doit être inférieur à 999.");
        }
    }

    @Override
    @Transactional
    public Room update(final Room room) {
        log.debug("Demande de mise à jour de la salle [id={}, number={}]", room.getId(), room.getNumber());
        validateRoom(room);

        Room updated = super.update(room);
        log.info("Salle mise à jour avec succès [id={}, number={}]", updated.getId(), updated.getNumber());
        return updated;
    }

    @Override
    public Optional<Room> findByNumberAndBuilding(final Integer number, final Building building) {
        return this.repository.findByNumberAndBuilding(number, building);
    }
}
