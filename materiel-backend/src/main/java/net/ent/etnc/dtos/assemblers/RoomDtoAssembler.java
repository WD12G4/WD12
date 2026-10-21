package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.assemblers.commons.DtoAssembler;
import net.ent.etnc.dtos.building.BuildingResponse;
import net.ent.etnc.dtos.room.RoomRequest;
import net.ent.etnc.dtos.room.RoomResponse;
import net.ent.etnc.models.Building;
import net.ent.etnc.models.Room;
import net.ent.etnc.services.BuildingService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Assembler pour les conversions de {@link Room}.
 */
@Component
public class RoomDtoAssembler implements DtoAssembler<Room, RoomRequest, RoomResponse> {

    private final BuildingDtoAssembler buildingAssembler;
    private final BuildingService buildingService;

    @Autowired
    public RoomDtoAssembler(BuildingDtoAssembler buildingAssembler, BuildingService buildingService) {
        this.buildingAssembler = buildingAssembler;
        this.buildingService = buildingService;
    }

    @Override
    public RoomResponse toDto(Room entity) {
        if (entity == null) return null;
        BuildingResponse buildingDto = buildingAssembler.toDto(entity.getBuilding());
        return new RoomResponse(entity.getId(), entity.getVersion(), entity.getNumber(), buildingDto);
    }

    @Override
    public Room toEntity(RoomRequest request) {
        if (request == null) return null;
        Room room = new Room();
        room.setNumber(request.number());
        room.setBuilding(resolveBuilding(request.buildingId()));
        return room;
    }

    @Override
    public void updateEntity(Room entity, RoomRequest request) {
        if (entity != null && request != null) {
            entity.setNumber(request.number());
            if (request.buildingId() != null) {
                entity.setBuilding(resolveBuilding(request.buildingId()));
            }
        }
    }

    private Building resolveBuilding(Long buildingId) {
        return buildingService.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Bâtiment introuvable avec l'id : " + buildingId));
    }
}
