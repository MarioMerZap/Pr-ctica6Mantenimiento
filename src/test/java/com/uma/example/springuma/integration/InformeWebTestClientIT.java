package com.uma.example.springuma.integration;

import com.uma.example.springuma.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.TestInstance; // Elimina o cambia a PER_METHOD
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Necesario para WebTestClient con servidor real
@AutoConfigureWebTestClient // Para que Spring configure y provea el bean WebTestClient
// @TestInstance(TestInstance.Lifecycle.PER_CLASS) // <-- CAUSA DEL PROBLEMA, ELIMINAR O CAMBIAR
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) // Para limpiar el contexto después de cada test
public class InformeWebTestClientIT { // Sugerencia de nombre: InformeWebTestClientIT

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RepositoryInforme repositoryInforme;

    @Autowired
    private RepositoryImagen repositoryImagen;

    private Imagen imagen; // Se inicializa en @BeforeEach

    @BeforeEach
    void setup() {

        repositoryInforme.deleteAll(); 
        repositoryImagen.deleteAll();
        
        Imagen imagenTemp = new Imagen();
        imagenTemp.setNombre("testImageForInforme");

        imagen = repositoryImagen.save(imagenTemp);
    }

    @Test
    @DisplayName("POST /informe - Crear informe")
    void createInforme_ShouldReturnCreated() {
        Informe informe = new Informe();
        informe.setContenido("Descripcion de prueba para informe");
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
        // informe específico para este test para asegurar que existe
        Informe informeParaBorrar = new Informe();
        informeParaBorrar.setContenido("Informe a ser borrado");
        informeParaBorrar.setImagen(imagen); 
        Informe saved = repositoryInforme.save(informeParaBorrar);

        webTestClient.delete()
                .uri("/informe/" + saved.getId())
                .exchange()
                .expectStatus().isNoContent();
    }
}