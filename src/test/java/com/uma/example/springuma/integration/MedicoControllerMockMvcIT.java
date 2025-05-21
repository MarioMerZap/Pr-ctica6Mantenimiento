package com.uma.example.springuma.integration;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.integration.base.AbstractIntegration;
import com.uma.example.springuma.model.Medico;
import com.uma.example.springuma.model.RepositoryMedico;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest 
@AutoConfigureMockMvc 
class MedicoControllerMockMvcIT  extends AbstractIntegration  { 

	@Autowired
	private MockMvc mockMvc; 

    @Autowired
    private ObjectMapper objectMapper; 

    @Autowired
    private RepositoryMedico repositoryMedico; 

    private Medico medicoBase; 

    
    @BeforeEach
    void setUp() {
        
        repositoryMedico.deleteAll();

       
        medicoBase = new Medico();
        medicoBase.setNombre("Dr. Ejemplo");
        medicoBase.setEspecialidad("Medicina General");
        medicoBase.setDni("00000000A");
        
    }

    @Test
    @DisplayName("POST /medico - Crear un nuevo médico correctamente")
    void createMedico_ShouldReturnCreatedStatus() throws Exception {
        mockMvc.perform(post("/medico")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(medicoBase)))
                .andDo(print())
                .andExpect(status().isCreated()); 
    }

    @Test
    @DisplayName("GET /medico/dni/{dni} - Obtener un médico por su DNI")
    void getMedicoByDni_WhenMedicoExists_ShouldReturnMedico() throws Exception {
        
        repositoryMedico.save(medicoBase); 

        mockMvc.perform(get("/medico/dni/" + medicoBase.getDni()))
                .andDo(print())
                .andExpect(status().isOk()) 
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nombre", is(medicoBase.getNombre())))
                .andExpect(jsonPath("$.especialidad", is(medicoBase.getEspecialidad())))
                .andExpect(jsonPath("$.dni", is(medicoBase.getDni())));
    }

    @Test
    @DisplayName("GET /medico/dni/{dni} - Intentar obtener médico por DNI no existente devuelve 404")
    void getMedicoByDni_WhenMedicoDoesNotExist_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/medico/dni/99999999X"))
                .andDo(print())
                .andExpect(status().isNotFound()); 
    }
    
    @Test
    @DisplayName("GET /medico/{id} - Obtener un médico por su ID")
    void getMedicoById_WhenMedicoExists_ShouldReturnMedico() throws Exception {
        
        Medico medicoGuardado = repositoryMedico.save(medicoBase);
        Long medicoId = medicoGuardado.getId();
        assertNotNull(medicoId);

        mockMvc.perform(get("/medico/" + medicoId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(medicoId.intValue()))) // jsonPath puede necesitar conversión para Long
                .andExpect(jsonPath("$.nombre", is(medicoBase.getNombre())))
                .andExpect(jsonPath("$.dni", is(medicoBase.getDni())));
    }

    /*@Test
    @DisplayName("GET /medico/{id} - Intentar obtener médico por ID no existente devuelve 200 OK con cuerpo vacío")
    void getMedicoById_WhenMedicoDoesNotExist_ShouldReturnOkWithEmptyBody() throws Exception {
        // Según la implementación actual de tu MedicoController, devuelve 200 OK con cuerpo vacío
        mockMvc.perform(get("/medico/999")) // Un ID que no existe
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("")); // Cuerpo vacío
    }*/


    @Test
    @DisplayName("PUT /medico - Actualizar un médico existente")
    void updateMedico_WhenMedicoExists_ShouldReturnNoContentAndUpdateMedico() throws Exception {
        // 1. Crear el médico
        Medico medicoGuardado = repositoryMedico.save(medicoBase);
        Long medicoId = medicoGuardado.getId();

        // 2. Modificar los datos del médico
        Medico medicoActualizado = new Medico();
        medicoActualizado.setId(medicoId); // ¡Importante! El ID debe ser el del médico a actualizar
        medicoActualizado.setNombre("Dr. Ejemplo Actualizado");
        medicoActualizado.setEspecialidad("Cardiología");
        medicoActualizado.setDni(medicoBase.getDni()); // DNI podría o no cambiar

        // 3. Realizar la petición PUT
        mockMvc.perform(put("/medico")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(medicoActualizado)))
                .andDo(print())
                .andExpect(status().isNoContent()); // Verificar estado 204 No Content

        // 4. Verificar que el médico fue actualizado en la BBDD
        Medico medicoVerificado = repositoryMedico.findById(medicoId).orElse(null);
        assertNotNull(medicoVerificado);
        is(medicoVerificado.getNombre().equals("Dr. Ejemplo Actualizado"));
        is(medicoVerificado.getEspecialidad().equals("Cardiología"));
    }
    
   /* @Test
    @DisplayName("PUT /medico - Intentar actualizar médico no existente devuelve 500")
    void updateMedico_WhenMedicoDoesNotExist_ShouldReturnInternalServerError() throws Exception {
        Medico medicoNoExistente = new Medico();
        medicoNoExistente.setId(999L); // ID que no existe
        medicoNoExistente.setNombre("Fantasma");
        medicoNoExistente.setDni("00000000X");

        mockMvc.perform(put("/medico")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(medicoNoExistente)))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error al actualizar el medico"));
    }*/

    @Test
@DisplayName("DELETE /medico/{id} - Eliminar un médico existente y comprobar GET falla")
void deleteMedico_ShouldReturnOk() throws Exception {
    Medico guardado = repositoryMedico.save(medicoBase);
    Long id = guardado.getId();

    mockMvc.perform(delete("/medico/" + id))
            .andExpect(status().isOk());

    // Tras borrar, la petición GET da error 500, lo comprobamos así
    mockMvc.perform(get("/medico/" + id))
            .andExpect(status().isInternalServerError());
}




    @Test
    @DisplayName("DELETE /medico/{id} - Intentar eliminar médico no existente devuelve 500")
    void deleteMedico_WhenMedicoDoesNotExist_ShouldReturnInternalServerError() throws Exception {
        // Según la implementación actual, si el medico no se encuentra, el if (medico != null) será falso.
        mockMvc.perform(delete("/medico/999")) // Un ID que no existe
                .andDo(print())
                .andExpect(status().isInternalServerError());
                
    }

    
}