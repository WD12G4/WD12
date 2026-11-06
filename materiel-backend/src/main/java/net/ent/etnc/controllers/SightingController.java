package net.ent.etnc.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.dtos.assemblers.SightingDtoAssembler;
import net.ent.etnc.dtos.sighting.SightingRequest;
import net.ent.etnc.dtos.sighting.SightingResponse;
import net.ent.etnc.services.SightingService;
import net.ent.etnc.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Point d'entrée REST des constats de terrain (PWA RECON) : ce qui a été scanné, où, quand et par qui.
 * Partagés entre tous les téléphones : un matériel constaté une fois l'est pour tout le monde.
 */
@RestController
@RequestMapping("/api/v1/sightings")
public class SightingController {

    private final SightingService sightingService;
    private final SightingDtoAssembler sightingAssembler;
    private final UserService userService;

    @Autowired
    public SightingController(SightingService sightingService, SightingDtoAssembler sightingAssembler,
                              UserService userService) {
        this.sightingService = sightingService;
        this.sightingAssembler = sightingAssembler;
        this.userService = userService;
    }

    /**
     * Tous les constats (consultables par tous les utilisateurs authentifiés).
     */
    @GetMapping
    public List<SightingResponse> getAll() {
        return sightingAssembler.toDtoList(sightingService.findAll());
    }

    /**
     * Enregistre un scan dans une salle (tout utilisateur authentifié : constater ne modifie pas le parc).
     */
    @PostMapping
    public SightingResponse record(@Valid @RequestBody SightingRequest request) {
        String seenBy = userService.currentUser().getUsername();
        return sightingAssembler.toDto(sightingService.record(
                request.serialNumber(), request.roomId(), request.seenAt(), seenBy, request.replaces()));
    }

    /**
     * Efface les constats d'une salle pour recommencer sa reconnaissance (réservé aux administrateurs et gestionnaires).
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping
    public ResponseEntity<Void> clearRoom(@RequestParam Long roomId) {
        sightingService.clearRoom(roomId);
        return ResponseEntity.noContent().build();
    }
}
