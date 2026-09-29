package cl.duoc.gymflow.catalog.config;

import cl.duoc.gymflow.catalog.entity.Clase;
import cl.duoc.gymflow.catalog.entity.Plan;
import cl.duoc.gymflow.catalog.entity.Sala;
import cl.duoc.gymflow.catalog.repository.ClaseRepository;
import cl.duoc.gymflow.catalog.repository.SalaRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de ejemplo solo para el perfil {@code local} (H2). Las fechas se calculan desde "ahora"
 * para que siempre haya clases futuras. En Oracle no se carga nada automáticamente.
 */
@Component
@Profile("local")
public class DatosLocales implements CommandLineRunner {

    private final SalaRepository salaRepository;
    private final ClaseRepository claseRepository;

    public DatosLocales(SalaRepository salaRepository, ClaseRepository claseRepository) {
        this.salaRepository = salaRepository;
        this.claseRepository = claseRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (salaRepository.count() > 0) {
            return;
        }
        Sala spinning = salaRepository.save(new Sala("Sala Spinning", "Providencia", 20));
        Sala multiuso = salaRepository.save(new Sala("Sala Multiuso", "Providencia", 30));
        Sala funcional = salaRepository.save(new Sala("Box Funcional", "Maipú", 15));

        Instant manana = Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.DAYS);
        claseRepository.save(new Clase("Spinning 45", "Cardio en bicicleta estática", "Camila Rojas",
                spinning, manana.plus(8, ChronoUnit.HOURS), 45, 20, Plan.BASICO));
        claseRepository.save(new Clase("Yoga Flow", "Movilidad y respiración", "Diego Soto",
                multiuso, manana.plus(10, ChronoUnit.HOURS), 60, 25, Plan.PLUS));
        claseRepository.save(new Clase("Funcional HIIT", "Circuito de alta intensidad", "Valentina Muñoz",
                funcional, manana.plus(19, ChronoUnit.HOURS), 50, 12, Plan.PREMIUM));
        claseRepository.save(new Clase("Pilates Mat", null, "Diego Soto",
                multiuso, manana.plus(1, ChronoUnit.DAYS).plus(9, ChronoUnit.HOURS), 60, 30, Plan.PLUS));
    }
}
