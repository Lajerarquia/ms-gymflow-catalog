package cl.duoc.gymflow.catalog.service;

import cl.duoc.gymflow.catalog.dto.SalaRequest;
import cl.duoc.gymflow.catalog.dto.SalaResponse;
import cl.duoc.gymflow.catalog.entity.Sala;
import cl.duoc.gymflow.catalog.error.ConflictoException;
import cl.duoc.gymflow.catalog.error.RecursoNoEncontradoException;
import cl.duoc.gymflow.catalog.eventos.Cambios;
import cl.duoc.gymflow.catalog.eventos.CatalogoModificado;
import cl.duoc.gymflow.catalog.eventos.EventoAuditoriaDto.Accion;
import cl.duoc.gymflow.catalog.eventos.EventoAuditoriaDto.TipoEntidad;
import cl.duoc.gymflow.catalog.repository.ClaseRepository;
import cl.duoc.gymflow.catalog.repository.SalaRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Salas del catálogo. EP5: crear, editar y eliminar publican {@link CatalogoModificado} dentro de la transacción;
 * se audita en Kafka ({@code audit.timeline}) solo si la transacción se confirma.
 */
@Service
public class SalaService {

    private final SalaRepository salaRepository;
    private final ClaseRepository claseRepository;
    private final ApplicationEventPublisher eventos;

    public SalaService(SalaRepository salaRepository, ClaseRepository claseRepository,
                       ApplicationEventPublisher eventos) {
        this.salaRepository = salaRepository;
        this.claseRepository = claseRepository;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<SalaResponse> listar() {
        return salaRepository.findAll(Sort.by("sede", "nombre")).stream().map(SalaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public SalaResponse obtener(Long id) {
        return SalaResponse.desde(buscar(id));
    }

    @Transactional
    public SalaResponse crear(SalaRequest datos) {
        Sala sala = salaRepository.save(new Sala(datos.name().trim(), datos.branch().trim(), datos.capacity()));
        eventos.publishEvent(CatalogoModificado.de(Accion.CREAR, TipoEntidad.SALA, sala.getId(),
                "Sala '" + sala.getNombre() + "' creada en " + sala.getSede() + " con capacidad " + sala.getCapacidad()));
        return SalaResponse.desde(sala);
    }

    @Transactional
    public SalaResponse actualizar(Long id, SalaRequest datos) {
        Sala sala = buscar(id);
        int claseMasGrande = claseRepository.capacidadMaximaEnSala(id);
        if (datos.capacity() < claseMasGrande) {
            throw new ConflictoException("La sala tiene una clase con cupo " + claseMasGrande
                    + "; su capacidad no puede bajar a " + datos.capacity());
        }
        Cambios cambios = new Cambios()
                .campo("nombre", sala.getNombre(), datos.name().trim())
                .campo("sede", sala.getSede(), datos.branch().trim())
                .campo("capacidad", sala.getCapacidad(), datos.capacity());
        sala.actualizar(datos.name().trim(), datos.branch().trim(), datos.capacity());
        eventos.publishEvent(CatalogoModificado.de(Accion.EDITAR, TipoEntidad.SALA, id,
                "Sala '" + sala.getNombre() + "' editada: " + cambios.texto()));
        return SalaResponse.desde(sala);
    }

    @Transactional
    public void eliminar(Long id) {
        Sala sala = buscar(id);
        if (claseRepository.existsBySalaId(id)) {
            throw new ConflictoException("No se puede eliminar la sala '" + sala.getNombre() + "' porque tiene clases");
        }
        salaRepository.delete(sala);
        eventos.publishEvent(CatalogoModificado.de(Accion.ELIMINAR, TipoEntidad.SALA, id,
                "Sala '" + sala.getNombre() + "' (" + sala.getSede() + ") eliminada"));
    }

    Sala buscar(Long id) {
        return salaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La sala " + id + " no existe"));
    }
}
