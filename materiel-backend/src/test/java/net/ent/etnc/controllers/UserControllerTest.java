package net.ent.etnc.controllers;

import net.ent.etnc.dtos.assemblers.UserDtoAssembler;
import net.ent.etnc.dtos.user.UserCreateRequest;
import net.ent.etnc.dtos.user.UserResponse;
import net.ent.etnc.dtos.user.UserUpdateRequest;
import net.ent.etnc.handlers.GlobalExceptionHandler;
import net.ent.etnc.models.User;
import net.ent.etnc.models.enums.ERole;
import net.ent.etnc.services.UserService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Web Tests")
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private UserDtoAssembler userAssembler;

    @InjectMocks
    private UserController userController;

    private User sampleUser;
    private UserResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("alice");
        sampleUser.setRole(ERole.USER);
        sampleUser.setEnabled(true);

        sampleResponse = new UserResponse(1L, 0L, "alice", ERole.USER, true);
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class GetAllTests {

        @Test
        @DisplayName("Doit retourner une page d'utilisateurs")
        void getAll_ReturnsPageOfUsers() throws Exception {
            Page<User> page = new PageImpl<>(List.of(sampleUser), PageRequest.of(0, 20), 1);
            when(userService.findAll(any(Pageable.class))).thenReturn(page);
            when(userAssembler.toDto(sampleUser)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/users")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].username").value("alice"))
                    .andExpect(jsonPath("$.content[0].role").value("USER"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("Doit retourner l'utilisateur quand il existe")
        void getById_WhenExists_ReturnsUser() throws Exception {
            when(userService.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(userAssembler.toDto(sampleUser)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.username").value("alice"));
        }

        @Test
        @DisplayName("Doit renvoyer 404 si l'utilisateur n'existe pas")
        void getById_WhenNotFound_Returns404() throws Exception {
            when(userService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/users/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users")
    class CreateTests {

        @Test
        @DisplayName("Doit créer l'utilisateur et retourner 201 Created")
        void create_ValidRequest_Returns201Created() throws Exception {
            String payload = """
                    {
                        "username": "bob",
                        "role": "MANAGER"
                    }
                    """;

            User bob = new User();
            bob.setId(2L);
            bob.setUsername("bob");
            bob.setRole(ERole.MANAGER);
            UserResponse bobResponse = new UserResponse(2L, 0L, "bob", ERole.MANAGER, true);

            when(userAssembler.toEntity(any(UserCreateRequest.class))).thenReturn(bob);
            when(userService.create(any(User.class))).thenReturn(bob);
            when(userAssembler.toDto(bob)).thenReturn(bobResponse);

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.username").value("bob"))
                    .andExpect(jsonPath("$.role").value("MANAGER"));
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si username est vide")
        void create_BlankUsername_Returns400() throws Exception {
            String payload = """
                    {
                        "username": "",
                        "role": "USER"
                    }
                    """;

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).create(any());
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si le rôle est absent")
        void create_NullRole_Returns400() throws Exception {
            String payload = """
                    {
                        "username": "bob"
                    }
                    """;

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).create(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{id}")
    class UpdateTests {

        @Test
        @DisplayName("Doit mettre à jour l'utilisateur et retourner 200 OK")
        void update_ValidRequest_Returns200Ok() throws Exception {
            String payload = """
                    {
                        "role": "ADMIN",
                        "enabled": false
                    }
                    """;

            when(userService.findById(1L)).thenReturn(Optional.of(sampleUser));
            doNothing().when(userAssembler).updateEntity(eq(sampleUser), any(UserUpdateRequest.class));
            when(userService.update(sampleUser)).thenReturn(sampleUser);
            UserResponse updatedResponse = new UserResponse(1L, 0L, "alice", ERole.ADMIN, false);
            when(userAssembler.toDto(sampleUser)).thenReturn(updatedResponse);

            mockMvc.perform(put("/api/v1/users/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.enabled").value(false));
        }

        @Test
        @DisplayName("Doit retourner 404 si l'utilisateur à mettre à jour n'existe pas")
        void update_WhenNotFound_Returns404() throws Exception {
            String payload = """
                    {
                        "role": "ADMIN"
                    }
                    """;

            when(userService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/v1/users/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Doit supprimer l'utilisateur et retourner 204 No Content")
        void delete_WhenExists_Returns204NoContent() throws Exception {
            when(userService.existsById(1L)).thenReturn(true);
            doNothing().when(userService).deleteById(1L);

            mockMvc.perform(delete("/api/v1/users/1"))
                    .andExpect(status().isNoContent());

            verify(userService).deleteById(1L);
        }

        @Test
        @DisplayName("Doit renvoyer 404 si l'utilisateur à supprimer n'existe pas")
        void delete_WhenNotFound_Returns404() throws Exception {
            when(userService.existsById(999L)).thenReturn(false);

            mockMvc.perform(delete("/api/v1/users/999"))
                    .andExpect(status().isNotFound());

            verify(userService, never()).deleteById(anyLong());
        }
    }
}
