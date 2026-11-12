package net.ent.etnc.services;

import net.ent.etnc.models.KillEvent;
import net.ent.etnc.services.commons.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface de service concernant les {@link KillEvent}.
 */
public interface KillEventService extends Service<KillEvent, Long> {

    /**
     * Recherche les événements d'arrêt pour un utilisateur donné.
     *
     * @param userId identifiant de l'utilisateur
     * @param pageable pagination
     * @return page de KillEvent
     */
    Page<KillEvent> findByUserId(Long userId, Pageable pageable);
}
