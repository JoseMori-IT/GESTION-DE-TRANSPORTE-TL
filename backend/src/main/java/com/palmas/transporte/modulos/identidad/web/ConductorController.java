package com.palmas.transporte.modulos.identidad.web;


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

import com.palmas.transporte.modulos.identidad.api.ConductorRequest;
import com.palmas.transporte.modulos.identidad.domain.Conductor;
import com.palmas.transporte.modulos.identidad.application.ConductorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/conductores")
public class ConductorController {

    private final ConductorService conductorService;

    public ConductorController(
            ConductorService conductorService) {
        this.conductorService = conductorService;
    }

    @GetMapping
    public List<Conductor> listar() {
        return conductorService.listar();
    }

    @GetMapping("/{id}")
    public Conductor buscarPorId(@PathVariable Long id) {
        return conductorService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Conductor> registrar(
            @Valid @RequestBody ConductorRequest request) {

        Conductor conductor =
            conductorService.registrar(request);

        return ResponseEntity
            .created(
                URI.create(
                    "/api/conductores/" + conductor.getId()
                )
            )
            .body(conductor);
    }

    @PutMapping("/{id}")
    public Conductor actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ConductorRequest request) {

        return conductorService.actualizar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    public Conductor desactivar(@PathVariable Long id) {
        return conductorService.desactivar(id);
    }
}