package net.ent.etnc.controllers;

import net.ent.etnc.handlers.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialTypesController Web Tests")
class MaterialTypesControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MaterialTypesController controller = new MaterialTypesController();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Doit renvoyer la liste complète de tous les types de matériels")
    void getAll_ReturnsAllMaterialTypes() throws Exception {
        mockMvc.perform(get("/api/v1/material-types/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$", hasItems("UC", "SCREEN", "PROJECTOR", "PERIPHERAL")));
    }
}
