package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.assemblers.commons.DtoAssembler;
import net.ent.etnc.dtos.killevent.KillEventRequest;
import net.ent.etnc.dtos.killevent.KillEventResponse;
import net.ent.etnc.dtos.material.MaterialResponse;
import net.ent.etnc.models.KillEvent;
import net.ent.etnc.models.Material;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Assembler pour les conversions de {@link KillEvent}.
 */
@Component
public class KillEventDtoAssembler implements DtoAssembler<KillEvent, KillEventRequest, KillEventResponse> {

    private final MaterialDtoAssembler materialDtoAssembler;

    @Autowired
    public KillEventDtoAssembler(final MaterialDtoAssembler materialDtoAssembler) {
        this.materialDtoAssembler = materialDtoAssembler;
    }

    @Override
    public KillEventResponse toDto(final KillEvent entity) {
        if (entity == null) return null;
        MaterialResponse materialResponse = entity.getUc() != null ? this.materialDtoAssembler.toDto(entity.getUc()) : null;
        return new KillEventResponse(
                entity.getId(),
                entity.getVersion(),
                entity.getProcessName(),
                materialResponse,
                entity.getKillDateTime(),
                entity.getResult());
    }

    @Override
    public KillEvent toEntity(final KillEventRequest request) {
        if (request == null) return null;
        KillEvent killEvent = new KillEvent();
        applyFields(killEvent, request);
        return killEvent;
    }

    @Override
    public void updateEntity(KillEvent entity, KillEventRequest request) {
        throw new UnsupportedOperationException("La mise à jour d'un Kill Event n'est pas supportée.");
    }

    private void applyFields(final KillEvent killEvent, final KillEventRequest request) {
        killEvent.setProcessName(request.processName());
        killEvent.setKillDateTime(request.killDateTime());
        killEvent.setResult(request.result());

        if (request.ucId() != null) {
            Material material = new Material();
            material.setId(request.ucId());
            killEvent.setUc(material);
        } else {
            killEvent.setUc(null);
        }
    }
}
