package net.ent.etnc.controllers;

import net.ent.etnc.dtos.assemblers.KillEventDtoAssembler;
import net.ent.etnc.dtos.killevent.KillEventResponse;
import net.ent.etnc.models.KillEvent;
import net.ent.etnc.services.KillEventService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * Point d'entrée REST pour la consultation du journal des arrêts (kill events).
 * Les événements sont enregistrés exclusivement par le service de supervision interne.
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
     * Permet le filtrage optionnel par utilisateur (réservé à l'ADMIN ou à l'utilisateur lui-même).
     */
    @GetMapping
    public Page<KillEventResponse> getAll(
            @RequestParam(required = false) final Long userId,
            @PageableDefault(size = 20, sort = "id") final Pageable pageable) {
        Page<KillEvent> events = (userId != null)
                ? this.killEventService.findByUserId(userId, pageable)
                : this.killEventService.findAll(pageable);
        return events.map(this.killEventDtoAssembler::toDto);
    }

    /**
     * Recherche un killevent par son identifiant.
     */
    @GetMapping("/{id}")
    public KillEventResponse getById(@PathVariable final Long id) {
        return this.killEventService.findById(id)
                .map(this.killEventDtoAssembler::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("KillEvent introuvable avec l'id : " + id));
    }
}
