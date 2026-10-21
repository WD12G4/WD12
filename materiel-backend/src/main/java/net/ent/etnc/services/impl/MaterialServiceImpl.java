package net.ent.etnc.services.impl;

import net.ent.etnc.models.Material;
import net.ent.etnc.models.Room;
import net.ent.etnc.repositories.MaterialRepository;
import net.ent.etnc.services.MaterialService;
import net.ent.etnc.services.commons.AbstractService;
import net.ent.etnc.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Service des {@link Room}.
 */
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
        if (material.getSerialNumber() != null && this.repository.existsBySerialNumber(material.getSerialNumber())) {
            throw new ServiceException(String.format("Le numéro de série %s éxiste déjà", material.getSerialNumber()));
        }
        validateMaterial(material);
        return super.create(material);
    }

    private void validateMaterial(final Material material) {
        if (material.getSerialNumber() == null) {
            throw new ServiceException("La numéro de la salle doit être renseigné.");
        }
        if (material.getSerialNumber().length() > 100) {
            throw new ServiceException("La numéro de la salle doit être inférieur à 100 caractères.");
        }
        if (material.getMaterialType() == null) {
            throw new ServiceException("Le type de matériel doit être renseigné.");
        }
        if (material.getEndAvailabilityDate().isBefore(LocalDate.now()) && !material.isArchived()) {
            throw new ServiceException("La date de disponibilité ne peut pas être avant le jour en cours si le matériel n'est pas archivé (soft delete).");
        }
        if (material.isArchived() && material.getRoom() != null) {
            throw new ServiceException("Le matériel ne peut pas être affecter à une salle si il est archivé.");
        }
    }

    @Override
    public Material update(final Material material) {
        validateMaterial(material);
        return super.update(material);
    }


}
