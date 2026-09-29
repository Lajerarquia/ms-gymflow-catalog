package cl.duoc.gymflow.catalog.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de las rutas internas de cupo. El id de la reserva permite que tomar y devolver sean idempotentes.
 */
public record CupoRequest(
        @NotNull(message = "El id de la reserva es obligatorio")
        Long reservationId) {
}
