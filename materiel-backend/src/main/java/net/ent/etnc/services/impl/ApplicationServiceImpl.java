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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Implémentation du service {@link ApplicationService}.
 * RG-27 : L'administration de la liste noire est réservée à l'administrateur.
 */
@Slf4j
@Service
public class ApplicationServiceImpl extends AbstractService<Application, ApplicationRepository> implements ApplicationService {

    /**
     * Composants système vitaux et processus protégés qu'il est formellement interdit de blacklister.
     */
    private static final Set<String> PROTECTED_PROCESS_NAMES = new HashSet<>();

    static {
        // Windows - Processus système et d'arrière-plan
        PROTECTED_PROCESS_NAMES.add("system");
        PROTECTED_PROCESS_NAMES.add("registry");
        PROTECTED_PROCESS_NAMES.add("memory compression");
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
        PROTECTED_PROCESS_NAMES.add("startmenuexperiencehost");
        PROTECTED_PROCESS_NAMES.add("startmenuexperiencehost.exe");
        PROTECTED_PROCESS_NAMES.add("searchhost");
        PROTECTED_PROCESS_NAMES.add("searchhost.exe");
        PROTECTED_PROCESS_NAMES.add("minisearchhost");
        PROTECTED_PROCESS_NAMES.add("minisearchhost.exe");
        PROTECTED_PROCESS_NAMES.add("searchindexer");
        PROTECTED_PROCESS_NAMES.add("searchindexer.exe");
        PROTECTED_PROCESS_NAMES.add("shellexperiencehost");
        PROTECTED_PROCESS_NAMES.add("shellexperiencehost.exe");
        PROTECTED_PROCESS_NAMES.add("applicationframehost");
        PROTECTED_PROCESS_NAMES.add("applicationframehost.exe");
        PROTECTED_PROCESS_NAMES.add("ctfmon");
        PROTECTED_PROCESS_NAMES.add("ctfmon.exe");
        PROTECTED_PROCESS_NAMES.add("logonui");
        PROTECTED_PROCESS_NAMES.add("logonui.exe");
        PROTECTED_PROCESS_NAMES.add("smartscreen");
        PROTECTED_PROCESS_NAMES.add("smartscreen.exe");
        PROTECTED_PROCESS_NAMES.add("securityhealthservice");
        PROTECTED_PROCESS_NAMES.add("securityhealthservice.exe");
        PROTECTED_PROCESS_NAMES.add("securityhealthsystray");
        PROTECTED_PROCESS_NAMES.add("securityhealthsystray.exe");
        PROTECTED_PROCESS_NAMES.add("msmpeng");
        PROTECTED_PROCESS_NAMES.add("msmpeng.exe");
        PROTECTED_PROCESS_NAMES.add("nissrv");
        PROTECTED_PROCESS_NAMES.add("nissrv.exe");
        PROTECTED_PROCESS_NAMES.add("sgrmbroker");
        PROTECTED_PROCESS_NAMES.add("sgrmbroker.exe");
        PROTECTED_PROCESS_NAMES.add("aggregatorhost");
        PROTECTED_PROCESS_NAMES.add("aggregatorhost.exe");
        PROTECTED_PROCESS_NAMES.add("usoclient");
        PROTECTED_PROCESS_NAMES.add("usoclient.exe");
        PROTECTED_PROCESS_NAMES.add("mousocoreworker");
        PROTECTED_PROCESS_NAMES.add("mousocoreworker.exe");
        PROTECTED_PROCESS_NAMES.add("wudfhost");
        PROTECTED_PROCESS_NAMES.add("wudfhost.exe");

        // Windows - Processus réseau, pilotes et sous-systèmes
        PROTECTED_PROCESS_NAMES.add("conhost");
        PROTECTED_PROCESS_NAMES.add("conhost.exe");
        PROTECTED_PROCESS_NAMES.add("dllhost");
        PROTECTED_PROCESS_NAMES.add("dllhost.exe");
        PROTECTED_PROCESS_NAMES.add("rundll32");
        PROTECTED_PROCESS_NAMES.add("rundll32.exe");
        PROTECTED_PROCESS_NAMES.add("dashost");
        PROTECTED_PROCESS_NAMES.add("dashost.exe");
        PROTECTED_PROCESS_NAMES.add("audiodg");
        PROTECTED_PROCESS_NAMES.add("audiodg.exe");

        // Composants et agent de supervision WD12
        PROTECTED_PROCESS_NAMES.add("materielagent");
        PROTECTED_PROCESS_NAMES.add("materielagent.exe");
        PROTECTED_PROCESS_NAMES.add("materiel-agent");
        PROTECTED_PROCESS_NAMES.add("materiel-agent.exe");
        PROTECTED_PROCESS_NAMES.add("wd12agent");
        PROTECTED_PROCESS_NAMES.add("wd12agent.exe");

        // Linux - Daemons et services vitaux
        PROTECTED_PROCESS_NAMES.add("systemd");
        PROTECTED_PROCESS_NAMES.add("init");
        PROTECTED_PROCESS_NAMES.add("kthreadd");
        PROTECTED_PROCESS_NAMES.add("systemd-journald");
        PROTECTED_PROCESS_NAMES.add("systemd-udevd");
        PROTECTED_PROCESS_NAMES.add("systemd-logind");
        PROTECTED_PROCESS_NAMES.add("systemd-resolved");
        PROTECTED_PROCESS_NAMES.add("systemd-timesyncd");
        PROTECTED_PROCESS_NAMES.add("systemd-networkd");
        PROTECTED_PROCESS_NAMES.add("dbus-daemon");
        PROTECTED_PROCESS_NAMES.add("dbus-broker");
        PROTECTED_PROCESS_NAMES.add("sshd");
        PROTECTED_PROCESS_NAMES.add("cron");
        PROTECTED_PROCESS_NAMES.add("crond");
        PROTECTED_PROCESS_NAMES.add("rsyslogd");
        PROTECTED_PROCESS_NAMES.add("networkmanager");
        PROTECTED_PROCESS_NAMES.add("polkitd");
        PROTECTED_PROCESS_NAMES.add("accounts-daemon");
        PROTECTED_PROCESS_NAMES.add("gdm");
        PROTECTED_PROCESS_NAMES.add("gdm3");
        PROTECTED_PROCESS_NAMES.add("lightdm");
        PROTECTED_PROCESS_NAMES.add("sddm");
        PROTECTED_PROCESS_NAMES.add("xorg");
        PROTECTED_PROCESS_NAMES.add("xwayland");

        // macOS - Daemons et processus essentiels
        PROTECTED_PROCESS_NAMES.add("launchd");
        PROTECTED_PROCESS_NAMES.add("kernel_task");
        PROTECTED_PROCESS_NAMES.add("windowserver");
        PROTECTED_PROCESS_NAMES.add("loginwindow");
        PROTECTED_PROCESS_NAMES.add("syslogd");
        PROTECTED_PROCESS_NAMES.add("configd");
        PROTECTED_PROCESS_NAMES.add("coreaudiod");
        PROTECTED_PROCESS_NAMES.add("diskarbitrationd");
        PROTECTED_PROCESS_NAMES.add("distnoted");
        PROTECTED_PROCESS_NAMES.add("fseventsd");
        PROTECTED_PROCESS_NAMES.add("securityd");
        PROTECTED_PROCESS_NAMES.add("trustd");
        PROTECTED_PROCESS_NAMES.add("tccd");
        PROTECTED_PROCESS_NAMES.add("dock");
        PROTECTED_PROCESS_NAMES.add("finder");
        PROTECTED_PROCESS_NAMES.add("controlcenter");
        PROTECTED_PROCESS_NAMES.add("notificationcenter");
        PROTECTED_PROCESS_NAMES.add("spotlight");
        PROTECTED_PROCESS_NAMES.add("mds");
        PROTECTED_PROCESS_NAMES.add("opendirectoryd");
    }

