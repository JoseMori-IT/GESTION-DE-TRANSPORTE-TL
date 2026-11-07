package com.palmas.transporte.modulos.recorridos.web;


import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.palmas.transporte.modulos.recorridos.api.RutaRequest;
import com.palmas.transporte.modulos.recorridos.domain.Ruta;
import com.palmas.transporte.modulos.recorridos.application.RutaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rutas")
public class RutaController {

    private final RutaService rutaService;

    public RutaController(RutaService rutaService) {
        this.rutaService = rutaService;
    }

    @GetMapping
    public List<Ruta> listar() {
        return rutaService.listar();
    }

    @GetMapping("/{id}")
    public Ruta buscarPorId(@PathVariable Long id) {
        return rutaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Ruta> registrar(
            @Valid @RequestBody RutaRequest request) {

        Ruta ruta = rutaService.registrar(request);

        return ResponseEntity
            .created(URI.create("/api/rutas/" + ruta.getId()))
            .body(ruta);
    }

    @PutMapping("/{id}")
    public Ruta actualizar(
            @PathVariable Long id,
            @Valid @RequestBody RutaRequest request) {

        return rutaService.actualizar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    public Ruta desactivar(@PathVariable Long id) {
        return rutaService.desactivar(id);
    }
}