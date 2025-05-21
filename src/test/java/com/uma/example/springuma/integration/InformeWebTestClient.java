package com.uma.example.springuma.integration;

import com.uma.example.springuma.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@SpringBootTest
@AutoConfigureWebTestClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class InformeWebTestClient {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RepositoryInforme repositoryInforme;

    @Autowired
    private RepositoryImagen repositoryImagen;

    private Imagen imagen;

    @BeforeEach
    void setup() {
        repositoryInforme.deleteAll();
        repositoryImagen.deleteAll();
        Imagen imagenTemp = new Imagen();
        imagenTemp.setNombre("testImage");
        
        imagen = repositoryImagen.save(imagenTemp);
    }

    @Test
    @DisplayName("POST /informe - Crear informe")
    void createInforme_ShouldReturnCreated() {
        Informe informe = new Informe();
        informe.setContenido("Descripcion de prueba");  // Aquí corregido
        informe.setImagen(imagen);

        webTestClient.post()
                .uri("/informe")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(informe), Informe.class)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    @DisplayName("DELETE /informe/{id} - Eliminar informe")
    void deleteInforme_ShouldReturnNoContent() {
        Informe saved = repositoryInforme.save(new Informe(null, "desc", imagen));

        webTestClient.delete()
                .uri("/informe/" + saved.getId())
                .exchange()
                .expectStatus().isNoContent();
    }
}
