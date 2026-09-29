package cl.duoc.gymflow.catalog.dto;

import cl.duoc.gymflow.catalog.entity.Clase;
import cl.duoc.gymflow.catalog.entity.Plan;
import java.time.Instant;

/**
 * Clase tal como la ven el frontend y ms-gymflow-reservations.
 * {@code occupiedSlots} y {@code availableSlots} alimentan la tasa de ocupación de /reports.
 */
public record ClaseResponse(
        Long id,
        String name,
        String description,
        String instructor,
        Long roomId,
        String roomName,
        String branch,
        Instant startsAt,
        Instant endsAt,
        int durationMinutes,
        int capacity,
        int availableSlots,
        int occupiedSlots,
        Plan plan) {

    public static ClaseResponse desde(Clase clase) {
        return new ClaseResponse(
                clase.getId(),
                clase.getNombre(),
                clase.getDescripcion(),
                clase.getInstructor(),
                clase.getSala().getId(),
                clase.getSala().getNombre(),
                clase.getSala().getSede(),
                clase.getInicio(),
                clase.getInicio().plusSeconds(clase.getDuracionMinutos() * 60L),
                clase.getDuracionMinutos(),
                clase.getCapacidad(),
                clase.getCuposDisponibles(),
                clase.getCuposOcupados(),
                clase.getPlan());
    }
}