    @Autowired
    public ApplicationServiceImpl(final ApplicationRepository repository) {
        super(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Application> findByName(final String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return this.repository.findByNameIgnoreCase(name.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlacklisted(final String processName) {
        if (processName == null || processName.isBlank()) {
            return false;
        }
        return this.repository.findByNameIgnoreCase(processName.trim()).map(Application::isBlacklisted).orElse(false);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Application toggleBlacklist(final Long id, final boolean blacklisted) {
        Application app = this.repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Application introuvable avec l'id : " + id));

        if (blacklisted && isProtectedSystemProcess(app.getName())) {
            log.warn("Tentative d'interdiction rejetée pour le composant système protégé [{}]", app.getName());
            throw new ServiceException("Impossible d'interdire un composant système protégé (" + app.getName() + ").");
        }

        app.setBlacklisted(blacklisted);
        log.info("Statut de liste noire mis à jour pour l'application [{}] : blacklisted={}", app.getName(), blacklisted);
        return this.repository.saveAndFlush(app);
    }

    @Override
    @Transactional
    public Application registerDiscoveredApplication(final String processName, final String iconBase64) {
        if (processName == null || processName.isBlank()) {
            return null;
        }

        String normalizedName = processName.trim();
        Optional<Application> existingOpt = this.repository.findByNameIgnoreCase(normalizedName);

        if (existingOpt.isPresent()) {
            Application existing = existingOpt.get();
            // Mettre à jour l'icône si elle était absente et est désormais fournie
            if (iconBase64 != null && !iconBase64.isBlank() && (existing.getIconBase64() == null || existing.getIconBase64().isBlank())) {
                existing.setIconBase64(iconBase64);
                return this.repository.saveAndFlush(existing);
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
        return this.repository.saveAndFlush(app);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Application> findAll(final Pageable pageable, final Boolean blacklisted) {
        if (blacklisted != null) {
            return this.repository.findByBlacklisted(blacklisted, pageable);
        }
        return this.repository.findAll(pageable);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Application create(final Application entity) {
        validateApplication(entity);
        return super.create(entity);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Application update(final Application entity) {
        validateApplication(entity);
        return super.update(entity);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteById(final Long id) {
        super.deleteById(id);
    }

    private void validateApplication(final Application app) {
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

    private boolean isProtectedSystemProcess(final String processName) {
        if (processName == null) {
            return false;
        }
        String clean = processName.trim().toLowerCase();
        return PROTECTED_PROCESS_NAMES.contains(clean) || PROTECTED_PROCESS_NAMES.contains(clean.replace(".exe", ""));
    }

    private String formatDisplayName(final String processName) {
        String clean = processName.replaceAll("(?i)\\.exe$", "");
        if (clean.isEmpty()) {
            return processName;
        }
        return Character.toUpperCase(clean.charAt(0)) + (clean.length() > 1 ? clean.substring(1) : "");
    }
}
