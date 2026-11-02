package net.ent.etnc.services;

import net.ent.etnc.models.Material;
import net.ent.etnc.services.commons.Service;

/**
 * Business contract for rentable objects.
 */
public interface MaterialService extends Service<Material, Long> {

    /**
     * Effectue un soft delete en archivant le matériel et en libérant sa salle.
     */
    void archiveById(Long id);

    /**
     * Affecte un matériel à une salle (ou le désaffecte si roomId est null).
     */
    Material assignRoom(Long id, Long roomId);
}
