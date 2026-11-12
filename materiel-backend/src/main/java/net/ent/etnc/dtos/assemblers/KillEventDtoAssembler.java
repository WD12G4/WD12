package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.assemblers.commons.DtoAssembler;
import net.ent.etnc.dtos.auth.UserSummaryResponse;
import net.ent.etnc.dtos.killevent.KillEventRequest;
import net.ent.etnc.dtos.killevent.KillEventResponse;
import net.ent.etnc.dtos.material.MaterialResponse;
import net.ent.etnc.models.KillEvent;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Assembler pour les conversions de {@link KillEvent}.
 */
@Component
public class KillEventDtoAssembler implements DtoAssembler<KillEvent, KillEventRequest, KillEventResponse> {

    private final MaterialDtoAssembler materialDtoAssembler;
    private final UserDtoAssembler userDtoAssembler;

    @Autowired
    public KillEventDtoAssembler(final MaterialDtoAssembler materialDtoAssembler,
                                 final UserDtoAssembler userDtoAssembler) {
        this.materialDtoAssembler = materialDtoAssembler;
        this.userDtoAssembler = userDtoAssembler;
    }

    @Override
    public KillEventResponse toDto(final KillEvent entity) {
        if (entity == null) return null;
        MaterialResponse materialResponse = entity.getUc() != null ? this.materialDtoAssembler.toDto(entity.getUc()) : null;
        UserSummaryResponse userResponse = entity.getUser() != null ? this.userDtoAssembler.toSummaryDto(entity.getUser()) : null;
        return new KillEventResponse(
                entity.getId(),
                entity.getVersion(),
                entity.getProcessName(),
                materialResponse,
                userResponse,
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
        killEvent.setResult(request.result() != null ? request.result() : true);

        if (request.ucId() != null) {
            Material material = new Material();
            material.setId(request.ucId());
            killEvent.setUc(material);
        } else {
            killEvent.setUc(null);
        }

        if (request.userId() != null) {
            User user = new User();
            user.setId(request.userId());
            killEvent.setUser(user);
        } else {
            killEvent.setUser(null);
        }
    }
}
