package cl.duoc.gymflow.catalog.repository;

import cl.duoc.gymflow.catalog.entity.Clase;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros opcionales del listado de clases. Cada filtro que viene en null simplemente no se aplica.
 */
public final class ClaseFiltros {

    private ClaseFiltros() {
    }

    public static Specification<Clase> con(Instant desde, Instant hasta, Long salaId) {
        Specification<Clase> filtros = (clase, consulta, cb) -> cb.conjunction();
        if (desde != null) {
            filtros = filtros.and((clase, consulta, cb) -> cb.greaterThanOrEqualTo(clase.get("inicio"), desde));
        }
        if (hasta != null) {
            filtros = filtros.and((clase, consulta, cb) -> cb.lessThan(clase.get("inicio"), hasta));
        }
        if (salaId != null) {
            filtros = filtros.and((clase, consulta, cb) -> cb.equal(clase.get("sala").get("id"), salaId));
        }
        return filtros;
    }
}
