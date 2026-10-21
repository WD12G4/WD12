package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.auth.UserSummaryResponse;
import net.ent.etnc.dtos.user.UserCreateRequest;
import net.ent.etnc.dtos.user.UserResponse;
import net.ent.etnc.models.User;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Assembler pour les conversions de {@link User}.
 */
@Component
public class UserDtoAssembler {

    public UserResponse toDto(User entity) {
        if (entity == null) return null;
        return new UserResponse(
                entity.getId(),
                entity.getVersion(),
                entity.getUsername(),
                entity.getRole(),
                entity.isEnabled()
        );
    }

    public UserSummaryResponse toSummaryDto(User entity) {
        if (entity == null) return null;
        return new UserSummaryResponse(entity.getId(), entity.getUsername(), entity.getRole());
    }

    public User toEntity(UserCreateRequest request) {
        if (request == null) return null;
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setRole(request.role());
        user.setEnabled(true);
        return user;
    }

    public List<UserResponse> toDtoList(Collection<User> entities) {
        if (entities == null) return List.of();
        return entities.stream().map(this::toDto).toList();
    }
}
