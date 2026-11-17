package net.ent.etnc.controllers;

import net.ent.etnc.dtos.application.ApplicationBlacklistRequest;
import net.ent.etnc.dtos.application.ApplicationResponse;
import net.ent.etnc.dtos.assemblers.ApplicationDtoAssembler;
import net.ent.etnc.handlers.GlobalExceptionHandler;
import net.ent.etnc.models.Application;
import net.ent.etnc.services.ApplicationService;
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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationController Web Tests")
class ApplicationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private ApplicationDtoAssembler applicationAssembler;

    @InjectMocks
    private ApplicationController applicationController;

    private Application sampleApp;
    private ApplicationResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(applicationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        sampleApp = new Application();
        sampleApp.setId(1L);
        sampleApp.setName("steam.exe");
        sampleApp.setDisplayName("Steam");
        sampleApp.setIconBase64("data:image/png;base64,ABC");
        sampleApp.setBlacklisted(true);

        sampleResponse = new ApplicationResponse(
                1L, 0L, "steam.exe", "Steam", "data:image/png;base64,ABC", true
        );
    }

    @Nested
    @DisplayName("GET /api/v1/applications")
    class GetAllTests {

        @Test
        @DisplayName("Doit retourner une page d'applications")
        void getAll_ReturnsPage() throws Exception {
            Page<Application> page = new PageImpl<>(List.of(sampleApp), PageRequest.of(0, 20), 1);
            when(applicationService.findAll(any(Pageable.class), eq(null))).thenReturn(page);
            when(applicationAssembler.toDto(sampleApp)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/applications")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("steam.exe"))
                    .andExpect(jsonPath("$.content[0].blacklisted").value(true));
        }

        @Test
        @DisplayName("Doit filtrer par blacklisted si demandé")
        void getAll_WithBlacklistFilter_ReturnsFilteredPage() throws Exception {
            Page<Application> page = new PageImpl<>(List.of(sampleApp), PageRequest.of(0, 20), 1);
            when(applicationService.findAll(any(Pageable.class), eq(true))).thenReturn(page);
            when(applicationAssembler.toDto(sampleApp)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/applications")
                            .param("blacklisted", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("steam.exe"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/applications/{id}/blacklist")
    class ToggleBlacklistTests {

        @Test
        @DisplayName("Doit basculer le statut et retourner 200 OK")
        void toggleBlacklist_Success() throws Exception {
            String payload = """
                    {
                        "blacklisted": false
                    }
                    """;

            Application unblacklisted = new Application();
            unblacklisted.setId(1L);
            unblacklisted.setName("steam.exe");
            unblacklisted.setBlacklisted(false);

            ApplicationResponse unblacklistedDto = new ApplicationResponse(
                    1L, 0L, "steam.exe", "Steam", null, false
            );

            when(applicationService.toggleBlacklist(1L, false)).thenReturn(unblacklisted);
            when(applicationAssembler.toDto(unblacklisted)).thenReturn(unblacklistedDto);

            mockMvc.perform(patch("/api/v1/applications/1/blacklist")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.blacklisted").value(false));
        }
    }

    @Nested
    @DisplayName("Tests de sécurité des rôles (RG-27)")
    class SecurityTests {

        @Test
        @DisplayName("toggleBlacklist doit être restreint aux administrateurs via @PreAuthorize(\"hasRole('ADMIN')\")")
        void toggleBlacklist_MustBeProtectedByAdminRole() throws NoSuchMethodException {
            PreAuthorize annotation = ApplicationController.class.getMethod("toggleBlacklist", Long.class, ApplicationBlacklistRequest.class)
                    .getAnnotation(PreAuthorize.class);
            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
        }

        @Test
        @DisplayName("getAll doit être accessible sans restriction ADMIN")
        void readMethods_MustNotRequireAdmin() throws NoSuchMethodException {
            PreAuthorize getAllAnnotation = ApplicationController.class.getMethod("getAll", Boolean.class, Pageable.class)
                    .getAnnotation(PreAuthorize.class);
            assertThat(getAllAnnotation).isNull();
        }
    }
}
