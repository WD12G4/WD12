package net.ent.etnc.services;

import net.ent.etnc.models.Sighting;
import net.ent.etnc.services.commons.Service;

import java.time.Instant;
import java.util.List;

/**
 * Contrat métier des constats de terrain (PWA RECON).
 */
public interface SightingService extends Service<Sighting, Long> {

    /**
     * Tous les constats, triés par salle puis par numéro de série.
     */
    List<Sighting> findAll();

    /**
     * Enregistre qu'un numéro a été scanné dans une salle : crée le constat, ou le met à jour s'il est plus récent.
     *
     * @param serialNumber numéro scanné (normalisé ici)
     * @param roomId       salle du scan
     * @param seenAt       date du scan (scan hors ligne envoyé plus tard), ou null pour maintenant
     * @param seenBy       agent connecté
     * @param replaces     numéro lu puis corrigé par l'agent, dont le constat est retiré, ou null
     */
    Sighting record(String serialNumber, Long roomId, Instant seenAt, String seenBy, String replaces);

    /**
     * Supprime les constats d'une salle (nouvelle reconnaissance).
     *
     * @return nombre de constats supprimés
     */
    int clearRoom(Long roomId);
}
