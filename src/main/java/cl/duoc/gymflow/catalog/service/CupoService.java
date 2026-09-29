package cl.duoc.gymflow.catalog.service;

import cl.duoc.gymflow.catalog.dto.CupoResponse;
import cl.duoc.gymflow.catalog.entity.Clase;
import cl.duoc.gymflow.catalog.entity.CupoReservado;
import cl.duoc.gymflow.catalog.error.ConflictoException;
import cl.duoc.gymflow.catalog.error.RecursoNoEncontradoException;
import cl.duoc.gymflow.catalog.repository.ClaseRepository;
import cl.duoc.gymflow.catalog.repository.CupoReservadoRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tomar y devolver cupos. Lo llama solo ms-gymflow-reservations:
 * <ul>
 *   <li>al CONFIRMAR una reserva → {@link #tomar};</li>
 *   <li>al CANCELAR una reserva que estaba confirmada → {@link #devolver}.</li>
 * </ul>
 * Ambas operaciones bloquean la fila de la clase (no hay sobreventa) y son idempotentes por reserva.
 */
@Service
public class CupoService {

    private final ClaseRepository claseRepository;
    private final CupoReservadoRepository cupoRepository;

    public CupoService(ClaseRepository claseRepository, CupoReservadoRepository cupoRepository) {
        this.claseRepository = claseRepository;
        this.cupoRepository = cupoRepository;
    }

    @Transactional
    public CupoResponse tomar(Long claseId, Long reservaId) {
        Clase clase = bloquear(claseId);

        Optional<CupoReservado> existente = cupoRepository.findByReservaId(reservaId);
        if (existente.isPresent()) {
            if (existente.get().getClase().getId().equals(claseId)) {
                return respuesta(clase, reservaId, false);
            }
            throw new ConflictoException("La reserva " + reservaId + " ya ocupa un cupo en otra clase");
        }

        if (clase.getCuposDisponibles() <= 0) {
            throw new ConflictoException("La clase '" + clase.getNombre() + "' no tiene cupos disponibles");
        }
        clase.tomarCupo();
        cupoRepository.save(new CupoReservado(clase, reservaId));
        return respuesta(clase, reservaId, true);
    }

    @Transactional
    public CupoResponse devolver(Long claseId, Long reservaId) {
        Clase clase = bloquear(claseId);

        Optional<CupoReservado> cupo = cupoRepository.findByReservaId(reservaId)
                .filter(c -> c.getClase().getId().equals(claseId));
        if (cupo.isEmpty()) {
            return respuesta(clase, reservaId, false);
        }
        cupoRepository.delete(cupo.get());
        clase.devolverCupo();
        return respuesta(clase, reservaId, true);
    }

    private Clase bloquear(Long claseId) {
        return claseRepository.buscarParaActualizar(claseId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La clase " + claseId + " no existe"));
    }

    private static CupoResponse respuesta(Clase clase, Long reservaId, boolean cambio) {
        return new CupoResponse(clase.getId(), reservaId, clase.getCuposDisponibles(), cambio);
    }
}
