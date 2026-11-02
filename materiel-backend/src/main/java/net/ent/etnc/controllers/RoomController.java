package net.ent.etnc.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.dtos.assemblers.RoomDtoAssembler;
import net.ent.etnc.dtos.room.RoomRequest;
import net.ent.etnc.dtos.room.RoomResponse;
import net.ent.etnc.models.Room;
import net.ent.etnc.services.RoomService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Point d'entrée REST pour la gestion des salles.
 */
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RoomDtoAssembler roomAssembler;

    @Autowired
    public RoomController(RoomService roomService, RoomDtoAssembler roomAssembler) {
        this.roomService = roomService;
        this.roomAssembler = roomAssembler;
    }

    /**
     * Liste paginée des salles (consultable par tous les utilisateurs authentifiés).
     */
    @GetMapping
    public Page<RoomResponse> getAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return roomService.findAll(pageable)
                .map(roomAssembler::toDto);
    }

    /**
     * Recherche une salle par son identifiant.
     */
    @GetMapping("/{id}")
    public RoomResponse getById(@PathVariable Long id) {
        return roomService.findById(id)
                .map(roomAssembler::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable avec l'id : " + id));
    }

    /**
     * Crée une nouvelle salle (réservé aux administrateurs).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse create(@Valid @RequestBody RoomRequest request) {
        Room entity = roomAssembler.toEntity(request);
        Room saved = roomService.create(entity);
        return roomAssembler.toDto(saved);
    }

    /**
     * Met à jour une salle existante (réservé aux administrateurs).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        Room existing = roomService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable avec l'id : " + id));

        roomAssembler.updateEntity(existing, request);
        Room updated = roomService.update(existing);
        return roomAssembler.toDto(updated);
    }

    /**
     * Supprime une salle par son identifiant (réservé aux administrateurs).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!roomService.existsById(id)) {
            throw new ResourceNotFoundException("Salle introuvable avec l'id : " + id);
        }
        roomService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
