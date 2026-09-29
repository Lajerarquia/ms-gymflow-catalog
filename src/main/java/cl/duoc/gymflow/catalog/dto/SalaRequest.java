package cl.duoc.gymflow.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de POST y PUT /api/catalog/rooms. */
public record SalaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String name,

        @NotBlank(message = "La sede es obligatoria")
        @Size(max = 100, message = "La sede no puede superar 100 caracteres")
        String branch,

        @NotNull(message = "La capacidad es obligatoria")
        @Min(value = 1, message = "La capacidad debe ser al menos 1")
        @Max(value = 500, message = "La capacidad no puede superar 500")
        Integer capacity) {
}
