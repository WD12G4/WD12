package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.auth.UserSummaryResponse;
import net.ent.etnc.dtos.user.UserCreateRequest;
import net.ent.etnc.dtos.user.UserResponse;
import net.ent.etnc.dtos.user.UserUpdateRequest;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserDtoAssembler Unit Tests")
class UserDtoAssemblerTest {

    private UserDtoAssembler assembler;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        assembler = new UserDtoAssembler();
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setVersion(0L);
        sampleUser.setUsername("alice");
        sampleUser.setRole(ERole.USER);
        sampleUser.setEnabled(true);
    }

    @Nested
    @DisplayName("Tests toDto")
    class ToDtoTests {

        @Test
        @DisplayName("Doit retourner null quand l'entité est null")
        void toDto_WhenEntityIsNull_ReturnsNull() {
            assertThat(assembler.toDto(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir correctement l'entité en DTO complet")
        void toDto_Success() {
            UserResponse response = assembler.toDto(sampleUser);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.version()).isEqualTo(0L);
            assertThat(response.username()).isEqualTo("alice");
            assertThat(response.role()).isEqualTo(ERole.USER);
            assertThat(response.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Tests toSummaryDto")
    class ToSummaryDtoTests {

        @Test
        @DisplayName("Doit retourner null quand l'entité est null")
        void toSummaryDto_WhenEntityIsNull_ReturnsNull() {
            assertThat(assembler.toSummaryDto(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir correctement l'entité en résumé")
        void toSummaryDto_Success() {
            UserSummaryResponse summary = assembler.toSummaryDto(sampleUser);

            assertThat(summary).isNotNull();
            assertThat(summary.id()).isEqualTo(1L);
            assertThat(summary.username()).isEqualTo("alice");
            assertThat(summary.role()).isEqualTo(ERole.USER);
        }
    }

    @Nested
    @DisplayName("Tests toEntity")
    class ToEntityTests {

        @Test
        @DisplayName("Doit retourner null quand la requête est null")
        void toEntity_WhenRequestIsNull_ReturnsNull() {
            assertThat(assembler.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir correctement UserCreateRequest en entité")
        void toEntity_Success() {
            UserCreateRequest request = new UserCreateRequest("bob", ERole.MANAGER);

            User entity = assembler.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getUsername()).isEqualTo("bob");
            assertThat(entity.getRole()).isEqualTo(ERole.MANAGER);
            assertThat(entity.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Tests updateEntity")
    class UpdateEntityTests {

        @Test
        @DisplayName("Doit mettre à jour tous les champs fournis")
        void updateEntity_AllFields_Success() {
            UserUpdateRequest request = new UserUpdateRequest(ERole.ADMIN, false);

            assembler.updateEntity(sampleUser, request);

            assertThat(sampleUser.getRole()).isEqualTo(ERole.ADMIN);
            assertThat(sampleUser.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("Ne doit pas modifier les champs non renseignés")
        void updateEntity_PartialFields_Success() {
            UserUpdateRequest request = new UserUpdateRequest(ERole.MANAGER, null);

            assembler.updateEntity(sampleUser, request);

            assertThat(sampleUser.getRole()).isEqualTo(ERole.MANAGER);
            assertThat(sampleUser.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Doit ignorer sans lever d'erreur si entité ou requête est null")
        void updateEntity_WhenNull_DoesNotThrow() {
            assembler.updateEntity(null, new UserUpdateRequest(ERole.USER, true));
            assembler.updateEntity(sampleUser, null);

            assertThat(sampleUser.getUsername()).isEqualTo("alice");
        }
    }

    @Nested
    @DisplayName("Tests toDtoList")
    class ToDtoListTests {

        @Test
        @DisplayName("Doit convertir une collection d'utilisateurs en liste de DTOs")
        void toDtoList_Success() {
            List<UserResponse> list = assembler.toDtoList(List.of(sampleUser));

            assertThat(list).hasSize(1);
            assertThat(list.get(0).username()).isEqualTo("alice");
        }

        @Test
        @DisplayName("Doit renvoyer une liste vide si collection est null")
        void toDtoList_WhenNull_ReturnsEmpty() {
            assertThat(assembler.toDtoList(null)).isEmpty();
        }
    }
}
