package cl.duoc.gymflow.catalog.repository;

import cl.duoc.gymflow.catalog.entity.Clase;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaseRepository extends JpaRepository<Clase, Long>, JpaSpecificationExecutor<Clase> {

    /** Trae la sala en la misma consulta, para no hacer una consulta extra por cada clase del listado. */
    @Override
    @EntityGraph(attributePaths = "sala")
    List<Clase> findAll(Specification<Clase> filtros, Sort orden);

    /**
     * Lee la clase bloqueando su fila ({@code SELECT ... FOR UPDATE}) hasta el fin de la transacción.
     * Así dos confirmaciones simultáneas sobre el último cupo se atienden una después de la otra
     * y la segunda ve que ya no quedan cupos: no hay sobreventa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Clase c where c.id = :id")
    Optional<Clase> buscarParaActualizar(@Param("id") Long id);

    boolean existsBySalaId(Long salaId);

    @Query("select coalesce(max(c.capacidad), 0) from Clase c where c.sala.id = :salaId")
    int capacidadMaximaEnSala(@Param("salaId") Long salaId);
}
