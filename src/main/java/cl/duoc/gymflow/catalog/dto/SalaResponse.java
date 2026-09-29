package cl.duoc.gymflow.catalog.dto;

import cl.duoc.gymflow.catalog.entity.Sala;

/** Sala tal como la ve el frontend. */
public record SalaResponse(Long id, String name, String branch, int capacity) {

    public static SalaResponse desde(Sala sala) {
        return new SalaResponse(sala.getId(), sala.getNombre(), sala.getSede(), sala.getCapacidad());
    }
}
