package net.ent.etnc.services.impl;

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
        if (findByNumber(building.getNumber()).isPresent()) {
            throw new ServiceException(String.format("Le bâtiment numéro %s éxiste déjà.", building.getNumber()));
        }
        validateBuilding(building);
        return super.create(building);
    }

    private void validateBuilding(final Building building) {
        if (building.getNumber() == null || building.getNumber() < 0) {
            throw new ServiceException("La numéro de bâtiment doit être positif.");
        }
        if (building.getNumber() > 999) {
            throw new ServiceException("La numéro de bâtiment doit être inférieur à 999.");
        }
    }

    @Override
    public Building update(final Building building) {
        validateBuilding(building);
        return super.update(building);
    }

    @Override
    public Optional<Building> findByNumber(final Integer number) {
        return this.repository.findByNumber(number);
    }
}
