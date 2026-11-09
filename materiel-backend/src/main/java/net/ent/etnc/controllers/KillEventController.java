package net.ent.etnc.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.dtos.assemblers.KillEventDtoAssembler;
import net.ent.etnc.dtos.killevent.KillEventRequest;
import net.ent.etnc.dtos.killevent.KillEventResponse;
import net.ent.etnc.models.KillEvent;
import net.ent.etnc.services.KillEventService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Point d'entrée REST pour la gestion des kill events.
 */
@RestController
@RequestMapping("/api/v1/killevents")
public class KillEventController {

    private final KillEventService killEventService;
    private final KillEventDtoAssembler killEventDtoAssembler;

    @Autowired
    public KillEventController(final KillEventService killEventService, final KillEventDtoAssembler killEventDtoAssembler) {
        this.killEventService = killEventService;
        this.killEventDtoAssembler = killEventDtoAssembler;
    }

    /**
     * Liste paginée des {@link KillEvent}.
     */
    @GetMapping
    public Page<KillEventResponse> getAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return this.killEventService.findAll(pageable)
                .map(this.killEventDtoAssembler::toDto);
    }

    /**
     * Recherche un killevent par son identifiant.
     */
    @GetMapping("/{id}")
    public KillEventResponse getById(@PathVariable Long id) {
        return this.killEventService.findById(id)
                .map(this.killEventDtoAssembler::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("KillEvent introuvable avec l'id : " + id));
    }

    /**
     * Crée un nouveau Kill Event.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KillEventResponse create(@Valid @RequestBody KillEventRequest request) {
        KillEvent entity = this.killEventDtoAssembler.toEntity(request);
        KillEvent saved = this.killEventService.create(entity);
        return this.killEventDtoAssembler.toDto(saved);
    }
}
