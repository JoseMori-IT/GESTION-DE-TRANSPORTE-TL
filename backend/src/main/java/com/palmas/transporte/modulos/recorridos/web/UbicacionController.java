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

import com.palmas.transporte.modulos.recorridos.api.UbicacionRequest;
import com.palmas.transporte.modulos.recorridos.domain.Ubicacion;
import com.palmas.transporte.modulos.recorridos.application.UbicacionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ubicaciones")
public class UbicacionController {

    private final UbicacionService ubicacionService;

    public UbicacionController(UbicacionService ubicacionService) {
        this.ubicacionService = ubicacionService;
    }

    @GetMapping
    public List<Ubicacion> listar() {
        return ubicacionService.listar();
    }

    @PostMapping
    public ResponseEntity<Ubicacion> registrar(
        @Valid @RequestBody UbicacionRequest request
    ) {
        Ubicacion ubicacion = ubicacionService.registrar(request);
        return ResponseEntity
            .created(URI.create("/api/ubicaciones/" + ubicacion.getId()))
            .body(ubicacion);
    }

    @PutMapping("/{id}")
    public Ubicacion actualizar(
        @PathVariable Long id,
        @Valid @RequestBody UbicacionRequest request
    ) {
        return ubicacionService.actualizar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    public Ubicacion desactivar(@PathVariable Long id) {
        return ubicacionService.desactivar(id);
    }
}
