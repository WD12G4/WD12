package net.ent.etnc.dtos.assemblers;

import net.ent.etnc.dtos.application.ApplicationRequest;
import net.ent.etnc.dtos.application.ApplicationResponse;
import net.ent.etnc.models.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplicationDtoAssembler Unit Tests")
class ApplicationDtoAssemblerTest {

    private ApplicationDtoAssembler assembler;
    private Application sampleEntity;

    @BeforeEach
    void setUp() {
        assembler = new ApplicationDtoAssembler();

        sampleEntity = new Application();
        sampleEntity.setId(10L);
        sampleEntity.setVersion(0L);
        sampleEntity.setName("steam.exe");
        sampleEntity.setDisplayName("Steam");
        sampleEntity.setIconBase64("data:image/png;base64,ABC");
        sampleEntity.setBlacklisted(true);
    }

    @Nested
    @DisplayName("Tests toDto")
    class ToDtoTests {

        @Test
        @DisplayName("Doit retourner null quand l'entité est null")
        void toDto_WhenNull_ReturnsNull() {
            assertThat(assembler.toDto(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir correctement une entité en DTO")
        void toDto_Success() {
            ApplicationResponse dto = assembler.toDto(sampleEntity);

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(10L);
            assertThat(dto.version()).isEqualTo(0L);
            assertThat(dto.name()).isEqualTo("steam.exe");
            assertThat(dto.displayName()).isEqualTo("Steam");
            assertThat(dto.iconBase64()).isEqualTo("data:image/png;base64,ABC");
            assertThat(dto.blacklisted()).isTrue();
        }
    }

    @Nested
    @DisplayName("Tests toEntity")
    class ToEntityTests {

        @Test
        @DisplayName("Doit retourner null quand la requête est null")
        void toEntity_WhenNull_ReturnsNull() {
            assertThat(assembler.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("Doit convertir correctement une requête en entité")
        void toEntity_Success() {
            ApplicationRequest request = new ApplicationRequest(
                    "discord.exe", "Discord", "data:image/png;base64,XYZ", false
            );

            Application entity = assembler.toEntity(request);

            assertThat(entity).isNotNull();
            assertThat(entity.getName()).isEqualTo("discord.exe");
            assertThat(entity.getDisplayName()).isEqualTo("Discord");
            assertThat(entity.getIconBase64()).isEqualTo("data:image/png;base64,XYZ");
            assertThat(entity.isBlacklisted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests updateEntity")
    class UpdateEntityTests {

        @Test
        @DisplayName("Doit mettre à jour les champs de l'entité")
        void updateEntity_Success() {
            ApplicationRequest request = new ApplicationRequest(
                    "steam_updated.exe", "Steam Gaming", null, false
            );

            assembler.updateEntity(sampleEntity, request);

            assertThat(sampleEntity.getName()).isEqualTo("steam_updated.exe");
            assertThat(sampleEntity.getDisplayName()).isEqualTo("Steam Gaming");
            assertThat(sampleEntity.getIconBase64()).isEqualTo("data:image/png;base64,ABC"); // inchangé car null dans request
            assertThat(sampleEntity.isBlacklisted()).isFalse();
        }

        @Test
        @DisplayName("Ne doit pas lever d'erreur si entité ou requête est null")
        void updateEntity_WhenNull_DoesNotThrow() {
            assembler.updateEntity(null, new ApplicationRequest("a", "b", null, null));
            assembler.updateEntity(sampleEntity, null);

            assertThat(sampleEntity.getName()).isEqualTo("steam.exe");
        }
    }
}
