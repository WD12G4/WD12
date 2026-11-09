package net.ent.etnc.models.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ERole Unit Tests")
class ERoleTest {

    @Test
    @DisplayName("USER ne doit avoir que le rôle USER")
    void user_Roles() {
        Set<ERole> roles = ERole.USER.getAllRoles();

        assertThat(roles).containsExactly(ERole.USER);
        assertThat(ERole.USER.hasRole(ERole.USER)).isTrue();
        assertThat(ERole.USER.hasRole(ERole.MANAGER)).isFalse();
        assertThat(ERole.USER.hasRole(ERole.ADMIN)).isFalse();
    }

    @Test
    @DisplayName("MANAGER doit hériter du rôle USER")
    void manager_Roles() {
        Set<ERole> roles = ERole.MANAGER.getAllRoles();

        assertThat(roles).contains(ERole.MANAGER, ERole.USER);
        assertThat(ERole.MANAGER.hasRole(ERole.MANAGER)).isTrue();
        assertThat(ERole.MANAGER.hasRole(ERole.USER)).isTrue();
        assertThat(ERole.MANAGER.hasRole(ERole.ADMIN)).isFalse();
    }

    @Test
    @DisplayName("ADMIN doit hériter des rôles MANAGER et USER")
    void admin_Roles() {
        Set<ERole> roles = ERole.ADMIN.getAllRoles();

        assertThat(roles).contains(ERole.ADMIN, ERole.MANAGER, ERole.USER);
        assertThat(ERole.ADMIN.hasRole(ERole.ADMIN)).isTrue();
        assertThat(ERole.ADMIN.hasRole(ERole.MANAGER)).isTrue();
        assertThat(ERole.ADMIN.hasRole(ERole.USER)).isTrue();
    }
}
