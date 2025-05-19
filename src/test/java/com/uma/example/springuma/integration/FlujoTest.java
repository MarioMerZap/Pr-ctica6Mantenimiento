package com.uma.example.springuma.integration;

import com.uma.example.springuma.model.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.file.Files;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ImagenInformeIT {

    @Autowired
    private WebTestClient webTestClient;

    private Long medicoId;
    private Long pacienteId;
    private Long imagenId;
    private Long informeId;

    @Test
    void testFlujoImagenPrediccionInforme() throws Exception {
        // 1. Crear medico (ya que paciente depende)
        Medico medico = new Medico();
        medico.setNombre("Dra. Ana");
        medico.setEspecialidad("Radiología");
        medico.setDni("12345678A");

        webTestClient.post().uri("/medico")
                .bodyValue(medico)
                .exchange()
                .expectStatus().isCreated();

        // 2. Obtener medico para id
        Medico[] medicos = webTestClient.get().uri("/medico/dni/12345678A")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Medico[].class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(medicos);
        medicoId = medicos[0].getId();

        // 3. Crear paciente asociado
        Paciente paciente = new Paciente();
        paciente.setNombre("Lucía Pérez");
        paciente.setDni("11223344B");
        // Asociar médico
        Medico m = new Medico();
        m.setId(medicoId);
        paciente.setMedico(m);

        webTestClient.post().uri("/paciente")
                .bodyValue(paciente)
                .exchange()
                .expectStatus().isCreated();

        // 4. Obtener paciente para usarlo en imagen
        Paciente[] pacientes = webTestClient.get().uri("/paciente/medico/" + medicoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Paciente[].class)
                .returnResult()
                .getResponseBody();

        pacienteId = null;
        for (Paciente p : pacientes) {
            if ("11223344B".equals(p.getDni())) {
                pacienteId = p.getId();
                break;
            }
        }
        Assertions.assertNotNull(pacienteId);

        // 5. Preparar archivo imagen
        ClassPathResource imageResource = new ClassPathResource("test-image.png");
        byte[] bytes = Files.readAllBytes(imageResource.getFile().toPath());

        // 6. Multipart para subir imagen + paciente JSON
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imageResource);
        // En el multipart, el paciente se pasa como JSON string
        String pacienteJson = "{\"id\":" + pacienteId + ",\"nombre\":\"" + paciente.getNombre() + "\",\"dni\":\"" + paciente.getDni() + "\",\"medico\":{\"id\":" + medicoId + "}}";
        body.add("paciente", pacienteJson);

        // 7. Subir imagen
        String uploadResponse = webTestClient.post().uri("/imagen")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        // OJO: Ajustar según lo que devuelve el servicio (supongamos devuelve el id en texto)
        imagenId = Long.parseLong(uploadResponse.trim());

        Assertions.assertNotNull(imagenId);

        // 8. Hacer la predicción
        String prediccion = webTestClient.get().uri("/imagen/predict/" + imagenId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertTrue(prediccion.contains("cancer") || prediccion.contains("no cancer"));

        // 9. Crear informe con imagen
        Informe informe = new Informe();
        informe.setContenido("Informe ejemplo.");
        Imagen imagen = new Imagen();
        imagen.setId(imagenId);
        informe.setImagen(imagen);

        webTestClient.post().uri("/informe")
                .bodyValue(informe)
                .exchange()
                .expectStatus().isCreated();

        // 10. Consultar informes de la imagen
        Informe[] informes = webTestClient.get().uri("/informe/imagen/" + imagenId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Informe[].class)
                .returnResult()
                .getResponseBody();

        Assertions.assertTrue(informes.length > 0);

        informeId = informes[0].getId();

        // 11. Eliminar informe
        webTestClient.delete().uri("/informe/" + informeId)
                .exchange()
                .expectStatus().isNoContent();

        // Opcional: borrar imagen, paciente, medico...
    }
}
