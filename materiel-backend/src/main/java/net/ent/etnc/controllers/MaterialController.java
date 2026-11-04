package net.ent.etnc.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.dtos.assemblers.MaterialDtoAssembler;
import net.ent.etnc.dtos.material.MaterialAssignRequest;
import net.ent.etnc.dtos.material.MaterialRequest;
import net.ent.etnc.dtos.material.MaterialResponse;
import net.ent.etnc.models.Material;
import net.ent.etnc.services.MaterialService;
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
 * Point d'entrée REST pour la gestion des matériels.
 */
@RestController
@RequestMapping("/api/v1/materials")
public class MaterialController {

    private final MaterialService materialService;
    private final MaterialDtoAssembler materialAssembler;

    @Autowired
    public MaterialController(MaterialService materialService, MaterialDtoAssembler materialAssembler) {
        this.materialService = materialService;
        this.materialAssembler = materialAssembler;
    }

    /**
     * Liste paginée des matériels (consultable par tous les utilisateurs authentifiés).
     */
    @GetMapping
    public Page<MaterialResponse> getAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return materialService.findAll(pageable)
                .map(materialAssembler::toDto);
    }

    /**
     * Recherche un matériel par son identifiant.
     */
    @GetMapping("/{id}")
    public MaterialResponse getById(@PathVariable Long id) {
        return materialService.findById(id)
                .map(materialAssembler::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel introuvable avec l'id : " + id));
    }

    /**
     * Crée un nouveau matériel (réservé aux administrateurs et gestionnaires).
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse create(@Valid @RequestBody MaterialRequest request) {
        Material entity = materialAssembler.toEntity(request);
        Material saved = materialService.create(entity);
        return materialAssembler.toDto(saved);
    }

    /**
     * Met à jour un matériel existant (réservé aux administrateurs et gestionnaires).
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PutMapping("/{id}")
    public MaterialResponse update(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        Material existing = materialService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matériel introuvable avec l'id : " + id));

        materialAssembler.updateEntity(existing, request);
        Material updated = materialService.update(existing);
        return materialAssembler.toDto(updated);
    }

    /**
     * Affecte ou désaffecte un matériel à une salle (autorisé aux administrateurs et gestionnaires).
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/{id}/room")
    public MaterialResponse assignRoom(@PathVariable Long id, @RequestBody MaterialAssignRequest request) {
        Material updated = materialService.assignRoom(id, request.roomId());
        return materialAssembler.toDto(updated);
    }

    /**
     * Supprime (soft delete / archivage) un matériel par son identifiant (réservé aux administrateurs et gestionnaires).
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        materialService.archiveById(id);
        return ResponseEntity.noContent().build();
    }
}
