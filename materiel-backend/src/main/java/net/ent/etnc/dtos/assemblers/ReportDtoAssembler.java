package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.auth.UserSummaryResponse;
import net.ent.etnc.dtos.material.MaterialResponse;
import net.ent.etnc.dtos.report.ReportCreateRequest;
import net.ent.etnc.dtos.report.ReportResponse;
import net.ent.etnc.dtos.report.ReportUpdateRequest;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.Report;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Assembler pour les conversions de {@link Report}.
 */
@Component
public class ReportDtoAssembler {

    private final MaterialDtoAssembler materialDtoAssembler;
    private final UserDtoAssembler userDtoAssembler;

    @Autowired
    public ReportDtoAssembler(final MaterialDtoAssembler materialDtoAssembler, final UserDtoAssembler userDtoAssembler) {
        this.materialDtoAssembler = materialDtoAssembler;
        this.userDtoAssembler = userDtoAssembler;
    }

    public ReportResponse toDto(final Report entity) {
        if (entity == null) return null;
        MaterialResponse materialResponse = entity.getMaterial() != null ? this.materialDtoAssembler.toDto(entity.getMaterial()) : null;
        UserSummaryResponse userSummaryResponse = entity.getUser() != null ? this.userDtoAssembler.toSummaryDto(entity.getUser()) : null;

        return new ReportResponse(
                entity.getId(),
                entity.getVersion(),
                entity.getDescription(),
                entity.getReportDateTime(),
                userSummaryResponse,
                materialResponse,
                entity.getHandleDateTime(),
                entity.isHandled()
        );
    }

    public Report toEntity(final ReportCreateRequest request) {
        if (request == null) return null;
        Report report = new Report();
        report.setDescription(request.description());

        if (request.materialId() != null) {
            Material material = new Material();
            material.setId(request.materialId());
            report.setMaterial(material);
        }
        return report;
    }

    public void updateEntity(final Report entity, final ReportUpdateRequest request) {
        if (entity != null && request != null) {
            entity.setDescription(request.description());
        }
    }
}
