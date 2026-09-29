package cl.duoc.gymflow.catalog.controller;

import cl.duoc.gymflow.catalog.dto.ClaseRequest;
import cl.duoc.gymflow.catalog.dto.ClaseResponse;
import cl.duoc.gymflow.catalog.service.ClaseService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Clases del catálogo. Los permisos por rol ya los aplicó el BFF (GET todos; POST/PUT/DELETE solo Admin);
 * este microservicio no está expuesto a internet.
 */
@RestController
@RequestMapping("/api/catalog/services")
public class ClaseController {

    private final ClaseService claseService;

    public ClaseController(ClaseService claseService) {
        this.claseService = claseService;
    }

    /** Filtros opcionales: {@code from}, {@code to} (ISO-8601, sobre el inicio de la clase) y {@code roomId}. */
    @GetMapping
    public List<ClaseResponse> listar(@RequestParam(required = false) Instant from,
                                      @RequestParam(required = false) Instant to,
                                      @RequestParam(required = false) Long roomId) {
        return claseService.listar(from, to, roomId);
    }

    @GetMapping("/{id}")
    public ClaseResponse obtener(@PathVariable Long id) {
        return claseService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<ClaseResponse> crear(@Valid @RequestBody ClaseRequest datos) {
        ClaseResponse creada = claseService.crear(datos);
        return ResponseEntity.created(URI.create("/api/catalog/services/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public ClaseResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClaseRequest datos) {
        return claseService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        claseService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
