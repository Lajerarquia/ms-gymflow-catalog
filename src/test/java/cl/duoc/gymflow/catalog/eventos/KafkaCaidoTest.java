package cl.duoc.gymflow.catalog.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Kafka caído (nadie escucha en localhost:1): catalog arranca, crea la sala sin esperar a Kafka, el health sigue UP
 * (reservations y el BFF dependen de él) y el error queda en el log.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogo-sin-kafka;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.bootstrap-servers=localhost:1",
        "spring.kafka.producer.properties.max.block.ms=1500",
        "spring.kafka.admin.operation-timeout=2s",
        "spring.kafka.admin.close-timeout=1s"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class KafkaCaidoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private ThreadPoolTaskExecutor tareas;

    @AfterEach
    void esperarPublicacionesPendientes() {
        await().atMost(Duration.ofSeconds(10)).until(() ->
                tareas.getActiveCount() == 0 && tareas.getThreadPoolExecutor().getQueue().isEmpty());
    }

    @Test
    void conKafkaCaido_laSalaSeCreaRapidoYElErrorQuedaEnElLog(CapturedOutput salida) throws Exception {
        long inicio = System.currentTimeMillis();
        mvc.perform(post("/api/catalog/rooms").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sala Norte\",\"branch\":\"Providencia\",\"capacity\":30}"))
                .andExpect(status().isCreated());
        assertThat(System.currentTimeMillis() - inicio).isLessThan(1500);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(salida.getOut()).contains("No se pudo publicar AUDITORIA_CREAR"));
    }

    @Test
    void conKafkaCaido_elHealthSigueUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
