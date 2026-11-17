package net.ent.etnc.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.models.Building;
import net.ent.etnc.models.Room;
import net.ent.etnc.repositories.MaterialRepository;
import net.ent.etnc.repositories.RoomRepository;
import net.ent.etnc.services.BuildingService;
import net.ent.etnc.services.RoomService;
import net.ent.etnc.services.commons.AbstractService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
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
     * Accès données pour les {@link net.ent.etnc.models.Material}.
     */
    private final MaterialRepository materialRepository;

    /**
     * Constructeur du service.
     */
    @Autowired
    public RoomServiceImpl(final RoomRepository repository,
                           final BuildingService buildingService,
                           final MaterialRepository materialRepository) {
        super(repository);
        this.buildingService = buildingService;
        this.materialRepository = materialRepository;
    }

    @Override
    @Transactional
    public Room create(final Room room) {
        log.debug("Demande de création d'une salle : numéro={}, bâtiment_id={}",
                room.getNumber(), room.getBuilding() != null ? room.getBuilding().getId() : null);

        validateRoom(room, null);

        Room created = super.create(room);
        log.info("Salle créée avec succès [id={}, number={}, buildingNumber={}]",
                created.getId(), created.getNumber(), created.getBuilding().getNumber());
        return created;
    }

    @Override
    @Transactional
    public Room update(final Room room) {
        log.debug("Demande de mise à jour de la salle [id={}, number={}]", room.getId(), room.getNumber());
        validateRoom(room, room.getId());

        Room updated = super.update(room);
        log.info("Salle mise à jour avec succès [id={}, number={}]", updated.getId(), updated.getNumber());
        return updated;
    }

    private void validateRoom(final Room room, final Long currentId) {
        if (room.getBuilding() == null || room.getBuilding().getId() == null) {
            log.warn("Validation rejetée : bâtiment non renseigné pour la salle.");
            throw new ServiceException("Le bâtiment doit être renseigné.");
        }

        Building building = this.buildingService.findById(room.getBuilding().getId())
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

        findByNumberAndBuilding(room.getNumber(), room.getBuilding()).ifPresent(other -> {
            if (!Objects.equals(currentId, other.getId())) {
                log.warn("Validation rejetée : la salle numéro {} existe déjà dans le bâtiment {}",
                        room.getNumber(), room.getBuilding().getNumber());
                throw new ServiceException(String.format("La salle numéro %s existe déjà dans le bâtiment %s.",
                        room.getNumber(), room.getBuilding().getNumber()));
            }
        });
    }

    @Override
    @Transactional
    public void deleteById(final Long id) {
        log.debug("Demande de suppression de la salle [id={}]", id);

        if (!existsById(id)) {
            log.warn("Échec suppression salle : salle introuvable avec l'id : {}", id);
            throw new ResourceNotFoundException("Salle introuvable avec l'id : " + id);
        }

        if (this.materialRepository.existsByRoomId(id)) {
            log.warn("Échec suppression salle [id={}] : la salle contient encore du matériel.", id);
            throw new ServiceException("Impossible de supprimer la salle : elle contient encore du matériel.");
        }

        super.deleteById(id);
        log.info("Salle supprimée avec succès [id={}]", id);
    }

    @Override
    public Optional<Room> findByNumberAndBuilding(final Integer number, final Building building) {
        return this.repository.findByNumberAndBuilding(number, building);
    }
}
