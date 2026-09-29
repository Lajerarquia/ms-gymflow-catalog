package cl.duoc.gymflow.catalog.dto;

/**
 * Resultado de tomar o devolver un cupo.
 *
 * @param changed false si la operación ya se había hecho antes para esa reserva (reintento idempotente)
 */
public record CupoResponse(Long serviceId, Long reservationId, int availableSlots, boolean changed) {
}
