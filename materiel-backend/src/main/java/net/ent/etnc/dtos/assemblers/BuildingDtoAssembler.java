package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.assemblers.commons.DtoAssembler;
import net.ent.etnc.dtos.building.BuildingRequest;
import net.ent.etnc.dtos.building.BuildingResponse;
import net.ent.etnc.models.Building;
import org.springframework.stereotype.Component;

/**
 * Assembler pour les conversions de {@link Building}.
 */
@Component
public class BuildingDtoAssembler implements DtoAssembler<Building, BuildingRequest, BuildingResponse> {

    @Override
    public BuildingResponse toDto(Building entity) {
        if (entity == null) return null;
        return new BuildingResponse(entity.getId(), entity.getVersion(), entity.getNumber());
    }

    @Override
    public Building toEntity(BuildingRequest request) {
        if (request == null) return null;
        Building building = new Building();
        building.setNumber(request.number());
        return building;
    }

    @Override
    public void updateEntity(Building entity, BuildingRequest request) {
        if (entity != null && request != null) {
            entity.setNumber(request.number());
        }
    }
}
