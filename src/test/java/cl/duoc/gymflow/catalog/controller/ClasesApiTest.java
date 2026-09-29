package cl.duoc.gymflow.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.gymflow.catalog.support.PruebaApiBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ClasesApiTest extends PruebaApiBase {

    /**
     * Contrato con el frontend y con reservations: estos son exactamente los campos de una clase.
     * Si se renombra uno, esta prueba falla antes de que se rompa una pantalla.
     */
    @Test
    void crear_devuelve201_conLosCamposDelContrato() throws Exception {
        long sala = crearSala("Sala Spinning", 20);

        JsonNode clase = leer(postJson("/api/catalog/services", clase(sala, 12, MANANA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/catalog/services/"))));

        List<String> campos = new ArrayList<>();
        clase.fieldNames().forEachRemaining(campos::add);
        assertThat(campos).containsExactly("id", "name", "description", "instructor", "roomId", "roomName",
                "branch", "startsAt", "endsAt", "durationMinutes", "capacity", "availableSlots", "occupiedSlots",
                "plan");
        assertThat(clase.get("roomName").asText()).isEqualTo("Sala Spinning");
        assertThat(clase.get("capacity").asInt()).isEqualTo(12);
        assertThat(clase.get("availableSlots").asInt()).isEqualTo(12);
        assertThat(clase.get("occupiedSlots").asInt()).isZero();
        assertThat(clase.get("startsAt").asText()).isEqualTo(MANANA.toString());
        assertThat(clase.get("endsAt").asText()).isEqualTo(MANANA.plus(45, ChronoUnit.MINUTES).toString());
    }

    @Test
    void datosFaltantes_responde400_conTodosLosCampos() throws Exception {
        postJson("/api/catalog/services", Map.of("name", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("name: El nombre es obligatorio")))
                .andExpect(jsonPath("$.message", containsString("roomId: La sala es obligatoria")))
                .andExpect(jsonPath("$.message", containsString("plan: El plan es obligatorio")));
    }

    @Test
    void planInexistente_responde400() throws Exception {
        Map<String, Object> cuerpo = clase(crearSala("Sala A", 20), 10, MANANA);
        cuerpo.put("plan", "ORO");

        postJson("/api/catalog/services", cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El valor 'ORO' no es válido para 'plan'"));
    }

    @Test
    void cupoMayorQueLaSala_responde400() throws Exception {
        postJson("/api/catalog/services", clase(crearSala("Sala A", 20), 25, MANANA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("supera la capacidad de la sala 'Sala A' (20)")));
    }

    @Test
    void salaInexistente_responde400() throws Exception {
        postJson("/api/catalog/services", clase(999, 10, MANANA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La sala 999 no existe"));
    }

    @Test
    void claseInexistente_responde404() throws Exception {
        mvc.perform(get("/api/catalog/services/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La clase 999 no existe"));
    }

    @Test
    void listar_filtraPorRangoDeFechasYSala_ordenadoPorInicio() throws Exception {
        long salaA = crearSala("Sala A", 20);
        long salaB = crearSala("Sala B", 20);
        crearClase(salaA, 10, MANANA.plus(3, ChronoUnit.HOURS));
        crearClase(salaA, 10, MANANA.plus(1, ChronoUnit.HOURS));
        crearClase(salaB, 10, MANANA.plus(2, ChronoUnit.HOURS));
        crearClase(salaA, 10, MANANA.plus(5, ChronoUnit.DAYS));

        mvc.perform(get("/api/catalog/services")
                        .param("from", MANANA.toString())
                        .param("to", MANANA.plus(1, ChronoUnit.DAYS).toString())
                        .param("roomId", String.valueOf(salaA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].startsAt").value(MANANA.plus(1, ChronoUnit.HOURS).toString()))
                .andExpect(jsonPath("$[1].startsAt").value(MANANA.plus(3, ChronoUnit.HOURS).toString()));

        mvc.perform(get("/api/catalog/services"))
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void filtroFromPosteriorATo_responde400() throws Exception {
        mvc.perform(get("/api/catalog/services")
                        .param("from", MANANA.toString())
                        .param("to", MANANA.minus(1, ChronoUnit.HOURS).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El filtro 'from' debe ser anterior a 'to'"));
    }

    @Test
    void filtroConFechaMalEscrita_responde400() throws Exception {
        mvc.perform(get("/api/catalog/services").param("from", "ayer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parámetro 'from' no es válido"));
    }

    @Test
    void editarPlanYCupo_recalculaLosDisponiblesRespetandoLosOcupados() throws Exception {
        long sala = crearSala("Sala A", 30);
        long clase = crearClase(sala, 10);
        postJson("/internal/services/" + clase + "/take-slot", Map.of("reservationId", 1));
        postJson("/internal/services/" + clase + "/take-slot", Map.of("reservationId", 2));

        Map<String, Object> cambios = clase(sala, 20, MANANA);
        cambios.put("plan", "PREMIUM");
        mvc.perform(put("/api/catalog/services/" + clase).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cambios)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("PREMIUM"))
                .andExpect(jsonPath("$.capacity").value(20))
                .andExpect(jsonPath("$.occupiedSlots").value(2))
                .andExpect(jsonPath("$.availableSlots").value(18));
    }

    @Test
    void bajarElCupoPorDebajoDeLosOcupados_responde409() throws Exception {
        long sala = crearSala("Sala A", 30);
        long clase = crearClase(sala, 3);
        for (long reserva = 1; reserva <= 3; reserva++) {
            postJson("/internal/services/" + clase + "/take-slot", Map.of("reservationId", reserva));
        }

        mvc.perform(put("/api/catalog/services/" + clase).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clase(sala, 2, MANANA))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La clase ya tiene 3 reservas confirmadas; el cupo no puede bajar a 2"));
    }

    @Test
    void eliminar_sinReservas_responde204_yConReservasConfirmadas_409() throws Exception {
        long sala = crearSala("Sala A", 30);
        long libre = crearClase(sala, 5);
        long ocupada = crearClase(sala, 5);
        postJson("/internal/services/" + ocupada + "/take-slot", Map.of("reservationId", 1));

        mvc.perform(delete("/api/catalog/services/" + libre)).andExpect(status().isNoContent());
        mvc.perform(get("/api/catalog/services/" + libre)).andExpect(status().isNotFound());

        mvc.perform(delete("/api/catalog/services/" + ocupada))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("tiene reservas confirmadas")));
    }
}
