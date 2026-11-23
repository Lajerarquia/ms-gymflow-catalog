package cl.duoc.gymflow.catalog.eventos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Envía un DTO a un tópico de Kafka sin bloquear ni hacer fallar la operación de negocio.
 * <ul>
 *   <li><b>Valida el DTO</b> (Bean Validation) antes de enviarlo: un evento incompleto no sale del servicio.</li>
 *   <li>Agrega las cabeceras {@code traceId} y {@code eventType}: se pueden ver y filtrar en Kafka UI sin abrir el JSON.</li>
 *   <li>Envía <b>en segundo plano</b> (executor de tareas de Spring Boot). Si Kafka está caído, el productor espera
 *       como máximo {@code max.block.ms} a tener metadatos; esa espera no la sufre la respuesta HTTP.</li>
 *   <li>El callback de {@code send} registra partición y offset (éxito) o el error. Los reintentos ante errores
 *       transitorios los hace el propio productor ({@code retries} + {@code delivery.timeout.ms}).</li>
 * </ul>
 * Si aun así falla, el evento se pierde y queda en el log: el cambio en el catálogo ya está guardado. La garantía completa
 * requiere el patrón outbox (documentado en el README).
 */
@Component
public class EnviadorKafka {

    private static final Logger log = LoggerFactory.getLogger(EnviadorKafka.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Executor executor;
    private final Validator validator;

    public EnviadorKafka(KafkaTemplate<String, Object> kafkaTemplate,
                         @Qualifier("applicationTaskExecutor") Executor executor, Validator validator) {
        this.kafkaTemplate = kafkaTemplate;
        this.executor = executor;
        this.validator = validator;
    }

    /** Nunca lanza excepciones. */
    public void enviar(String topico, String clave, Object evento, String traceId, String tipo) {
        Set<ConstraintViolation<Object>> errores = validator.validate(evento);
        if (!errores.isEmpty()) {
            log.error("Evento {} inválido, no se publica en {} (clave={}): {}", tipo, topico, clave,
                    errores.stream().map(e -> e.getPropertyPath() + " " + e.getMessage()).collect(Collectors.joining(", ")));
            return;
        }
        ProducerRecord<String, Object> registro = new ProducerRecord<>(topico, clave, evento);
        registro.headers().add(KafkaConfig.CABECERA_TRACE_ID, traceId.getBytes(StandardCharsets.UTF_8));
        registro.headers().add(KafkaConfig.CABECERA_TIPO, tipo.getBytes(StandardCharsets.UTF_8));
        try {
            executor.execute(() -> publicar(registro, tipo, traceId));
        } catch (RuntimeException rechazado) {
            log.error("No se pudo programar el envío de {} a {} (clave={})", tipo, topico, clave, rechazado);
        }
    }

    private void publicar(ProducerRecord<String, Object> registro, String tipo, String traceId) {
        try {
            kafkaTemplate.send(registro).whenComplete((resultado, error) -> {
                if (error == null) {
                    var meta = resultado.getRecordMetadata();
                    log.info("Publicado {} en {} partición={} offset={} clave={} traceId={}", tipo, meta.topic(),
                            meta.partition(), meta.offset(), registro.key(), traceId);
                } else {
                    log.error("No se pudo publicar {} en {} (clave={}, traceId={}): {}. La operación ya quedó guardada.",
                            tipo, registro.topic(), registro.key(), traceId, causa(error));
                }
            });
        } catch (RuntimeException fallo) {
            // send() puede lanzar de inmediato (p. ej. sin metadatos tras max.block.ms o productor cerrado).
            log.error("No se pudo publicar {} en {} (clave={}, traceId={}): {}. La operación ya quedó guardada.",
                    tipo, registro.topic(), registro.key(), traceId, causa(fallo));
        }
    }

    /** spring-kafka envuelve el error ("Send failed"): se muestra la causa real (p. ej. el timeout de metadatos). */
    private static String causa(Throwable error) {
        Throwable raiz = NestedExceptionUtils.getMostSpecificCause(error);
        return raiz.getClass().getSimpleName() + ": " + raiz.getMessage();
    }
}
