package net.ent.etnc.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.models.Building;
import net.ent.etnc.repositories.BuildingRepository;
import net.ent.etnc.services.BuildingService;
import net.ent.etnc.services.commons.AbstractService;
import net.ent.etnc.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service des {@link Building}.
 */
@Slf4j
@Service
public class BuildingServiceImpl extends AbstractService<Building, BuildingRepository> implements BuildingService {

    /**
     * Constructeur du service.
     */
    @Autowired
    public BuildingServiceImpl(final BuildingRepository repository) {
        super(repository);
    }

    @Override
    @Transactional
    public Building create(final Building building) {
        log.debug("Demande de création d'un bâtiment avec le numéro : {}", building.getNumber());

        if (findByNumber(building.getNumber()).isPresent()) {
            log.warn("Échec création bâtiment : le bâtiment numéro {} existe déjà.", building.getNumber());
            throw new ServiceException(String.format("Le bâtiment numéro %s existe déjà.", building.getNumber()));
        }
        validateBuilding(building);

        Building created = super.create(building);
        log.info("Bâtiment créé avec succès [id={}, number={}]", created.getId(), created.getNumber());
        return created;
    }

    private void validateBuilding(final Building building) {
        if (building.getNumber() == null || building.getNumber() < 0) {
            log.warn("Validation rejetée : numéro de bâtiment invalide ou négatif ({})", building.getNumber());
            throw new ServiceException("Le numéro de bâtiment doit être positif.");
        }
        if (building.getNumber() > 999) {
            log.warn("Validation rejetée : numéro de bâtiment supérieur à 999 ({})", building.getNumber());
            throw new ServiceException("Le numéro de bâtiment doit être inférieur à 999.");
        }
    }

    @Override
    @Transactional
    public Building update(final Building building) {
        log.debug("Demande de mise à jour du bâtiment [id={}, number={}]", building.getId(), building.getNumber());
        validateBuilding(building);

        Building updated = super.update(building);
        log.info("Bâtiment mis à jour avec succès [id={}, number={}]", updated.getId(), updated.getNumber());
        return updated;
    }

    @Override
    public Optional<Building> findByNumber(final Integer number) {
        return this.repository.findByNumber(number);
    }
}
