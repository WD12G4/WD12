package net.ent.etnc.services.impl;

import net.ent.etnc.models.Application;
import net.ent.etnc.repositories.ApplicationRepository;
import net.ent.etnc.services.commons.ResourceNotFoundException;
import net.ent.etnc.services.commons.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationServiceImpl Unit Tests")
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository repository;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private Application sampleApp;

    @BeforeEach
    void setUp() {
        sampleApp = new Application();
        sampleApp.setId(1L);
        sampleApp.setName("steam.exe");
        sampleApp.setDisplayName("Steam");
        sampleApp.setIconBase64("data:image/png;base64,ABC");
        sampleApp.setBlacklisted(false);
    }

    @Nested
    @DisplayName("Tests findByName et isBlacklisted")
    class SearchTests {

        @Test
        @DisplayName("findByName retourne l'application quand elle existe")
        void findByName_WhenExists_ReturnsApp() {
            when(repository.findByNameIgnoreCase("steam.exe")).thenReturn(Optional.of(sampleApp));

            Optional<Application> result = applicationService.findByName("steam.exe");

            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("steam.exe");
        }

        @Test
        @DisplayName("isBlacklisted retourne true si l'application est sur liste noire")
        void isBlacklisted_WhenBlacklisted_ReturnsTrue() {
            sampleApp.setBlacklisted(true);
            when(repository.findByNameIgnoreCase("steam.exe")).thenReturn(Optional.of(sampleApp));

            boolean result = applicationService.isBlacklisted("steam.exe");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isBlacklisted retourne false si l'application n'est pas trouvée ou non blacklistée")
        void isBlacklisted_WhenNotBlacklistedOrUnknown_ReturnsFalse() {
            when(repository.findByNameIgnoreCase("unknown.exe")).thenReturn(Optional.empty());

            assertThat(applicationService.isBlacklisted("unknown.exe")).isFalse();
            assertThat(applicationService.isBlacklisted(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests toggleBlacklist")
    class ToggleBlacklistTests {

        @Test
        @DisplayName("Doit basculer le statut blacklisté avec succès")
        void toggleBlacklist_Success() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleApp));
            when(repository.saveAndFlush(any(Application.class))).thenReturn(sampleApp);

            Application updated = applicationService.toggleBlacklist(1L, true);

            assertThat(updated.isBlacklisted()).isTrue();
            verify(repository).saveAndFlush(sampleApp);
        }

        @Test
        @DisplayName("Doit refuser de blacklister un processus système protégé (ex: explorer.exe)")
        void toggleBlacklist_ThrowsException_WhenProtectedSystemProcess() {
            Application explorer = new Application();
            explorer.setId(2L);
            explorer.setName("explorer.exe");
            explorer.setBlacklisted(false);

            when(repository.findById(2L)).thenReturn(Optional.of(explorer));

            assertThatThrownBy(() -> applicationService.toggleBlacklist(2L, true))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("Impossible d'interdire un composant système protégé");

            verify(repository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Doit lever ResourceNotFoundException si l'application n'existe pas")
        void toggleBlacklist_ThrowsNotFound_WhenAppNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationService.toggleBlacklist(99L, true))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Tests registerDiscoveredApplication")
    class AutoDiscoveryTests {

        @Test
        @DisplayName("Doit créer une nouvelle application auto-découverte si inexistante")
        void registerDiscoveredApplication_WhenNew_CreatesApplication() {
            when(repository.findByNameIgnoreCase("discord.exe")).thenReturn(Optional.empty());
            when(repository.saveAndFlush(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Application created = applicationService.registerDiscoveredApplication("discord.exe", "data:image/png;base64,DISCORD");

            assertThat(created).isNotNull();
            assertThat(created.getName()).isEqualTo("discord.exe");
            assertThat(created.getDisplayName()).isEqualTo("Discord");
            assertThat(created.getIconBase64()).isEqualTo("data:image/png;base64,DISCORD");
            assertThat(created.isBlacklisted()).isFalse();
            verify(repository).saveAndFlush(any(Application.class));
        }

        @Test
        @DisplayName("Doit enrichir l'icône si l'application existante n'en avait pas")
        void registerDiscoveredApplication_WhenExistingWithoutIcon_UpdatesIcon() {
            sampleApp.setIconBase64(null);
            when(repository.findByNameIgnoreCase("steam.exe")).thenReturn(Optional.of(sampleApp));
            when(repository.saveAndFlush(sampleApp)).thenReturn(sampleApp);

            Application result = applicationService.registerDiscoveredApplication("steam.exe", "data:image/png;base64,NEW_ICON");

            assertThat(result.getIconBase64()).isEqualTo("data:image/png;base64,NEW_ICON");
            verify(repository).saveAndFlush(sampleApp);
        }

        @Test
        @DisplayName("Ne doit pas écraser l'icône si elle est déjà présente")
        void registerDiscoveredApplication_WhenExistingWithIcon_KeepsExistingIcon() {
            when(repository.findByNameIgnoreCase("steam.exe")).thenReturn(Optional.of(sampleApp));

            Application result = applicationService.registerDiscoveredApplication("steam.exe", "data:image/png;base64,OTHER");

            assertThat(result.getIconBase64()).isEqualTo("data:image/png;base64,ABC");
            verify(repository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Tests findAll avec filtre")
    class FindAllTests {

        @Test
        @DisplayName("Doit filtrer par blacklisted si le paramètre est présent")
        void findAll_WithFilter_CallsFindByBlacklisted() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Application> page = new PageImpl<>(List.of(sampleApp));
            when(repository.findByBlacklisted(true, pageable)).thenReturn(page);

            Page<Application> result = applicationService.findAll(pageable, true);

            assertThat(result).hasSize(1);
            verify(repository).findByBlacklisted(true, pageable);
        }

        @Test
        @DisplayName("Doit appeler repository.findAll si le filtre est null")
        void findAll_WithoutFilter_CallsFindAll() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Application> page = new PageImpl<>(List.of(sampleApp));
            when(repository.findAll(pageable)).thenReturn(page);

            Page<Application> result = applicationService.findAll(pageable, null);

            assertThat(result).hasSize(1);
            verify(repository).findAll(pageable);
        }
    }
}
