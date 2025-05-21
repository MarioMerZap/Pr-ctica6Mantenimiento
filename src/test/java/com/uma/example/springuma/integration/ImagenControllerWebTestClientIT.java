package com.uma.example.springuma.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uma.example.springuma.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap; 


import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ImagenControllerWebTestClientIT { 

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RepositoryPaciente repositoryPaciente;

    @Autowired
    private RepositoryImagen repositoryImagen;

    @Autowired 
    private ObjectMapper objectMapper;

    private Paciente paciente;

    @BeforeEach
    void setup() throws IOException { 
        
        Paciente nuevoPaciente = new Paciente();
        nuevoPaciente.setNombre("Paciente Imagen");
        
        nuevoPaciente.setDni("12345678Z");
        paciente = repositoryPaciente.save(nuevoPaciente);
    }

    @Test
    @DisplayName("POST /imagen - Subir imagen para un paciente")
    void uploadImage_ShouldReturnOk() throws IOException {
        ClassPathResource imageResource = new ClassPathResource("healthy.png"); 
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", imageResource);
        builder.part("paciente", objectMapper.writeValueAsString(paciente))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE); 

        webTestClient.post()
                .uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectStatus().isOk();
    }

     @Test
     @DisplayName("DELETE /imagen/{id} - Eliminar imagen existente")
      void deleteImage_ShouldReturnNoContent() throws IOException {
        // 1. Subir imagen primero para tener algo que borrar
        ClassPathResource imageResource = new ClassPathResource("nohealthy.png");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", imageResource);
        builder.part("paciente", objectMapper.writeValueAsString(paciente))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        webTestClient.post()
                .uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectStatus().isOk(); // Solo verificamos que la subida fue exitosa

        // 2. AHORA, CONSULTAR LA BASE DE DATOS PARA OBTENER EL ID DE LA IMAGEN CREADA
        // Asumimo que solo se ha subido una imagen para este paciente en este test
        List<Imagen> imagenesDelPaciente = repositoryImagen.getByPacienteId(paciente.getId());
        
        // Verifico que se creó al menos una imagen y tomar la primera (o la última si es más apropiado)
        assertThat(imagenesDelPaciente).isNotEmpty(); 
        Long imagenId = imagenesDelPaciente.get(imagenesDelPaciente.size() - 1).getId(); // Tomar la última imagen subida
        assertNotNull(imagenId, "No se pudo obtener el ID de la imagen de la base de datos");


        // 3. Verifico que la imagen existe en la BBDD antes de borrar (opcional pero bueno para la confianza)
        List<Imagen> imagenesAntesDelete = repositoryImagen.getByPacienteId(paciente.getId());
        assertThat(imagenesAntesDelete).extracting(Imagen::getId).contains(imagenId);


        // 4. Elimino la imagen usando el ID obtenido de la BBDD
        webTestClient.delete()
                .uri("/imagen/" + imagenId)
                .exchange()
                .expectStatus().isNoContent();

        //Verifico que la imagen ya no existe en la BBDD
        List<Imagen> imagenesDespuesDelete = repositoryImagen.getByPacienteId(paciente.getId());
        assertThat(imagenesDespuesDelete).extracting(Imagen::getId).doesNotContain(imagenId);
}
}