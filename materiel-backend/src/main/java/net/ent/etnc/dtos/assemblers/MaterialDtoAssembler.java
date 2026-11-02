package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.assemblers.commons.DtoAssembler;
import net.ent.etnc.dtos.material.MaterialRequest;
import net.ent.etnc.dtos.material.MaterialResponse;
import net.ent.etnc.dtos.room.RoomResponse;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.Room;
import net.ent.etnc.services.RoomService;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Assembler pour les conversions de {@link Material}.
 */
@Component
public class MaterialDtoAssembler implements DtoAssembler<Material, MaterialRequest, MaterialResponse> {

    private final RoomDtoAssembler roomAssembler;
    private final RoomService roomService;

    @Autowired
    public MaterialDtoAssembler(RoomDtoAssembler roomAssembler, RoomService roomService) {
        this.roomAssembler = roomAssembler;
        this.roomService = roomService;
    }

    @Override
    public MaterialResponse toDto(Material entity) {
        if (entity == null) return null;
        RoomResponse roomDto = entity.getRoom() != null ? roomAssembler.toDto(entity.getRoom()) : null;
        return new MaterialResponse(
                entity.getId(),
                entity.getVersion(),
                entity.getSerialNumber(),
                entity.getMaterialType(),
                entity.getEndAvailabilityDate(),
                roomDto,
                entity.isArchived()
        );
    }

    @Override
    public Material toEntity(MaterialRequest request) {
        if (request == null) return null;
        Material material = new Material();
        applyFields(material, request);
        return material;
    }

    @Override
    public void updateEntity(Material entity, MaterialRequest request) {
        if (entity != null && request != null) {
            applyFields(entity, request);
        }
    }

    private void applyFields(Material material, MaterialRequest request) {
        material.setSerialNumber(request.serialNumber());
        material.setMaterialType(request.materialType());
        material.setEndAvailabilityDate(request.endAvailabilityDate());
        material.setArchived(request.archived());

        if (request.roomId() != null) {
            Room room = roomService.findById(request.roomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Salle introuvable avec l'id : " + request.roomId()));
            material.setRoom(room);
        } else {
            material.setRoom(null);
        }
    }
}
