package cl.duoc.gymflow.catalog.controller;

import cl.duoc.gymflow.catalog.dto.CupoRequest;
import cl.duoc.gymflow.catalog.dto.CupoResponse;
import cl.duoc.gymflow.catalog.service.CupoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rutas internas de cupo (ver {@link RutasInternas}). */
@RestController
@RequestMapping(RutasInternas.BASE)
public class CupoController {

    private final CupoService cupoService;

    public CupoController(CupoService cupoService) {
        this.cupoService = cupoService;
    }

    @PostMapping(RutasInternas.TOMAR_CUPO)
    public CupoResponse tomar(@PathVariable Long id, @Valid @RequestBody CupoRequest datos) {
        return cupoService.tomar(id, datos.reservationId());
    }

    @PostMapping(RutasInternas.DEVOLVER_CUPO)
    public CupoResponse devolver(@PathVariable Long id, @Valid @RequestBody CupoRequest datos) {
        return cupoService.devolver(id, datos.reservationId());
    }
}
