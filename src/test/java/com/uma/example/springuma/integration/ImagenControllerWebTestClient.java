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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ImagenControllerWebTestClient {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RepositoryPaciente repositoryPaciente;

    @Autowired
    private RepositoryImagen repositoryImagen;

    private Paciente paciente;

    @BeforeEach
    void setup() {
        repositoryImagen.deleteAll();
        repositoryPaciente.deleteAll();
        paciente = repositoryPaciente.save(new Paciente("Paciente Imagen", 30, "2025-05-21", "12345678Z", null));

    }

    @Test
    @DisplayName("POST /imagen - Subir imagen para un paciente")
    void uploadImage_ShouldReturnOk() throws IOException {
        ClassPathResource image = new ClassPathResource("healthy.png");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", image);
        builder.part("paciente", new ObjectMapper().writeValueAsString(paciente))
                .header("Content-Type", "application/json");

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
        // Subir imagen primero
        ClassPathResource image = new ClassPathResource("nohealthy.png");

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("image", image);
        builder.part("paciente", new ObjectMapper().writeValueAsString(paciente))
                .header("Content-Type", "application/json");

        webTestClient.post()
                .uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectStatus().isOk();

                List<Imagen> imagenes = repositoryImagen.getByPacienteId(paciente.getId());

        assertThat(imagenes).hasSize(1);

        Long id = imagenes.get(0).getId();

        webTestClient.delete()
                .uri("/imagen/" + id)
                .exchange()
                .expectStatus().isNoContent();
    }
}
