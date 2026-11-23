package cl.duoc.gymflow.catalog.eventos;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Productor Kafka de catalog (EP5): publica en {@code audit.timeline} las acciones de los usuarios sobre clases y salas.
 * Es el segundo productor de ese tópico (el otro es ms-gymflow-reservations); los dos usan el mismo DTO y la misma
 * clave ({@code eventId}). La confiabilidad (acks=all, idempotencia, reintentos...) está en {@code application.yml}.
 * <p>
 * Serializers: clave {@link StringSerializer}; valor {@link JsonSerializer} con el ObjectMapper de Spring Boot (fechas
 * ISO-8601) y sin cabeceras de tipo, para que ms-gymflow-audit lo lea con su propia clase.
 */
@Configuration
public class KafkaConfig {

    public static final String TOPICO_AUDITORIA = "audit.timeline";

    public static final String CABECERA_TRACE_ID = "traceId";
    public static final String CABECERA_TIPO = "eventType";

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties propiedades,
                                                           ObjectProvider<SslBundles> sslBundles,
                                                           ObjectMapper objectMapper) {
        Map<String, Object> config = propiedades.buildProducerProperties(sslBundles.getIfAvailable());
        JsonSerializer<Object> valor = new JsonSerializer<>(objectMapper);
        valor.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), valor);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Mismo tópico y mismos parámetros que declara reservations y que crea infra/kafka/scripts/crear-topicos.sh:
     * quien arranque primero lo crea y los demás lo encuentran hecho. 3 particiones, 3 réplicas,
     * min.insync.replicas=2, compact+delete con retención de 30 días y min.compaction.lag.ms de 1 hora.
     */
    @Bean
    public NewTopic topicoAuditoria() {
        return TopicBuilder.name(TOPICO_AUDITORIA)
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG,
                        TopicConfig.CLEANUP_POLICY_COMPACT + "," + TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000")
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .config(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG, "3600000")
                .build();
    }
}
