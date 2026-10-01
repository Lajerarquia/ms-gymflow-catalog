package cl.duoc.gymflow.catalog.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.gymflow.catalog.repository.ClaseRepository;
import cl.duoc.gymflow.catalog.repository.CupoReservadoRepository;
import cl.duoc.gymflow.catalog.repository.SalaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Base de las pruebas de API: contexto completo con H2 en memoria (las propiedades se pasan aquí,
 * sin application.yml de pruebas, para no reemplazar el de main) y la BD vacía antes de cada prueba.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogo-pruebas;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
public abstract class PruebaApiBase {

    protected static final Instant MANANA = Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.DAYS);

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected SalaRepository salaRepository;

    @Autowired
    protected ClaseRepository claseRepository;

    @Autowired
    protected CupoReservadoRepository cupoRepository;

    @BeforeEach
    void limpiarBd() {
        cupoRepository.deleteAll();
        claseRepository.deleteAll();
        salaRepository.deleteAll();
    }

    protected ResultActions postJson(String ruta, Object cuerpo) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cuerpo)));
    }

    protected long crearSala(String nombre, int capacidad) throws Exception {
        String json = postJson("/api/catalog/rooms",
                Map.of("name", nombre, "branch", "Providencia", "capacity", capacidad))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    protected long crearClase(long salaId, int cupo) throws Exception {
        return crearClase(salaId, cupo, MANANA);
    }

    protected long crearClase(long salaId, int cupo, Instant inicio) throws Exception {
        String json = postJson("/api/catalog/services", clase(salaId, cupo, inicio))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    /** Cuerpo válido de POST/PUT /api/catalog/services, tal como lo enviará el frontend a través del BFF. */
    protected static Map<String, Object> clase(long salaId, int cupo, Instant inicio) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("name", "Spinning 45");
        cuerpo.put("description", "Cardio en bicicleta");
        cuerpo.put("instructor", "Camila Rojas");
        cuerpo.put("roomId", salaId);
        cuerpo.put("startsAt", inicio.toString());
        cuerpo.put("durationMinutes", 45);
        cuerpo.put("capacity", cupo);
        cuerpo.put("plan", "BASICO");
        return cuerpo;
    }

    protected JsonNode leer(ResultActions resultado) throws Exception {
        return objectMapper.readTree(resultado.andReturn().getResponse().getContentAsString());
    }
}
