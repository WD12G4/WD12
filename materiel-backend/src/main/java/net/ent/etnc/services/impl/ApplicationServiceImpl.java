package net.ent.etnc.services.impl;

import lombok.extern.slf4j.Slf4j;
import net.ent.etnc.models.Application;
import net.ent.etnc.repositories.ApplicationRepository;
import net.ent.etnc.services.ApplicationService;
import net.ent.etnc.services.commons.AbstractService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Implémentation du service {@link ApplicationService}.
 */
@Slf4j
@Service
public class ApplicationServiceImpl extends AbstractService<Application, ApplicationRepository>
        implements ApplicationService {

    /**
     * Composants système vitaux protégés qu'il est formellement interdit de blacklister.
     */
    private static final Set<String> PROTECTED_PROCESS_NAMES = new HashSet<>();

    static {
        // Windows
        PROTECTED_PROCESS_NAMES.add("system");
        PROTECTED_PROCESS_NAMES.add("registry");
        PROTECTED_PROCESS_NAMES.add("smss");
        PROTECTED_PROCESS_NAMES.add("smss.exe");
        PROTECTED_PROCESS_NAMES.add("csrss");
        PROTECTED_PROCESS_NAMES.add("csrss.exe");
        PROTECTED_PROCESS_NAMES.add("wininit");
        PROTECTED_PROCESS_NAMES.add("wininit.exe");
        PROTECTED_PROCESS_NAMES.add("services");
        PROTECTED_PROCESS_NAMES.add("services.exe");
        PROTECTED_PROCESS_NAMES.add("lsass");
        PROTECTED_PROCESS_NAMES.add("lsass.exe");
        PROTECTED_PROCESS_NAMES.add("svchost");
        PROTECTED_PROCESS_NAMES.add("svchost.exe");
        PROTECTED_PROCESS_NAMES.add("fontdrvhost");
        PROTECTED_PROCESS_NAMES.add("fontdrvhost.exe");
        PROTECTED_PROCESS_NAMES.add("winlogon");
        PROTECTED_PROCESS_NAMES.add("winlogon.exe");
        PROTECTED_PROCESS_NAMES.add("dwm");
        PROTECTED_PROCESS_NAMES.add("dwm.exe");
        PROTECTED_PROCESS_NAMES.add("explorer");
        PROTECTED_PROCESS_NAMES.add("explorer.exe");
        PROTECTED_PROCESS_NAMES.add("spoolsv");
        PROTECTED_PROCESS_NAMES.add("spoolsv.exe");
        PROTECTED_PROCESS_NAMES.add("taskhostw");
        PROTECTED_PROCESS_NAMES.add("taskhostw.exe");
        PROTECTED_PROCESS_NAMES.add("sihost");
        PROTECTED_PROCESS_NAMES.add("sihost.exe");
        PROTECTED_PROCESS_NAMES.add("runtimebroker");
        PROTECTED_PROCESS_NAMES.add("runtimebroker.exe");
        PROTECTED_PROCESS_NAMES.add("taskmgr");
        PROTECTED_PROCESS_NAMES.add("taskmgr.exe");
        PROTECTED_PROCESS_NAMES.add("conhost");
        PROTECTED_PROCESS_NAMES.add("conhost.exe");
        PROTECTED_PROCESS_NAMES.add("msmpeng");
        PROTECTED_PROCESS_NAMES.add("msmpeng.exe");

        // Linux
        PROTECTED_PROCESS_NAMES.add("systemd");
        PROTECTED_PROCESS_NAMES.add("init");
        PROTECTED_PROCESS_NAMES.add("sshd");
        PROTECTED_PROCESS_NAMES.add("xorg");
        PROTECTED_PROCESS_NAMES.add("wayland");

        // macOS
        PROTECTED_PROCESS_NAMES.add("launchd");
        PROTECTED_PROCESS_NAMES.add("kernel_task");
        PROTECTED_PROCESS_NAMES.add("windowserver");
        PROTECTED_PROCESS_NAMES.add("finder");
        PROTECTED_PROCESS_NAMES.add("dock");
    }

    @Autowired
    public ApplicationServiceImpl(ApplicationRepository repository) {
        super(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Application> findByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return repository.findByNameIgnoreCase(name.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String processName) {
        if (processName == null || processName.isBlank()) {
            return false;
        }
        return repository.findByNameIgnoreCase(processName.trim())
                .map(Application::isBlacklisted)
                .orElse(false);
    }

    @Override
    @Transactional
    public Application toggleBlacklist(Long id, boolean blacklisted) {
        Application app = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application introuvable avec l'id : " + id));

        if (blacklisted && isProtectedSystemProcess(app.getName())) {
            log.warn("Tentative d'interdiction rejetée pour le composant système protégé [{}]", app.getName());
            throw new ServiceException("Impossible d'interdire un composant système protégé (" + app.getName() + ").");
        }

        app.setBlacklisted(blacklisted);
        log.info("Statut de liste noire mis à jour pour l'application [{}] : blacklisted={}", app.getName(), blacklisted);
        return repository.saveAndFlush(app);
    }

    @Override
    @Transactional
    public Application registerDiscoveredApplication(String processName, String iconBase64) {
        if (processName == null || processName.isBlank()) {
            return null;
        }

        String normalizedName = processName.trim();
        Optional<Application> existingOpt = repository.findByNameIgnoreCase(normalizedName);

        if (existingOpt.isPresent()) {
            Application existing = existingOpt.get();
            // Mettre à jour l'icône si elle était absente et est désormais fournie
            if (iconBase64 != null && !iconBase64.isBlank() && (existing.getIconBase64() == null || existing.getIconBase64().isBlank())) {
                existing.setIconBase64(iconBase64);
                return repository.saveAndFlush(existing);
            }
            return existing;
        }

        // Création de l'application auto-découverte
        Application app = new Application();
        app.setName(normalizedName);
        app.setDisplayName(formatDisplayName(normalizedName));
        app.setIconBase64(iconBase64);
        app.setBlacklisted(false);

        log.info("Nouvelle application découverte et enregistrée dans le catalogue : [{}]", normalizedName);
        return repository.saveAndFlush(app);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Application> findAll(Pageable pageable, Boolean blacklisted) {
        if (blacklisted != null) {
            return repository.findByBlacklisted(blacklisted, pageable);
        }
        return repository.findAll(pageable);
    }

    @Override
    @Transactional
    public Application create(Application entity) {
        validateApplication(entity);
        return super.create(entity);
    }

    @Override
    @Transactional
    public Application update(Application entity) {
        validateApplication(entity);
        return super.update(entity);
    }

    private void validateApplication(Application app) {
        if (app == null) {
            throw new ServiceException("L'application ne peut pas être null.");
        }
        if (app.getName() == null || app.getName().isBlank()) {
            throw new ServiceException("Le nom de l'application doit être renseigné.");
        }
        if (app.isBlacklisted() && isProtectedSystemProcess(app.getName())) {
            throw new ServiceException("Impossible d'interdire un composant système protégé (" + app.getName() + ").");
        }
    }

    private boolean isProtectedSystemProcess(String processName) {
        if (processName == null) return false;
        String clean = processName.trim().toLowerCase();
        return PROTECTED_PROCESS_NAMES.contains(clean) || PROTECTED_PROCESS_NAMES.contains(clean.replace(".exe", ""));
    }

    private String formatDisplayName(String processName) {
        String clean = processName.replaceAll("(?i)\\.exe$", "");
        if (clean.isEmpty()) return processName;
        return Character.toUpperCase(clean.charAt(0)) + (clean.length() > 1 ? clean.substring(1) : "");
    }
}
