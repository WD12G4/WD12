package net.ent.etnc.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.Report;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.repositories.MaterialRepository;
import net.ent.etnc.repositories.ReportRepository;
import net.ent.etnc.services.ReportService;
import net.ent.etnc.services.UserService;
import net.ent.etnc.services.commons.AbstractService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service des {@link Report}.
 */
@Slf4j
@Service
public class ReportServiceImpl extends AbstractService<Report, ReportRepository> implements ReportService {

    private final MaterialRepository materialRepository;
    private final UserService userService;

    @Autowired
    public ReportServiceImpl(final ReportRepository repository,
                             final MaterialRepository materialRepository,
                             final UserService userService) {
        super(repository);
        this.materialRepository = materialRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public Report create(final Report report) {
        log.debug("Demande de création d'un signalement.");

        User currentUser = this.userService.currentUser();
        report.setUser(currentUser);

        if (report.getMaterial() == null || report.getMaterial().getId() == null) {
            log.warn("Validation rejetée : matériel manquant.");
            throw new ServiceException("Le matériel doit être renseigné.");
        }

        Material material = this.materialRepository.findById(report.getMaterial().getId())
                .orElseThrow(() -> {
                    log.warn("Validation rejetée : matériel introuvable (id={}).", report.getMaterial().getId());
                    return new ResourceNotFoundException("Matériel introuvable avec l'id : " + report.getMaterial().getId());
                });
        report.setMaterial(material);

        report.setReportDateTime(LocalDateTime.now());
        report.setHandled(false);
        report.setHandleDateTime(null);

        validateReport(report);

        Report created = super.create(report);
        log.info("Signalement créé avec succès [id={}, user={}, material={}]",
                created.getId(), currentUser.getUsername(), material.getSerialNumber());
        return created;
    }

    @Override
    @Transactional
    public Report update(final Report report) {
        if (report.getId() == null) {
            throw new ResourceNotFoundException("Signalement introuvable.");
        }

        Report existing = this.repository.findById(report.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable avec l'id : " + report.getId()));

        User currentUser = this.userService.currentUser();
        if (!existing.getUser().getId().equals(currentUser.getId())) {
            log.warn("Tentative de modification non autorisée du signalement {} par l'utilisateur {}",
                    report.getId(), currentUser.getUsername());
            throw new AccessDeniedException("Seul l'utilisateur ayant créé le signalement peut le modifier.");
        }

        if (existing.isHandled()) {
            log.warn("Tentative de modification d'un signalement déjà traité (id={}).", report.getId());
            throw new ServiceException("Impossible de modifier un signalement déjà traité.");
        }

        // L'utilisateur créateur, le matériel, la date initiale et le statut handled ne peuvent pas être altérés
        report.setUser(existing.getUser());
        report.setMaterial(existing.getMaterial());
        report.setReportDateTime(existing.getReportDateTime());
        report.setHandled(existing.isHandled());
        report.setHandleDateTime(existing.getHandleDateTime());

        validateReport(report);

        Report updated = super.update(report);
        log.info("Signalement [id={}] mis à jour avec succès par l'utilisateur {}", updated.getId(), currentUser.getUsername());
        return updated;
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public Report markAsHandled(final Long id, final boolean handled) {
        log.debug("Modification de l'état handled du signalement [id={}, handled={}]", id, handled);
        Report existing = this.repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable avec l'id : " + id));

        existing.setHandled(handled);
        existing.setHandleDateTime(handled ? LocalDateTime.now() : null);

        Report updated = this.repository.saveAndFlush(existing);
        log.info("Signalement [id={}] marqué comme handled={}", id, handled);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Report> findByMaterialId(final Long materialId, final Pageable pageable) {
        return this.repository.findByMaterial_Id(materialId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Report> findByUserId(final Long userId, final Pageable pageable) {
        return this.repository.findByUser_Id(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Report> findByHandled(final boolean handled, final Pageable pageable) {
        return this.repository.findByHandled(handled, pageable);
    }

    @Override
    @Transactional
    public void deleteById(final Long id) {
        Report existing = this.repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable avec l'id : " + id));

        User currentUser = this.userService.currentUser();
        boolean isManagerOrAdmin = currentUser.getRole().hasRole(ERole.MANAGER);
        boolean isCreator = existing.getUser().getId().equals(currentUser.getId());

        if (!isManagerOrAdmin && !isCreator) {
            log.warn("Tentative de suppression non autorisée du signalement {} par l'utilisateur {}",
                    id, currentUser.getUsername());
            throw new AccessDeniedException("Vous n'êtes pas autorisé à supprimer ce signalement.");
        }

        super.deleteById(id);
        log.info("Signalement [id={}] supprimé par l'utilisateur {}", id, currentUser.getUsername());
    }

    private void validateReport(final Report report) {
        if (report.getDescription() == null || report.getDescription().isBlank()) {
            log.warn("Validation rejetée : description manquante.");
            throw new ServiceException("La description doit être renseignée.");
        }
        if (report.getDescription().length() > 500) {
            log.warn("Validation rejetée : description trop longue (longueur={}).", report.getDescription().length());
            throw new ServiceException("La description ne doit pas dépasser 500 caractères.");
        }
    }
}
