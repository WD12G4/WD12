package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.auth.UserSummaryResponse;
import net.ent.etnc.dtos.material.MaterialResponse;
import net.ent.etnc.dtos.report.ReportCreateRequest;
import net.ent.etnc.dtos.report.ReportResponse;
import net.ent.etnc.dtos.report.ReportUpdateRequest;
import net.ent.etnc.models.Material;
import net.ent.etnc.models.Report;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportDtoAssembler Unit Tests")
class ReportDtoAssemblerTest {

    @Mock
    private MaterialDtoAssembler materialDtoAssembler;

    @Mock
    private UserDtoAssembler userDtoAssembler;

    @InjectMocks
    private ReportDtoAssembler reportDtoAssembler;

    private Report sampleEntity;
    private User sampleUser;
    private Material sampleMaterial;
    private UserSummaryResponse sampleUserSummary;
    private MaterialResponse sampleMaterialResponse;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("alice");
        sampleUser.setRole(ERole.USER);

        sampleMaterial = new Material();
        sampleMaterial.setId(10L);
        sampleMaterial.setSerialNumber("UC-ALICE");
        sampleMaterial.setMaterialType(EMaterialType.UC);

        sampleEntity = new Report();
        sampleEntity.setId(50L);
        sampleEntity.setVersion(0L);
        sampleEntity.setDescription("Ventilateur bruyant");
        sampleEntity.setUser(sampleUser);
        sampleEntity.setMaterial(sampleMaterial);
        sampleEntity.setReportDateTime(LocalDateTime.of(2026, 3, 1, 9, 30));
        sampleEntity.setHandled(false);

        sampleUserSummary = new UserSummaryResponse(1L, "alice", ERole.USER);
        sampleMaterialResponse = new MaterialResponse(
                10L, 0L, "UC-ALICE", EMaterialType.UC, LocalDate.now().plusYears(1), null, false
        );
    }

    @Nested
    @DisplayName("Tests toDto")
    class ToDtoTests {

        @Test
        @DisplayName("Doit retourner null si l'entité est null")
        void toDto_WhenNull_ReturnsNull() {
            assertThat(reportDtoAssembler.toDto(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir correctement l'entité en DTO")
        void toDto_Success() {
            when(materialDtoAssembler.toDto(sampleMaterial)).thenReturn(sampleMaterialResponse);
            when(userDtoAssembler.toSummaryDto(sampleUser)).thenReturn(sampleUserSummary);

            ReportResponse response = reportDtoAssembler.toDto(sampleEntity);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(50L);
            assertThat(response.version()).isEqualTo(0L);
            assertThat(response.description()).isEqualTo("Ventilateur bruyant");
            assertThat(response.user()).isEqualTo(sampleUserSummary);
            assertThat(response.material()).isEqualTo(sampleMaterialResponse);
            assertThat(response.handled()).isFalse();
            assertThat(response.handleDateTime()).isNull();
        }

        @Test
        @DisplayName("Doit gérer une entité avec user et material nuls")
        void toDto_WhenUserAndMaterialNull_ReturnsDtoWithNulls() {
            sampleEntity.setUser(null);
            sampleEntity.setMaterial(null);

            ReportResponse response = reportDtoAssembler.toDto(sampleEntity);

            assertThat(response).isNotNull();
            assertThat(response.user()).isNull();
            assertThat(response.material()).isNull();
        }
    }

    @Nested
    @DisplayName("Tests toEntity")
    class ToEntityTests {

        @Test
        @DisplayName("Doit retourner null si la requête est null")
        void toEntity_WhenNull_ReturnsNull() {
            assertThat(reportDtoAssembler.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir correctement la requête en entité")
        void toEntity_Success() {
            ReportCreateRequest request = new ReportCreateRequest("Souris cassée", 10L);

            Report entity = reportDtoAssembler.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getDescription()).isEqualTo("Souris cassée");
            assertThat(entity.getMaterial()).isNotNull();
            assertThat(entity.getMaterial().getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Doit gérer une requête sans materialId")
        void toEntity_WhenMaterialIdNull_MaterialIsNull() {
            ReportCreateRequest request = new ReportCreateRequest("Problème global", null);

            Report entity = reportDtoAssembler.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getMaterial()).isNull();
        }
    }

    @Nested
    @DisplayName("Tests updateEntity")
    class UpdateEntityTests {

        @Test
        @DisplayName("Doit mettre à jour la description de l'entité")
        void updateEntity_Success() {
            ReportUpdateRequest request = new ReportUpdateRequest("Nouvelle description révisée");

            reportDtoAssembler.updateEntity(sampleEntity, request);

            assertThat(sampleEntity.getDescription()).isEqualTo("Nouvelle description révisée");
        }

        @Test
        @DisplayName("Doit ignorer si requête ou entité est null")
        void updateEntity_WhenNull_DoesNotThrow() {
            reportDtoAssembler.updateEntity(null, new ReportUpdateRequest("Desc"));
            reportDtoAssembler.updateEntity(sampleEntity, null);

            assertThat(sampleEntity.getDescription()).isEqualTo("Ventilateur bruyant");
        }
    }
}
