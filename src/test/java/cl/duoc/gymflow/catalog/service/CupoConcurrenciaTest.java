package cl.duoc.gymflow.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.gymflow.catalog.error.ConflictoException;
import cl.duoc.gymflow.catalog.support.PruebaApiBase;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * El caso que motivó el sistema: "se sobrevenden clases". Diez confirmaciones llegan a la vez
 * por una clase con 3 cupos; el bloqueo de fila hace que exactamente 3 lo consigan.
 */
class CupoConcurrenciaTest extends PruebaApiBase {

    @Autowired
    private CupoService cupoService;

    @Test
    void diezConfirmacionesSimultaneas_sobreTresCupos_soloTresLoConsiguen() throws Exception {
        long claseId = crearClase(crearSala("Sala A", 10), 3);
        int intentos = 10;
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(intentos);
        List<Future<Boolean>> resultados = new ArrayList<>();

        for (long reserva = 1; reserva <= intentos; reserva++) {
            long reservaId = reserva;
            resultados.add(hilos.submit(() -> {
                largada.await();
                try {
                    cupoService.tomar(claseId, reservaId);
                    return true;
                } catch (ConflictoException sinCupo) {
                    return false;
                }
            }));
        }
        largada.countDown();
        hilos.shutdown();
        assertThat(hilos.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        long exitosos = 0;
        for (Future<Boolean> resultado : resultados) {
            if (resultado.get()) {
                exitosos++;
            }
        }
        assertThat(exitosos).isEqualTo(3);
        assertThat(claseRepository.findById(claseId).orElseThrow().getCuposDisponibles()).isZero();
        assertThat(cupoRepository.countByClaseId(claseId)).isEqualTo(3);
    }
}
