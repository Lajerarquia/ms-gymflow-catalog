package cl.duoc.gymflow.catalog.service;

import cl.duoc.gymflow.catalog.dto.SalaRequest;
import cl.duoc.gymflow.catalog.dto.SalaResponse;
import cl.duoc.gymflow.catalog.entity.Sala;
import cl.duoc.gymflow.catalog.error.ConflictoException;
import cl.duoc.gymflow.catalog.error.RecursoNoEncontradoException;
import cl.duoc.gymflow.catalog.repository.ClaseRepository;
import cl.duoc.gymflow.catalog.repository.SalaRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalaService {

    private final SalaRepository salaRepository;
    private final ClaseRepository claseRepository;

    public SalaService(SalaRepository salaRepository, ClaseRepository claseRepository) {
        this.salaRepository = salaRepository;
        this.claseRepository = claseRepository;
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
        sala.actualizar(datos.name().trim(), datos.branch().trim(), datos.capacity());
        return SalaResponse.desde(sala);
    }

    @Transactional
    public void eliminar(Long id) {
        Sala sala = buscar(id);
        if (claseRepository.existsBySalaId(id)) {
            throw new ConflictoException("No se puede eliminar la sala '" + sala.getNombre() + "' porque tiene clases");
        }
        salaRepository.delete(sala);
    }

    Sala buscar(Long id) {
        return salaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La sala " + id + " no existe"));
    }
}
