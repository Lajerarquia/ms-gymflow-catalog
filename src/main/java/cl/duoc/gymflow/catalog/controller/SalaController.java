package cl.duoc.gymflow.catalog.controller;

import cl.duoc.gymflow.catalog.dto.SalaRequest;
import cl.duoc.gymflow.catalog.dto.SalaResponse;
import cl.duoc.gymflow.catalog.service.SalaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Salas. Los permisos ya los aplicó el BFF (GET Admin e Instructor; POST/PUT/DELETE solo Admin). */
@RestController
@RequestMapping("/api/catalog/rooms")
public class SalaController {

    private final SalaService salaService;

    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    @GetMapping
    public List<SalaResponse> listar() {
        return salaService.listar();
    }

    @GetMapping("/{id}")
    public SalaResponse obtener(@PathVariable Long id) {
        return salaService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<SalaResponse> crear(@Valid @RequestBody SalaRequest datos) {
        SalaResponse creada = salaService.crear(datos);
        return ResponseEntity.created(URI.create("/api/catalog/rooms/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public SalaResponse actualizar(@PathVariable Long id, @Valid @RequestBody SalaRequest datos) {
        return salaService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        salaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
