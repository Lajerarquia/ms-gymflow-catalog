package cl.duoc.gymflow.catalog.service;

import cl.duoc.gymflow.catalog.dto.ClaseRequest;
import cl.duoc.gymflow.catalog.dto.ClaseResponse;
import cl.duoc.gymflow.catalog.entity.Clase;
import cl.duoc.gymflow.catalog.entity.Sala;
import cl.duoc.gymflow.catalog.error.ConflictoException;
import cl.duoc.gymflow.catalog.error.RecursoNoEncontradoException;
import cl.duoc.gymflow.catalog.error.SolicitudInvalidaException;
import cl.duoc.gymflow.catalog.repository.ClaseFiltros;
import cl.duoc.gymflow.catalog.repository.ClaseRepository;
import cl.duoc.gymflow.catalog.repository.CupoReservadoRepository;
import cl.duoc.gymflow.catalog.repository.SalaRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaseService {

    private final ClaseRepository claseRepository;
    private final SalaRepository salaRepository;
    private final CupoReservadoRepository cupoRepository;

    public ClaseService(ClaseRepository claseRepository, SalaRepository salaRepository,
                        CupoReservadoRepository cupoRepository) {
        this.claseRepository = claseRepository;
        this.salaRepository = salaRepository;
        this.cupoRepository = cupoRepository;
    }

    @Transactional(readOnly = true)
    public List<ClaseResponse> listar(Instant desde, Instant hasta, Long salaId) {
        if (desde != null && hasta != null && !desde.isBefore(hasta)) {
            throw new SolicitudInvalidaException("El filtro 'from' debe ser anterior a 'to'");
        }
        return claseRepository.findAll(ClaseFiltros.con(desde, hasta, salaId), Sort.by("inicio"))
                .stream().map(ClaseResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public ClaseResponse obtener(Long id) {
        return ClaseResponse.desde(claseRepository.findById(id).orElseThrow(() -> noExiste(id)));
    }

    @Transactional
    public ClaseResponse crear(ClaseRequest datos) {
        Sala sala = salaValida(datos);
        Clase clase = new Clase(datos.name().trim(), datos.description(), datos.instructor().trim(), sala,
                datos.startsAt(), datos.durationMinutes(), datos.capacity(), datos.plan());
        return ClaseResponse.desde(claseRepository.save(clase));
    }

    /**
     * Edita la clase, incluidos el plan y el cupo. Se bloquea la fila igual que al tomar un cupo, para que
     * una confirmación simultánea no se pierda al recalcular los cupos disponibles.
     */
    @Transactional
    public ClaseResponse actualizar(Long id, ClaseRequest datos) {
        Clase clase = claseRepository.buscarParaActualizar(id).orElseThrow(() -> noExiste(id));
        Sala sala = salaValida(datos);
        if (datos.capacity() < clase.getCuposOcupados()) {
            throw new ConflictoException("La clase ya tiene " + clase.getCuposOcupados()
                    + " reservas confirmadas; el cupo no puede bajar a " + datos.capacity());
        }
        clase.actualizar(datos.name().trim(), datos.description(), datos.instructor().trim(), sala,
                datos.startsAt(), datos.durationMinutes(), datos.capacity(), datos.plan());
        return ClaseResponse.desde(clase);
    }

    @Transactional
    public void eliminar(Long id) {
        Clase clase = claseRepository.buscarParaActualizar(id).orElseThrow(() -> noExiste(id));
        if (cupoRepository.existsByClaseId(id)) {
            throw new ConflictoException("No se puede eliminar la clase '" + clase.getNombre()
                    + "' porque tiene reservas confirmadas");
        }
        claseRepository.delete(clase);
    }

    private Sala salaValida(ClaseRequest datos) {
        Sala sala = salaRepository.findById(datos.roomId())
                .orElseThrow(() -> new SolicitudInvalidaException("La sala " + datos.roomId() + " no existe"));
        if (datos.capacity() > sala.getCapacidad()) {
            throw new SolicitudInvalidaException("El cupo (" + datos.capacity() + ") supera la capacidad de la sala '"
                    + sala.getNombre() + "' (" + sala.getCapacidad() + ")");
        }
        return sala;
    }

    private static RecursoNoEncontradoException noExiste(Long id) {
        return new RecursoNoEncontradoException("La clase " + id + " no existe");
    }
}
