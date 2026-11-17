package net.ent.etnc.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.dtos.assemblers.BuildingDtoAssembler;
import net.ent.etnc.dtos.building.BuildingRequest;
import net.ent.etnc.dtos.building.BuildingResponse;
import net.ent.etnc.models.Building;
import net.ent.etnc.services.BuildingService;
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
 * Point d'entrée REST pour la gestion des bâtiments.
 */
@RestController
@RequestMapping("/api/v1/buildings")
public class BuildingController {

    private final BuildingService buildingService;
    private final BuildingDtoAssembler buildingAssembler;

    @Autowired
    public BuildingController(BuildingService buildingService, BuildingDtoAssembler buildingAssembler) {
        this.buildingService = buildingService;
        this.buildingAssembler = buildingAssembler;
    }

    /**
     * Liste paginée des bâtiments (consultable par tous les utilisateurs authentifiés).
     */
    @GetMapping
    public Page<BuildingResponse> getAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return this.buildingService.findAll(pageable)
                .map(this.buildingAssembler::toDto);
    }

    /**
     * Crée un nouveau bâtiment (réservé aux administrateurs).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BuildingResponse create(@Valid @RequestBody BuildingRequest request) {
        Building entity = this.buildingAssembler.toEntity(request);
        Building saved = this.buildingService.create(entity);
        return this.buildingAssembler.toDto(saved);
    }

    /**
     * Met à jour un bâtiment existant (réservé aux administrateurs).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public BuildingResponse update(@PathVariable Long id, @Valid @RequestBody BuildingRequest request) {
        Building existing = this.buildingService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bâtiment introuvable avec l'id : " + id));

        this.buildingAssembler.updateEntity(existing, request);
        Building updated = this.buildingService.update(existing);
        return this.buildingAssembler.toDto(updated);
    }

    /**
     * Supprime un bâtiment par son identifiant (réservé aux administrateurs).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!this.buildingService.existsById(id)) {
            throw new ResourceNotFoundException("Bâtiment introuvable avec l'id : " + id);
        }
        this.buildingService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
