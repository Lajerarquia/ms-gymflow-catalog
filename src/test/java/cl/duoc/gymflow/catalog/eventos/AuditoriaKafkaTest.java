package cl.duoc.gymflow.catalog.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.gymflow.catalog.eventos.EventoAuditoriaDto.Accion;
import cl.duoc.gymflow.catalog.eventos.EventoAuditoriaDto.TipoEntidad;
import cl.duoc.gymflow.catalog.support.PruebaApiBase;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Productor de auditoría de catalog (EP5) contra 3 brokers embebidos: qué se publica en {@code audit.timeline} al crear,
 * editar y eliminar clases y salas, con quién, desde dónde y con qué traza; y que nada sale si la operación se rechaza.
 * Los mensajes se leen con un consumidor real que deserializa el JSON al DTO, igual que ms-gymflow-audit.
 */
class AuditoriaKafkaTest extends PruebaApiBase {

    private static final Duration ESPERA = Duration.ofSeconds(15);

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Test
    void crearSala_publicaCrearConActorTrazaEIp() throws Exception {
        String json = mvc.perform(comoAdmin(post("/api/catalog/rooms"), "traza-sala-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Sala Norte", "branch", "Providencia", "capacity", 30))))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Trace-Id", "traza-sala-1"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(json).get("id").asText();

        ConsumerRecord<String, EventoAuditoriaDto> registro =
                leer(r -> r.value().entityType() == TipoEntidad.SALA && r.value().entityId().equals(id), 1).get(0);
        EventoAuditoriaDto evento = registro.value();

        assertThat(registro.key()).isEqualTo(evento.eventId());
        assertThat(cabecera(registro, "traceId")).isEqualTo("traza-sala-1");
        assertThat(cabecera(registro, "eventType")).isEqualTo("AUDITORIA_CREAR");
        assertThat(UUID.fromString(evento.eventId())).isNotNull();
        assertThat(evento.action()).isEqualTo(Accion.CREAR);
        assertThat(evento.actorId()).isEqualTo("oid-admin");
        assertThat(evento.actorName()).isEqualTo("Benjamín Palma");
        assertThat(evento.detail()).isEqualTo("Sala 'Sala Norte' creada en Providencia con capacidad 30");
        assertThat(evento.sourceService()).isEqualTo("ms-gymflow-catalog");
        assertThat(evento.sourceIp()).isEqualTo("200.10.20.30");
        assertThat(evento.traceId()).isEqualTo("traza-sala-1");
        assertThat(evento.occurredAt()).isNotNull();
    }

    @Test
    void editarClase_publicaEditarConLosCamposQueCambiaron() throws Exception {
        long sala = crearSala("Sala Sur", 40);
        long clase = crearClase(sala, 10);
        Map<String, Object> cuerpo = clase(sala, 15, MANANA);
        cuerpo.put("plan", "PLUS");

        mvc.perform(comoAdmin(put("/api/catalog/services/" + clase), "traza-editar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());

        EventoAuditoriaDto edicion = leer(r -> r.value().entityType() == TipoEntidad.CLASE
                && r.value().entityId().equals(String.valueOf(clase)) && r.value().action() == Accion.EDITAR, 1)
                .get(0).value();
        assertThat(edicion.detail()).isEqualTo("Clase 'Spinning 45' editada: cupo 10 → 15; plan BASICO → PLUS");
        assertThat(edicion.actorName()).isEqualTo("Benjamín Palma");
        assertThat(edicion.traceId()).isEqualTo("traza-editar");
    }

    @Test
    void eliminarClase_publicaEliminar_ySinIdentidadQuedaComoDesconocido() throws Exception {
        long sala = crearSala("Sala Este", 20);
        long clase = crearClase(sala, 10);

        // Sin las cabeceras del BFF (las pruebas existentes llaman así): se audita igual, marcado como desconocido.
        mvc.perform(delete("/api/catalog/services/" + clase)).andExpect(status().isNoContent());

        List<ConsumerRecord<String, EventoAuditoriaDto>> registros = leer(r -> r.value().entityType() == TipoEntidad.CLASE
                && r.value().entityId().equals(String.valueOf(clase)), 2);
        assertThat(registros).extracting(r -> r.value().action()).containsExactlyInAnyOrder(Accion.CREAR, Accion.ELIMINAR);
        EventoAuditoriaDto eliminacion = registros.stream().map(ConsumerRecord::value)
                .filter(e -> e.action() == Accion.ELIMINAR).findFirst().orElseThrow();
        assertThat(eliminacion.detail()).startsWith("Clase 'Spinning 45' eliminada (inicio ");
        assertThat(eliminacion.actorId()).isEqualTo("desconocido");
        assertThat(eliminacion.actorName()).isEqualTo("Usuario desconocido");
        assertThat(eliminacion.sourceIp()).isNull();
    }

    @Test
    void operacionRechazada_noSeAudita() throws Exception {
        long sala = crearSala("Sala Oeste", 20);
        crearClase(sala, 10);

        // Sala con clases → 409: la transacción hace rollback y no debe salir un ELIMINAR.
        mvc.perform(comoAdmin(delete("/api/catalog/rooms/" + sala), "traza-409")).andExpect(status().isConflict());

        List<ConsumerRecord<String, EventoAuditoriaDto>> registros = leer(r -> r.value().entityType() == TipoEntidad.SALA
                && r.value().entityId().equals(String.valueOf(sala)), 2, Duration.ofSeconds(4));
        assertThat(registros).extracting(r -> r.value().action()).containsExactly(Accion.CREAR);
    }

    @Test
    void tomarCupo_noSeAuditaEnCatalog() throws Exception {
        long sala = crearSala("Sala Centro", 20);
        long clase = crearClase(sala, 10);

        // Las rutas internas de cupo las llama reservations al confirmar; esa acción la audita reservations.
        mvc.perform(post("/internal/services/" + clase + "/take-slot").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reservationId\": 1}")).andExpect(status().isOk());

        List<ConsumerRecord<String, EventoAuditoriaDto>> registros = leer(r -> r.value().entityType() == TipoEntidad.CLASE
                && r.value().entityId().equals(String.valueOf(clase)), 2, Duration.ofSeconds(4));
        assertThat(registros).extracting(r -> r.value().action()).containsExactly(Accion.CREAR);
    }

    @Test
    void elTopicoSeCreaConSusConfiguraciones() throws Exception {
        try (AdminClient admin = AdminClient.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                broker.getBrokersAsString()))) {
            TopicDescription topico = admin.describeTopics(List.of(KafkaConfig.TOPICO_AUDITORIA)).allTopicNames().get()
                    .get(KafkaConfig.TOPICO_AUDITORIA);
            assertThat(topico.partitions()).hasSize(3);
            assertThat(topico.partitions()).allSatisfy(p -> assertThat(p.replicas()).hasSize(3));
            ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, KafkaConfig.TOPICO_AUDITORIA);
            Config config = admin.describeConfigs(List.of(recurso)).all().get().get(recurso);
            assertThat(config.get("cleanup.policy").value()).isEqualTo("compact,delete");
            assertThat(config.get("retention.ms").value()).isEqualTo("2592000000");
            assertThat(config.get("min.insync.replicas").value()).isEqualTo("2");
            assertThat(config.get("min.compaction.lag.ms").value()).isEqualTo("3600000");
        }
    }

    // ---- Utilidades ----

    /** Cabeceras como las envía el BFF: identidad (nombre URL-encoded), traza e IP del cliente. */
    private static MockHttpServletRequestBuilder comoAdmin(MockHttpServletRequestBuilder peticion, String traza) {
        return peticion.header("X-User-Id", "oid-admin")
                .header("X-User-Name", URLEncoder.encode("Benjamín Palma", StandardCharsets.UTF_8))
                .header("X-Trace-Id", traza)
                .header("X-Client-Ip", "200.10.20.30");
    }

    private List<ConsumerRecord<String, EventoAuditoriaDto>> leer(Predicate<ConsumerRecord<String, EventoAuditoriaDto>> filtro,
                                                                  int esperados) {
        return leer(filtro, esperados, ESPERA);
    }

    /** Lee audit.timeline desde el principio con un grupo nuevo; termina al juntar {@code esperados} o al vencer la espera. */
    private List<ConsumerRecord<String, EventoAuditoriaDto>> leer(Predicate<ConsumerRecord<String, EventoAuditoriaDto>> filtro,
                                                                  int esperados, Duration espera) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "prueba-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        JsonDeserializer<EventoAuditoriaDto> valor = new JsonDeserializer<>(EventoAuditoriaDto.class, objectMapper, false);
        List<ConsumerRecord<String, EventoAuditoriaDto>> encontrados = new ArrayList<>();
        try (Consumer<String, EventoAuditoriaDto> consumidor =
                     new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valor).createConsumer()) {
            consumidor.subscribe(List.of(KafkaConfig.TOPICO_AUDITORIA));
            long limite = System.currentTimeMillis() + espera.toMillis();
            while (encontrados.size() < esperados && System.currentTimeMillis() < limite) {
                for (ConsumerRecord<String, EventoAuditoriaDto> registro : consumidor.poll(Duration.ofMillis(300))) {
                    if (filtro.test(registro)) {
                        encontrados.add(registro);
                    }
                }
            }
        }
        return encontrados;
    }

    private static String cabecera(ConsumerRecord<?, ?> registro, String nombre) {
        Header cabecera = registro.headers().lastHeader(nombre);
        return cabecera == null ? null : new String(cabecera.value(), StandardCharsets.UTF_8);
    }
}
