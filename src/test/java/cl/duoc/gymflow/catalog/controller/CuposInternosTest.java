package cl.duoc.gymflow.catalog.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.gymflow.catalog.support.PruebaApiBase;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contrato de las rutas internas que llama ms-gymflow-reservations. Las rutas están escritas a mano
 * (no con las constantes) a propósito: si alguien cambia {@link RutasInternas}, esta prueba falla y avisa
 * que también hay que cambiar el cliente de reservations.
 */
class CuposInternosTest extends PruebaApiBase {

    @Test
    void tomarCupo_descuentaUno() throws Exception {
        long clase = crearClase(crearSala("Sala A", 10), 3);

        postJson("/internal/services/" + clase + "/take-slot", Map.of("reservationId", 100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").value(clase))
                .andExpect(jsonPath("$.reservationId").value(100))
                .andExpect(jsonPath("$.availableSlots").value(2))
                .andExpect(jsonPath("$.changed").value(true));

        mvc.perform(get("/api/catalog/services/" + clase))
                .andExpect(jsonPath("$.availableSlots").value(2))
                .andExpect(jsonPath("$.occupiedSlots").value(1));
    }

    @Test
    void tomarCupoDosVecesParaLaMismaReserva_noDescuentaDosVeces() throws Exception {
        long clase = crearClase(crearSala("Sala A", 10), 3);
        postJson("/internal/services/" + clase + "/take-slot", Map.of("reservationId", 100));

        postJson("/internal/services/" + clase + "/take-slot", Map.of("reservationId", 100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSlots").value(2))
                .andExpect(jsonPath("$.changed").value(false));
    }

    @Test
    void sinCupos_responde409() throws Exception {
        long clase = crearClase(crearSala("Sala A", 10), 1);
        postJson("/internal/services/" + clase + "/take-slot", Map.of("reservationId", 100));

        postJson("/internal/services/" + clase + "/take-slot", Map.of("reservationId", 101))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("La clase 'Spinning 45' no tiene cupos disponibles"));
    }

    @Test
    void reservaQueYaOcupaCupoEnOtraClase_responde409() throws Exception {
        long sala = crearSala("Sala A", 10);
        long claseA = crearClase(sala, 3);
        long claseB = crearClase(sala, 3);
        postJson("/internal/services/" + claseA + "/take-slot", Map.of("reservationId", 100));

        postJson("/internal/services/" + claseB + "/take-slot", Map.of("reservationId", 100))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("ya ocupa un cupo en otra clase")));
    }

    @Test
    void devolverCupo_loSuma_yEsIdempotente() throws Exception {
        long clase = crearClase(crearSala("Sala A", 10), 3);
        postJson("/internal/services/" + clase + "/take-slot", Map.of("reservationId", 100));

        postJson("/internal/services/" + clase + "/release-slot", Map.of("reservationId", 100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSlots").value(3))
                .andExpect(jsonPath("$.changed").value(true));

        postJson("/internal/services/" + clase + "/release-slot", Map.of("reservationId", 100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSlots").value(3))
                .andExpect(jsonPath("$.changed").value(false));
    }

    @Test
    void devolverCupoDeUnaReservaQueNuncaLoTomo_noCambiaNada() throws Exception {
        long clase = crearClase(crearSala("Sala A", 10), 3);

        postJson("/internal/services/" + clase + "/release-slot", Map.of("reservationId", 555))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSlots").value(3))
                .andExpect(jsonPath("$.changed").value(false));
    }

    @Test
    void claseInexistente_responde404() throws Exception {
        postJson("/internal/services/999/take-slot", Map.of("reservationId", 100))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La clase 999 no existe"));
    }

    @Test
    void sinReservationId_responde400() throws Exception {
        long clase = crearClase(crearSala("Sala A", 10), 3);

        postJson("/internal/services/" + clase + "/take-slot", Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("reservationId: El id de la reserva es obligatorio"));
    }
}
