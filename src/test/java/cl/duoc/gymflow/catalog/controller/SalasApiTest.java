package cl.duoc.gymflow.catalog.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.gymflow.catalog.support.PruebaApiBase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SalasApiTest extends PruebaApiBase {

    @Test
    void crearYListar_conLosCamposDelContrato() throws Exception {
        postJson("/api/catalog/rooms", Map.of("name", "Box Funcional", "branch", "Maipú", "capacity", 15))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Box Funcional"))
                .andExpect(jsonPath("$.branch").value("Maipú"))
                .andExpect(jsonPath("$.capacity").value(15));

        mvc.perform(get("/api/catalog/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void capacidadFueraDeRango_responde400() throws Exception {
        postJson("/api/catalog/rooms", Map.of("name", "Sala", "branch", "Centro", "capacity", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("capacity: La capacidad debe ser al menos 1"));
    }

    @Test
    void salaInexistente_responde404() throws Exception {
        mvc.perform(get("/api/catalog/rooms/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La sala 999 no existe"));
    }

    @Test
    void bajarCapacidadPorDebajoDeUnaClase_responde409() throws Exception {
        long sala = crearSala("Sala A", 30);
        crearClase(sala, 25);

        mvc.perform(put("/api/catalog/rooms/" + sala).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Sala A", "branch", "Providencia", "capacity", 20))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La sala tiene una clase con cupo 25; su capacidad no puede bajar a 20"));
    }

    @Test
    void eliminarSalaConClases_responde409_ySinClases_204() throws Exception {
        long conClases = crearSala("Sala A", 30);
        long vacia = crearSala("Sala B", 30);
        crearClase(conClases, 10);

        mvc.perform(delete("/api/catalog/rooms/" + conClases))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("porque tiene clases")));
        mvc.perform(delete("/api/catalog/rooms/" + vacia))
                .andExpect(status().isNoContent());
    }
}
