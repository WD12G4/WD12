package net.ent.etnc.services;

import net.ent.etnc.models.Building;
import net.ent.etnc.services.commons.Service;

import java.util.Optional;

/**
 * Business contract for rental lifecycle operations.
 */
public interface BuildingService extends Service<Building, Long> {

    Optional<Building> findByNumber(final Integer number);
}
