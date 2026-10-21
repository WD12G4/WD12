package net.ent.etnc.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.models.Material;
import net.ent.etnc.repositories.MaterialRepository;
import net.ent.etnc.services.MaterialService;
import net.ent.etnc.services.commons.AbstractService;
import net.ent.etnc.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Service des {@link Material}.
 */
@Slf4j
@Service
public class MaterialServiceImpl extends AbstractService<Material, MaterialRepository> implements MaterialService {

    /**
     * Constructeur du service.
     */
    @Autowired
    public MaterialServiceImpl(final MaterialRepository repository) {
        super(repository);
    }

    @Override
    @Transactional
    public Material create(final Material material) {
        log.debug("Demande de création d'un matériel avec numéro de série : {}", material.getSerialNumber());

        if (material.getSerialNumber() != null && this.repository.existsBySerialNumber(material.getSerialNumber())) {
            log.warn("Échec création matériel : le numéro de série {} existe déjà.", material.getSerialNumber());
            throw new ServiceException(String.format("Le numéro de série %s existe déjà.", material.getSerialNumber()));
        }

        validateMaterial(material);

        Material created = super.create(material);
        log.info("Matériel créé avec succès [id={}, serialNumber={}, type={}]",
                created.getId(), created.getSerialNumber(), created.getMaterialType());
        return created;
    }

    @Override
    @Transactional
    public Material update(final Material material) {
        log.debug("Demande de mise à jour du matériel [id={}, serialNumber={}]", material.getId(), material.getSerialNumber());
        validateMaterial(material);

        Material updated = super.update(material);
        log.info("Matériel mis à jour avec succès [id={}]", updated.getId());
        return updated;
    }

    private void validateMaterial(final Material material) {
        if (material.getSerialNumber() == null) {
            log.warn("Validation rejetée : numéro de série manquant.");
            throw new ServiceException("Le numéro de série doit être renseigné.");
        }
        if (material.getSerialNumber().length() > 100) {
            log.warn("Validation rejetée : numéro de série trop long (longueur={}).", material.getSerialNumber().length());
            throw new ServiceException("Le numéro de série doit être inférieur à 100 caractères.");
        }
        if (material.getMaterialType() == null) {
            log.warn("Validation rejetée : type de matériel manquant.");
            throw new ServiceException("Le type de matériel doit être renseigné.");
        }
        if (material.getEndAvailabilityDate().isBefore(LocalDate.now()) && !material.isArchived()) {
            log.warn("Validation rejetée : date de fin de disponibilité ({}) antérieure au jour en cours pour un matériel non archivé.",
                    material.getEndAvailabilityDate());
            throw new ServiceException("La date de disponibilité ne peut pas être avant le jour en cours si le matériel n'est pas archivé (soft delete).");
        }
        if (material.isArchived() && material.getRoom() != null) {
            log.warn("Validation rejetée : tentative d'affecter un matériel archivé à une salle (room_id={}).",
                    material.getRoom().getId());
            throw new ServiceException("Le matériel ne peut pas être affecté à une salle s'il est archivé.");
        }
    }
}
