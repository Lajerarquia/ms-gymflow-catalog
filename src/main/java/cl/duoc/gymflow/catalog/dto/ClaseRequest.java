package cl.duoc.gymflow.catalog.dto;

import cl.duoc.gymflow.catalog.entity.Plan;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Cuerpo de POST y PUT /api/catalog/services. Con PUT el Admin edita, entre otras cosas, el plan y el cupo
 * ({@code capacity}); los cupos disponibles se recalculan solos, no se envían.
 */
public record ClaseRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String name,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String description,

        @NotBlank(message = "El instructor es obligatorio")
        @Size(max = 100, message = "El instructor no puede superar 100 caracteres")
        String instructor,

        @NotNull(message = "La sala es obligatoria")
        Long roomId,

        @NotNull(message = "La fecha y hora de inicio es obligatoria")
        Instant startsAt,

        @NotNull(message = "La duración es obligatoria")
        @Min(value = 15, message = "La duración mínima es 15 minutos")
        @Max(value = 240, message = "La duración máxima es 240 minutos")
        Integer durationMinutes,

        @NotNull(message = "El cupo es obligatorio")
        @Min(value = 1, message = "El cupo debe ser al menos 1")
        Integer capacity,

        @NotNull(message = "El plan es obligatorio")
        Plan plan) {
}
