package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.application.ApplicationRequest;
import net.ent.etnc.dtos.application.ApplicationResponse;
import net.ent.etnc.dtos.assemblers.commons.DtoAssembler;
import net.ent.etnc.models.Application;
import org.springframework.stereotype.Component;

/**
 * Assembler pour les conversions de {@link Application}.
 */
@Component
public class ApplicationDtoAssembler implements DtoAssembler<Application, ApplicationRequest, ApplicationResponse> {

    @Override
    public ApplicationResponse toDto(Application entity) {
        if (entity == null) {
            return null;
        }
        return new ApplicationResponse(
                entity.getId(),
                entity.getVersion(),
                entity.getName(),
                entity.getDisplayName(),
                entity.getIconBase64(),
                entity.isBlacklisted(),
                entity.getDescription()
        );
    }

    @Override
    public Application toEntity(ApplicationRequest request) {
        if (request == null) {
            return null;
        }
        Application app = new Application();
        app.setName(request.name());
        app.setDisplayName(request.displayName());
        app.setIconBase64(request.iconBase64());
        app.setBlacklisted(request.blacklisted() != null && request.blacklisted());
        app.setDescription(request.description());
        return app;
    }

    @Override
    public void updateEntity(Application entity, ApplicationRequest request) {
        if (entity != null && request != null) {
            if (request.name() != null) {
                entity.setName(request.name());
            }
            if (request.displayName() != null) {
                entity.setDisplayName(request.displayName());
            }
            if (request.iconBase64() != null) {
                entity.setIconBase64(request.iconBase64());
            }
            if (request.blacklisted() != null) {
                entity.setBlacklisted(request.blacklisted());
            }
            if (request.description() != null) {
                entity.setDescription(request.description());
            }
        }
    }
}
