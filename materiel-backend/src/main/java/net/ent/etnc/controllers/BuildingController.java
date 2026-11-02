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
        return buildingService.findAll(pageable)
                .map(buildingAssembler::toDto);
    }

    /**
     * Recherche un bâtiment par son identifiant.
     */
    @GetMapping("/{id}")
    public BuildingResponse getById(@PathVariable Long id) {
        return buildingService.findById(id)
                .map(buildingAssembler::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Bâtiment introuvable avec l'id : " + id));
    }

    /**
     * Crée un nouveau bâtiment (réservé aux administrateurs).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BuildingResponse create(@Valid @RequestBody BuildingRequest request) {
        Building entity = buildingAssembler.toEntity(request);
        Building saved = buildingService.create(entity);
        return buildingAssembler.toDto(saved);
    }

    /**
     * Met à jour un bâtiment existant (réservé aux administrateurs).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public BuildingResponse update(@PathVariable Long id, @Valid @RequestBody BuildingRequest request) {
        Building existing = buildingService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bâtiment introuvable avec l'id : " + id));

        buildingAssembler.updateEntity(existing, request);
        Building updated = buildingService.update(existing);
        return buildingAssembler.toDto(updated);
    }

    /**
     * Supprime un bâtiment par son identifiant (réservé aux administrateurs).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!buildingService.existsById(id)) {
            throw new ResourceNotFoundException("Bâtiment introuvable avec l'id : " + id);
        }
        buildingService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
