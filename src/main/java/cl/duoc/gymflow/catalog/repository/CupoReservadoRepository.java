package cl.duoc.gymflow.catalog.repository;

import cl.duoc.gymflow.catalog.entity.CupoReservado;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CupoReservadoRepository extends JpaRepository<CupoReservado, Long> {

    Optional<CupoReservado> findByReservaId(Long reservaId);

    boolean existsByClaseId(Long claseId);

    long countByClaseId(Long claseId);
}
