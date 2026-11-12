package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.auth.UserSummaryResponse;
import net.ent.etnc.dtos.killevent.KillEventRequest;
import net.ent.etnc.dtos.killevent.KillEventResponse;
import net.ent.etnc.dtos.material.MaterialResponse;
import net.ent.etnc.models.KillEvent;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.EMaterialType;
import net.ent.etnc.models.enums.ERole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KillEventDtoAssembler Unit Tests")
class KillEventDtoAssemblerTest {

    @Mock
    private MaterialDtoAssembler materialDtoAssembler;

    @Mock
    private UserDtoAssembler userDtoAssembler;

    @InjectMocks
    private KillEventDtoAssembler killEventDtoAssembler;

    private KillEvent sampleEntity;
    private Material sampleUc;
    private User sampleUser;
    private MaterialResponse sampleMaterialResponse;
    private UserSummaryResponse sampleUserSummaryResponse;

    @BeforeEach
    void setUp() {
        sampleUc = new Material();
        sampleUc.setId(1L);
        sampleUc.setSerialNumber("UC-001");
        sampleUc.setMaterialType(EMaterialType.UC);

        sampleUser = new User();
        sampleUser.setId(5L);
        sampleUser.setUsername("professeur");
        sampleUser.setRole(ERole.USER);

        sampleEntity = new KillEvent();
        sampleEntity.setId(10L);
        sampleEntity.setVersion(0L);
        sampleEntity.setProcessName("malicious.exe");
        sampleEntity.setUc(sampleUc);
        sampleEntity.setUser(sampleUser);
        sampleEntity.setKillDateTime(LocalDateTime.of(2026, 1, 15, 10, 30));
        sampleEntity.setResult(true);

        sampleMaterialResponse = new MaterialResponse(
                1L, 0L, "UC-001", EMaterialType.UC, LocalDate.now().plusYears(1), null, false
        );
        sampleUserSummaryResponse = new UserSummaryResponse(5L, "professeur", ERole.USER);
    }

    @Nested
    @DisplayName("Tests toDto")
    class ToDtoTests {

        @Test
        @DisplayName("Doit retourner null quand l'entité est null")
        void toDto_WhenEntityIsNull_ReturnsNull() {
            assertThat(killEventDtoAssembler.toDto(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir correctement l'entité en DTO")
        void toDto_Success() {
            when(materialDtoAssembler.toDto(sampleUc)).thenReturn(sampleMaterialResponse);
            when(userDtoAssembler.toSummaryDto(sampleUser)).thenReturn(sampleUserSummaryResponse);

            KillEventResponse response = killEventDtoAssembler.toDto(sampleEntity);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.version()).isEqualTo(0L);
            assertThat(response.processName()).isEqualTo("malicious.exe");
            assertThat(response.uc()).isEqualTo(sampleMaterialResponse);
            assertThat(response.user()).isEqualTo(sampleUserSummaryResponse);
            assertThat(response.killDateTime()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 30));
            assertThat(response.result()).isTrue();
        }

        @Test
        @DisplayName("Doit gérer une entité sans UC et sans User")
        void toDto_WhenUcAndUserAreNull_ReturnsDtoWithNullRelations() {
            sampleEntity.setUc(null);
            sampleEntity.setUser(null);

            KillEventResponse response = killEventDtoAssembler.toDto(sampleEntity);

            assertThat(response).isNotNull();
            assertThat(response.uc()).isNull();
            assertThat(response.user()).isNull();
            assertThat(response.result()).isTrue();
        }
    }

    @Nested
    @DisplayName("Tests toEntity")
    class ToEntityTests {

        @Test
        @DisplayName("Doit retourner null quand la requête est null")
        void toEntity_WhenRequestIsNull_ReturnsNull() {
            assertThat(killEventDtoAssembler.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir correctement la requête en entité")
        void toEntity_Success() {
            KillEventRequest request = new KillEventRequest(
                    "malicious.exe", 1L, LocalDateTime.of(2026, 1, 15, 10, 30), true, 5L
            );

            KillEvent entity = killEventDtoAssembler.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getProcessName()).isEqualTo("malicious.exe");
            assertThat(entity.getKillDateTime()).isEqualTo(LocalDateTime.of(2026, 1, 15, 10, 30));
            assertThat(entity.getResult()).isTrue();
            assertThat(entity.getUc()).isNotNull();
            assertThat(entity.getUc().getId()).isEqualTo(1L);
            assertThat(entity.getUser()).isNotNull();
            assertThat(entity.getUser().getId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("Doit gérer une requête sans ucId ni userId")
        void toEntity_WhenUcIdAndUserIdAreNull() {
            KillEventRequest request = new KillEventRequest(
                    "malicious.exe", null, LocalDateTime.of(2026, 1, 15, 10, 30), false, null
            );

            KillEvent entity = killEventDtoAssembler.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getUc()).isNull();
            assertThat(entity.getUser()).isNull();
            assertThat(entity.getResult()).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests updateEntity")
    class UpdateEntityTests {

        @Test
        @DisplayName("Doit lever UnsupportedOperationException")
        void updateEntity_ThrowsUnsupportedOperationException() {
            KillEventRequest request = new KillEventRequest(
                    "malicious.exe", 1L, LocalDateTime.of(2026, 1, 15, 10, 30), true
            );

            assertThatThrownBy(() -> killEventDtoAssembler.updateEntity(sampleEntity, request))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("mise à jour d'un Kill Event n'est pas supportée");
        }
    }
}
