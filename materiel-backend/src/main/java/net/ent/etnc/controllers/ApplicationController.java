package net.ent.etnc.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.dtos.application.ApplicationBlacklistRequest;
import net.ent.etnc.dtos.application.ApplicationResponse;
import net.ent.etnc.dtos.assemblers.ApplicationDtoAssembler;
import net.ent.etnc.models.Application;
import net.ent.etnc.services.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
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
        return this.applicationService.findAll(pageable, blacklisted)
                .map(this.applicationAssembler::toDto);
    }

    /**
     * Active ou désactive le statut de liste noire d'une application (réservé aux administrateurs, RG-27).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/blacklist")
    public ApplicationResponse toggleBlacklist(
            @PathVariable final Long id,
            @Valid @RequestBody final ApplicationBlacklistRequest request
    ) {
        Application updated = this.applicationService.toggleBlacklist(id, request.blacklisted());
        return this.applicationAssembler.toDto(updated);
    }
}
