package net.ent.etnc.controllers;

import net.ent.etnc.dtos.assemblers.RoomDtoAssembler;
import net.ent.etnc.dtos.building.BuildingResponse;
import net.ent.etnc.dtos.room.RoomRequest;
import net.ent.etnc.dtos.room.RoomResponse;
import net.ent.etnc.handlers.GlobalExceptionHandler;
import net.ent.etnc.models.Building;
import net.ent.etnc.models.Room;
import net.ent.etnc.services.RoomService;
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
@DisplayName("RoomController Web Tests")
class RoomControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoomService roomService;

    @Mock
    private RoomDtoAssembler roomAssembler;

    @InjectMocks
    private RoomController roomController;

    private Room sampleRoom;
    private RoomResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        Building building = new Building();
        building.setId(10L);
        building.setNumber(1);

        sampleRoom = new Room();
        sampleRoom.setId(1L);
        sampleRoom.setNumber(204);
        sampleRoom.setBuilding(building);

        sampleResponse = new RoomResponse(
                1L, 0L, 204, new BuildingResponse(10L, 0L, 1)
        );
    }

    @Nested
    @DisplayName("GET /api/v1/rooms")
    class GetAllTests {

        @Test
        @DisplayName("Doit retourner une page de salles")
        void getAll_ReturnsPageOfRooms() throws Exception {
            Page<Room> page = new PageImpl<>(List.of(sampleRoom), PageRequest.of(0, 20), 1);
            when(roomService.findAll(any(Pageable.class))).thenReturn(page);
            when(roomAssembler.toDto(sampleRoom)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/rooms")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].number").value(204))
                    .andExpect(jsonPath("$.content[0].building.number").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/rooms/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("Doit retourner la salle quand elle existe")
        void getById_WhenExists_ReturnsRoom() throws Exception {
            when(roomService.findById(1L)).thenReturn(Optional.of(sampleRoom));
            when(roomAssembler.toDto(sampleRoom)).thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/rooms/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.number").value(204));
        }

        @Test
        @DisplayName("Doit renvoyer 404 si la salle n'existe pas")
        void getById_WhenNotFound_Returns404() throws Exception {
            when(roomService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/rooms/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/rooms")
    class CreateTests {

        @Test
        @DisplayName("Doit créer la salle et retourner 201 Created")
        void create_ValidRequest_Returns201Created() throws Exception {
            String payload = """
                    {
                        "buildingId": 10,
                        "number": 204
                    }
                    """;

            when(roomAssembler.toEntity(any(RoomRequest.class))).thenReturn(sampleRoom);
            when(roomService.create(any(Room.class))).thenReturn(sampleRoom);
            when(roomAssembler.toDto(sampleRoom)).thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.number").value(204));
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si buildingId est absent")
        void create_NullBuildingId_Returns400() throws Exception {
            String payload = """
                    {
                        "number": 204
                    }
                    """;

            mockMvc.perform(post("/api/v1/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verify(roomService, never()).create(any());
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si le numéro est négatif")
        void create_NegativeNumber_Returns400() throws Exception {
            String payload = """
                    {
                        "buildingId": 10,
                        "number": -1
                    }
                    """;

            mockMvc.perform(post("/api/v1/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verify(roomService, never()).create(any());
        }

        @Test
        @DisplayName("Doit renvoyer 400 Bad Request si le numéro dépasse 999")
        void create_NumberGreaterThan999_Returns400() throws Exception {
            String payload = """
                    {
                        "buildingId": 10,
                        "number": 1000
                    }
                    """;

            mockMvc.perform(post("/api/v1/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verify(roomService, never()).create(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/rooms/{id}")
    class UpdateTests {

        @Test
        @DisplayName("Doit mettre à jour la salle et retourner 200 OK")
        void update_ValidRequest_Returns200Ok() throws Exception {
            String payload = """
                    {
                        "buildingId": 10,
                        "number": 205
                    }
                    """;

            when(roomService.findById(1L)).thenReturn(Optional.of(sampleRoom));
            doNothing().when(roomAssembler).updateEntity(eq(sampleRoom), any(RoomRequest.class));
            when(roomService.update(sampleRoom)).thenReturn(sampleRoom);
            when(roomAssembler.toDto(sampleRoom)).thenReturn(sampleResponse);

            mockMvc.perform(put("/api/v1/rooms/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("Doit retourner 404 si la salle à mettre à jour n'existe pas")
        void update_WhenNotFound_Returns404() throws Exception {
            String payload = """
                    {
                        "buildingId": 10,
                        "number": 205
                    }
                    """;

            when(roomService.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(put("/api/v1/rooms/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/rooms/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Doit supprimer la salle et retourner 204 No Content")
        void delete_WhenExists_Returns204NoContent() throws Exception {
            when(roomService.existsById(1L)).thenReturn(true);
            doNothing().when(roomService).deleteById(1L);

            mockMvc.perform(delete("/api/v1/rooms/1"))
                    .andExpect(status().isNoContent());

            verify(roomService).deleteById(1L);
        }

        @Test
        @DisplayName("Doit renvoyer 404 si la salle à supprimer n'existe pas")
        void delete_WhenNotFound_Returns404() throws Exception {
            when(roomService.existsById(999L)).thenReturn(false);

            mockMvc.perform(delete("/api/v1/rooms/999"))
                    .andExpect(status().isNotFound());

            verify(roomService, never()).deleteById(anyLong());
        }
    }
}
