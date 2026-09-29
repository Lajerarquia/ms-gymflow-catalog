package cl.duoc.gymflow.catalog.controller;

/**
 * Rutas internas que llama ms-gymflow-reservations. <b>Son un contrato</b>: si cambian aquí, hay que
 * cambiarlas también en el cliente de catálogo de reservations (y en CLAUDE.md).
 * <p>
 * El BFF no las expone: solo reenvía rutas {@code /api/catalog/...} declaradas explícitamente.
 * <pre>
 * POST /internal/services/{id}/take-slot     body {"reservationId": 42}
 * POST /internal/services/{id}/release-slot  body {"reservationId": 42}
 * → 200 {"serviceId": 1, "reservationId": 42, "availableSlots": 11, "changed": true}
 * → 404 la clase no existe · 409 sin cupos (solo take-slot) · 400 falta reservationId
 * </pre>
 */
public final class RutasInternas {

    public static final String BASE = "/internal/services";
    public static final String TOMAR_CUPO = "/{id}/take-slot";
    public static final String DEVOLVER_CUPO = "/{id}/release-slot";

    private RutasInternas() {
    }
}
