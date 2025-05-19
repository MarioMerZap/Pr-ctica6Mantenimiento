package com.uma.example.springuma.integration;

import com.uma.example.springuma.model.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SistemaClinicoIT {

    @Autowired
    private WebTestClient webTestClient;

    private Long medicoId;
    private Long pacienteId;
    private Long imagenId;
    private Long informeId;

    @Test
    void flujoCompletoDelSistema() {
        // 1. Crear médico
        Medico medico = new Medico();
        medico.setNombre("Dra. Ana");
        medico.setEspecialidad("Radiología");
        medico.setDni("12345678A");

        webTestClient.post().uri("/medico")
                .bodyValue(medico)
                .exchange()
                .expectStatus().isCreated();

        // 2. Obtener el médico por DNI
        Medico medicoResponse = webTestClient.get().uri("/medico/dni/12345678A")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Medico.class)
                .returnResult()
                .getResponseBody();

        medicoId = medicoResponse != null ? medicoResponse.getId() : null;
        Assertions.assertNotNull(medicoId);

        // 3. Crear paciente
        Paciente paciente = new Paciente();
        paciente.setNombre("Lucía Pérez");
        paciente.setDni("11223344B");
        // Asignar el medico completo, no solo ID
        Medico medicoAsociado = new Medico();
        medicoAsociado.setId(medicoId);
        paciente.setMedico(medicoAsociado);

        webTestClient.post().uri("/paciente")
                .bodyValue(paciente)
                .exchange()
                .expectStatus().isCreated();

        // 4. Obtener paciente para confirmar creación
        Paciente pacienteResponse = webTestClient.get().uri("/paciente/medico/" + medicoId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Paciente.class)
                .returnResult()
                .getResponseBody()
                .stream()
                .filter(p -> p.getDni().equals("11223344B"))
                .findFirst()
                .orElse(null);

        Assertions.assertNotNull(pacienteResponse);
        pacienteId = pacienteResponse.getId();

        // 5. Simulamos que ya existe una imagen con ID 1
        imagenId = 1L;

        // 6. Obtener objeto Imagen para asociar al Informe
        Imagen imagen = webTestClient.get()
            .uri("/imagen/info/" + imagenId)
            .exchange()
            .expectStatus().isOk()
            .expectBody(Imagen.class)
            .returnResult()
            .getResponseBody();

        Assertions.assertNotNull(imagen);

        // 7. Crear informe asociado a la imagen
        Informe informe = new Informe();
        informe.setContenido("No se detectan anomalías.");
        informe.setImagen(imagen);

        webTestClient.post().uri("/informe")
                .bodyValue(informe)
                .exchange()
                .expectStatus().isCreated();

        // 8. Consultar informes de imagen
        webTestClient.get().uri("/informe/imagen/" + imagenId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Informe.class)
                .value(list -> {
                    Assertions.assertFalse(list.isEmpty());
                    informeId = list.get(0).getId();
                });

        // 9. Borrar informe
        webTestClient.delete().uri("/informe/" + informeId)
                .exchange()
                .expectStatus().isNoContent();
    }
}
