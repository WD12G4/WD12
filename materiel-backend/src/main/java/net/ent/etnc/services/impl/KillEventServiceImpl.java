package net.ent.etnc.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.models.KillEvent;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.repositories.KillEventRepository;
import net.ent.etnc.repositories.MaterialRepository;
import net.ent.etnc.services.KillEventService;
import net.ent.etnc.services.UserService;
import net.ent.etnc.services.commons.AbstractService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Service des {@link KillEvent}.
 */
@Slf4j
@Service
public class KillEventServiceImpl extends AbstractService<KillEvent, KillEventRepository> implements KillEventService {

    private final MaterialRepository materialRepository;
    private final UserService userService;

    /**
     * Constructeur du service.
     */
    @Autowired
    public KillEventServiceImpl(final KillEventRepository repository,
                                final MaterialRepository materialRepository,
                                final UserService userService) {
        super(repository);
        this.materialRepository = materialRepository;
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KillEvent> findAll(final Pageable pageable) {
        User currentUser = this.userService.currentUser();
        if (ERole.ADMIN.equals(currentUser.getRole())) {
            return this.repository.findAll(pageable);
        }
        return this.repository.findByUser_Id(currentUser.getId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KillEvent> findByUserId(final Long userId, final Pageable pageable) {
        User currentUser = this.userService.currentUser();
        if (ERole.ADMIN.equals(currentUser.getRole())) {
            return this.repository.findByUser_Id(userId, pageable);
        }
        if (!currentUser.getId().equals(userId)) {
            log.warn("Tentative d'accès non autorisée aux KillEvents de l'utilisateur {} par {}",
                    userId, currentUser.getUsername());
            throw new AccessDeniedException("Vous n'êtes pas autorisé à consulter les KillEvents d'un autre utilisateur.");
        }
        return this.repository.findByUser_Id(currentUser.getId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KillEvent> findById(final Long id) {
        Optional<KillEvent> killEventOpt = this.repository.findById(id);
        if (killEventOpt.isEmpty()) {
            return Optional.empty();
        }

        KillEvent killEvent = killEventOpt.get();
        User currentUser = this.userService.currentUser();
        if (ERole.ADMIN.equals(currentUser.getRole())
                || (killEvent.getUser() != null && killEvent.getUser().getId().equals(currentUser.getId()))) {
            return killEventOpt;
        }

        log.warn("Tentative d'accès non autorisée au KillEvent {} par {}", id, currentUser.getUsername());
        throw new AccessDeniedException("Vous n'avez pas accès à ce KillEvent.");
    }

    @Override
    @Transactional
    public KillEvent create(final KillEvent killEvent) {
        log.debug("Demande de création d'un Kill Event : {}", killEvent.getProcessName());

        if (killEvent.getUser() == null || killEvent.getUser().getId() == null) {
            User currentUser = this.userService.currentUser();
            killEvent.setUser(currentUser);
        } else {
            User user = this.userService.findById(killEvent.getUser().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id : " + killEvent.getUser().getId()));
            killEvent.setUser(user);
        }

        validateKillEvent(killEvent);
        KillEvent created = super.create(killEvent);
        log.info("Kill Event créé avec succès [processus={}, UC={}, utilisateur={}, date/heure={}, résultat={}]",
                created.getProcessName(), created.getUc(),
                created.getUser() != null ? created.getUser().getUsername() : "inconnu",
                created.getKillDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                created.getResult());
        return created;
    }

    @Override
    public KillEvent update(final KillEvent killEvent) {
        throw new UnsupportedOperationException("La mise à jour d'un Kill Event n'est pas autorisée.");
    }

    @Override
    @Transactional
    public void deleteById(final Long id) {
        throw new UnsupportedOperationException("La suppression d'un Kill Event n'est pas autorisée.");
    }

    private void validateKillEvent(final KillEvent killEvent) {
        String processName = killEvent.getProcessName();
        if (processName == null || processName.isBlank()) {
            log.warn("Validation rejetée : nom du processus manquant.");
            throw new ServiceException("Le nom du processus doit être renseigné.");
        }
        if (processName.length() > 100) {
            log.warn("Validation rejetée : nom du processus trop long (longueur={}).", processName.length());
            throw new ServiceException("Le nom du processus doit être inférieur à 100 caractères.");
        }
        if (killEvent.getUc() == null || killEvent.getUc().getId() == null) {
            log.warn("Validation rejetée : UC non renseignée.");
            throw new ServiceException("L'UC doit être renseignée.");
        }

        killEvent.setUc(validateMaterial(killEvent.getUc().getId()));

        if (killEvent.getUser() == null) {
            log.warn("Validation rejetée : utilisateur non renseigné.");
            throw new ServiceException("L'utilisateur doit être renseigné.");
        }

        if (killEvent.getKillDateTime() == null) {
            log.warn("Validation rejetée : heure/date non renseignée.");
            throw new ServiceException("L'heure et la date doivent être renseignées.");
        }
        if (killEvent.getKillDateTime().isAfter(LocalDateTime.now())) {
            log.warn("Validation rejetée : date/heure d'arrêt du processus ({}) postérieure au jour/à l'heure actuelle.",
                    killEvent.getKillDateTime());
            throw new ServiceException("La date/heure d'arrêt d'un processus ne peut pas être après le jour/l'heure actuelle.");
        }
        if (killEvent.getResult() == null) {
            log.warn("Validation rejetée : résultat de l'arrêt non renseigné.");
            throw new ServiceException("Le résultat de l'arrêt doit être renseigné.");
        }
    }

    private Material validateMaterial(final Long id) {
        Material uc = this.materialRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Validation rejetée : UC manquante dans la base de données (id={}).", id);
                    return new ResourceNotFoundException("UC non trouvée avec l'id : " + id);
                });

        if (!EMaterialType.UC.equals(uc.getMaterialType())) {
            log.warn("Validation rejetée : Le matériel n'est pas une UC.");
            throw new ServiceException("Le matériel doit être une UC pour avoir un kill event.");
        }
        return uc;
    }
}
