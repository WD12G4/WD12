package net.ent.etnc.controllers;

import net.ent.etnc.dtos.assemblers.UserDtoAssembler;
import net.ent.etnc.dtos.user.UserResponse;
import net.ent.etnc.handlers.GlobalExceptionHandler;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Web Tests")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private UserDtoAssembler userDtoAssembler;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(userService, userDtoAssembler);

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("admin");
        sampleUser.setRole(ERole.ADMIN);
    }

    @Nested
    @DisplayName("Tests du endpoint /api/v1/auth/me/")
    class MeTests {

        @Test
        @DisplayName("Renvoie le profil de l'utilisateur connecté")
        void me_ReturnsUserProfile() throws Exception {
            UserResponse response = new UserResponse(1L, 0L, "admin", ERole.ADMIN, true);

            when(userService.currentUser()).thenReturn(sampleUser);
            when(userDtoAssembler.toDto(sampleUser)).thenReturn(response);

            mockMvc.perform(get("/api/v1/auth/me/"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }
    }
}
