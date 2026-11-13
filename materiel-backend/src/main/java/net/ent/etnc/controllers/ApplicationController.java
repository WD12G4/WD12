package net.ent.etnc.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.dtos.application.ApplicationBlacklistRequest;
import net.ent.etnc.dtos.application.ApplicationRequest;
import net.ent.etnc.dtos.application.ApplicationResponse;
import net.ent.etnc.dtos.assemblers.ApplicationDtoAssembler;
import net.ent.etnc.models.Application;
import net.ent.etnc.services.ApplicationService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Point d'entrée REST pour la gestion du catalogue d'applications et des règles de liste noire.
 */
@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationDtoAssembler applicationAssembler;

    @Autowired
    public ApplicationController(final ApplicationService applicationService, final ApplicationDtoAssembler applicationAssembler) {
        this.applicationService = applicationService;
        this.applicationAssembler = applicationAssembler;
    }

    /**
     * Liste paginée des applications avec filtrage optionnel sur le statut blacklisté.
     */
    @GetMapping
    public Page<ApplicationResponse> getAll(
            @RequestParam(required = false) final Boolean blacklisted,
            @PageableDefault(size = 20, sort = "name") final Pageable pageable
    ) {
        return applicationService.findAll(pageable, blacklisted)
                .map(applicationAssembler::toDto);
    }

    /**
     * Recherche une application par son identifiant.
     */
    @GetMapping("/{id}")
    public ApplicationResponse getById(@PathVariable final Long id) {
        return applicationService.findById(id)
                .map(applicationAssembler::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Application introuvable avec l'id : " + id));
    }

    /**
     * Crée une application ou règle manuellement.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@Valid @RequestBody final ApplicationRequest request) {
        Application app = applicationAssembler.toEntity(request);
        Application created = applicationService.create(app);
        return applicationAssembler.toDto(created);
    }

    /**
     * Met à jour une application existante.
     */
    @PutMapping("/{id}")
    public ApplicationResponse update(@PathVariable final Long id, @Valid @RequestBody final ApplicationRequest request) {
        Application app = applicationService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application introuvable avec l'id : " + id));
        applicationAssembler.updateEntity(app, request);
        Application updated = applicationService.update(app);
        return applicationAssembler.toDto(updated);
    }

    /**
     * Active ou désactive le statut de liste noire d'une application.
     */
    @PatchMapping("/{id}/blacklist")
    public ApplicationResponse toggleBlacklist(
            @PathVariable final Long id,
            @Valid @RequestBody final ApplicationBlacklistRequest request
    ) {
        Application updated = applicationService.toggleBlacklist(id, request.blacklisted());
        return applicationAssembler.toDto(updated);
    }

    /**
     * Supprime une application du catalogue.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable final Long id) {
        applicationService.deleteById(id);
    }
}
