package net.ent.etnc.services;

import net.ent.etnc.models.Report;
import net.ent.etnc.services.commons.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface de service concernant les {@link Report}.
 */
public interface ReportService extends Service<Report, Long> {

    /**
     * Marque un signalement comme traité ou non traité (réservé aux gestionnaires et administrateurs).
     *
     * @param id identifiant du signalement
     * @param handled true si traité, false sinon
     * @return signalement mis à jour
     */
    Report markAsHandled(Long id, boolean handled);

    /**
     * Recherche les signalements associés à un matériel donné.
     */
    Page<Report> findByMaterialId(Long materialId, Pageable pageable);

    /**
     * Recherche les signalements créés par un utilisateur donné.
     */
    Page<Report> findByUserId(Long userId, Pageable pageable);

    /**
     * Recherche les signalements selon leur statut de traitement.
     */
    Page<Report> findByHandled(boolean handled, Pageable pageable);
}
