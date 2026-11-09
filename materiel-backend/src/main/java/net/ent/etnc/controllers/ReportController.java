package net.ent.etnc.controllers;

import jakarta.validation.Valid;
import net.ent.etnc.dtos.assemblers.ReportDtoAssembler;
import net.ent.etnc.dtos.report.ReportCreateRequest;
import net.ent.etnc.dtos.report.ReportResponse;
import net.ent.etnc.dtos.report.ReportUpdateRequest;
import net.ent.etnc.models.Report;
import net.ent.etnc.services.ReportService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Point d'entrée REST pour la gestion des signalements (Report).
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final ReportDtoAssembler reportDtoAssembler;

    @Autowired
    public ReportController(final ReportService reportService, final ReportDtoAssembler reportDtoAssembler) {
        this.reportService = reportService;
        this.reportDtoAssembler = reportDtoAssembler;
    }

    /**
     * Liste paginée des signalements, avec filtres optionnels par matériel, utilisateur ou statut.
     */
    @GetMapping
    public Page<ReportResponse> getAll(
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean handled,
            @PageableDefault(size = 20, sort = "reportDateTime", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Report> reports;
        if (materialId != null) {
            reports = this.reportService.findByMaterialId(materialId, pageable);
        } else if (userId != null) {
            reports = this.reportService.findByUserId(userId, pageable);
        } else if (handled != null) {
            reports = this.reportService.findByHandled(handled, pageable);
        } else {
            reports = this.reportService.findAll(pageable);
        }
        return reports.map(this.reportDtoAssembler::toDto);
    }

    /**
     * Recherche un signalement par son identifiant.
     */
    @GetMapping("/{id}")
    public ReportResponse getById(@PathVariable Long id) {
        return this.reportService.findById(id)
                .map(this.reportDtoAssembler::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable avec l'id : " + id));
    }

    /**
     * Crée un nouveau signalement (accessible à tout utilisateur authentifié).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(@Valid @RequestBody ReportCreateRequest request) {
        Report entity = this.reportDtoAssembler.toEntity(request);
        Report saved = this.reportService.create(entity);
        return this.reportDtoAssembler.toDto(saved);
    }

    /**
     * Met à jour la description d'un signalement (réservé à l'utilisateur qui l'a créé).
     */
    @PutMapping("/{id}")
    public ReportResponse update(@PathVariable Long id, @Valid @RequestBody ReportUpdateRequest request) {
        Report existing = this.reportService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable avec l'id : " + id));

        this.reportDtoAssembler.updateEntity(existing, request);
        Report updated = this.reportService.update(existing);
        return this.reportDtoAssembler.toDto(updated);
    }

    /**
     * Modifie l'état de traitement d'un signalement (réservé aux gestionnaires et administrateurs).
     */
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @PatchMapping("/{id}/handle")
    public ReportResponse markAsHandled(@PathVariable Long id,
                                        @RequestParam(defaultValue = "true") boolean handled) {
        Report updated = this.reportService.markAsHandled(id, handled);
        return this.reportDtoAssembler.toDto(updated);
    }

    /**
     * Supprime un signalement (réservé au créateur du signalement ou aux gestionnaires/administrateurs).
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        this.reportService.deleteById(id);
    }
}
