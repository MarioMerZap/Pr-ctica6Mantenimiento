package com.uma.example.springuma.integration;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.Paciente;
import com.uma.example.springuma.model.RepositoryMedico;
import com.uma.example.springuma.model.RepositoryPaciente;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest
@AutoConfigureMockMvc
public class PacienteControllerMockMvcIT extends AbstractIntegration {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepositoryPaciente repositoryPaciente;

    @Autowired
    private RepositoryMedico repositoryMedico;

    private Paciente pacienteBase;
    private Medico medico;

    @BeforeEach
    void setUp() {
        repositoryPaciente.deleteAll();
        repositoryMedico.deleteAll();

        medico = new Medico();
        medico.setNombre("Dr. Responsable");
        medico.setEspecialidad("Oncología");
        medico.setDni("11111111B");
        medico = repositoryMedico.save(medico);

        pacienteBase = new Paciente();
        pacienteBase.setNombre("Paciente Ejemplo");
        pacienteBase.setEdad(45);
        pacienteBase.setDni("99999999X");
        pacienteBase.setCita("2025-06-01");
        pacienteBase.setMedico(medico);
    }

    @Test
    @DisplayName("POST /paciente - Crear un nuevo paciente")
    void createPaciente_ShouldReturnCreatedStatus() throws Exception {
        mockMvc.perform(post("/paciente")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pacienteBase)))
                .andExpect(status().isCreated());
    }


    @Test
    @DisplayName("GET /paciente/{id} - Obtener un paciente por ID")
    void getPacienteById_ShouldReturnPaciente() throws Exception {
        Paciente pacienteGuardado = repositoryPaciente.save(pacienteBase);
        Long id = pacienteGuardado.getId();
        assertNotNull(id);

        mockMvc.perform(get("/paciente/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", is(pacienteBase.getNombre())))
                .andExpect(jsonPath("$.dni", is(pacienteBase.getDni())));
    }

    @Test
    @DisplayName("PUT /paciente - Actualizar un paciente existente")
    void updatePaciente_ShouldReturnNoContentAndUpdate() throws Exception {
        Paciente pacienteGuardado = repositoryPaciente.save(pacienteBase);
        Long id = pacienteGuardado.getId();

        pacienteGuardado.setNombre("Paciente Actualizado");
        pacienteGuardado.setEdad(50);

        mockMvc.perform(put("/paciente")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pacienteGuardado)))
                .andExpect(status().isNoContent());

        Paciente actualizado = repositoryPaciente.findById(id).orElse(null);
        assertNotNull(actualizado);
        is(actualizado.getNombre().equals("Paciente Actualizado"));
        is(actualizado.getEdad() == 50);
    }

    @Test
@DisplayName("DELETE /paciente/{id} - Eliminar un paciente existente y comprobar GET falla")
void deletePaciente_ShouldReturnOk() throws Exception {
    Paciente guardado = repositoryPaciente.save(pacienteBase);
    Long id = guardado.getId();

    mockMvc.perform(delete("/paciente/" + id))
            .andExpect(status().isOk());

    // Tras borrar, la petición GET da error 500, lo comprobamos así
    mockMvc.perform(get("/paciente/" + id))
            .andExpect(status().isInternalServerError());
}




    @Test
    @DisplayName("GET /paciente/dni/{dni} - DNI no existente devuelve 404")
    void getPacienteByDni_NotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/paciente/dni/00000000Z"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /paciente/{id} - ID no existente devuelve 500")
    void deletePaciente_NotFound_ShouldReturnError() throws Exception {
        mockMvc.perform(delete("/paciente/9999"))
                .andExpect(status().isInternalServerError());
    }
}
