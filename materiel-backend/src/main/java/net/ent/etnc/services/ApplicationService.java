package net.ent.etnc.services;

import net.ent.etnc.models.Application;
import net.ent.etnc.services.commons.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Contrat de service pour la gestion des applications et des règles de liste noire.
 */
public interface ApplicationService extends Service<Application, Long> {

    /**
     * Recherche une application par son nom (insensible à la casse).
     */
    Optional<Application> findByName(String name);

    /**
     * Vérifie si un processus est sur la liste noire.
     */
    boolean isBlacklisted(String processName);

    /**
     * Modifie le statut de liste noire d'une application.
     */
    Application toggleBlacklist(Long id, boolean blacklisted);

    /**
     * Enregistre ou met à jour une application découverte par un agent.
     */
    Application registerDiscoveredApplication(String processName, String iconBase64);

    /**
     * Récupère la liste paginée des applications avec filtrage optionnel sur le statut blacklisté.
     */
    Page<Application> findAll(Pageable pageable, Boolean blacklisted);
}
