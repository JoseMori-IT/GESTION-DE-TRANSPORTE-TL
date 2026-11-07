package com.palmas.transporte.modulos.mantenimiento.web;


import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.palmas.transporte.modulos.mantenimiento.api.CerrarMantenimientoRequest;
import com.palmas.transporte.modulos.mantenimiento.api.EvaluarMantenimientoRequest;
import com.palmas.transporte.modulos.mantenimiento.api.MantenimientoRequest;
import com.palmas.transporte.modulos.mantenimiento.api.MantenimientoResponse;
import com.palmas.transporte.modulos.mantenimiento.api.RepararMantenimientoRequest;
import com.palmas.transporte.modulos.mantenimiento.application.MantenimientoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mantenimientos")
public class MantenimientoController {

    private final MantenimientoService
        mantenimientoService;

    public MantenimientoController(
            MantenimientoService mantenimientoService) {

        this.mantenimientoService =
            mantenimientoService;
    }

    @GetMapping
    public List<MantenimientoResponse> listar() {
        return mantenimientoService.listar();
    }

    @GetMapping("/{id}")
    public MantenimientoResponse buscarPorId(
            @PathVariable Long id) {

        return mantenimientoService.buscarPorId(id);
    }

    @GetMapping("/vehiculo/{vehiculoId}")
    public List<MantenimientoResponse>
            listarPorVehiculo(
                @PathVariable Long vehiculoId
            ) {

        return mantenimientoService
            .listarPorVehiculo(vehiculoId);
    }

    @PostMapping
    public ResponseEntity<MantenimientoResponse>
            registrar(
                @Valid
                @RequestBody
                MantenimientoRequest request,

                @AuthenticationPrincipal
                Jwt jwt
            ) {

        MantenimientoResponse mantenimiento =
            mantenimientoService.registrar(
                request,
                jwt.getSubject()
            );

        return ResponseEntity
            .created(
                URI.create(
                    "/api/mantenimientos/"
                    + mantenimiento.id()
                )
            )
            .body(mantenimiento);
    }

    @PatchMapping("/{id}/evaluar")
    public MantenimientoResponse evaluar(
            @PathVariable Long id,
            @Valid
            @RequestBody
            EvaluarMantenimientoRequest request) {

        return mantenimientoService.evaluar(
            id,
            request
        );
    }

    @PatchMapping("/{id}/iniciar")
    public MantenimientoResponse iniciar(
            @PathVariable Long id) {

        return mantenimientoService.iniciar(id);
    }

    @PatchMapping("/{id}/reparar")
    public MantenimientoResponse reparar(
            @PathVariable Long id,
            @Valid
            @RequestBody
            RepararMantenimientoRequest request) {

        return mantenimientoService.reparar(
            id,
            request
        );
    }

    @PatchMapping("/{id}/cerrar")
    public MantenimientoResponse cerrar(
            @PathVariable Long id,
            @Valid
            @RequestBody
            CerrarMantenimientoRequest request) {

        return mantenimientoService.cerrar(
            id,
            request
        );
    }

    @PatchMapping("/{id}/cancelar")
    public MantenimientoResponse cancelar(
            @PathVariable Long id) {

        return mantenimientoService.cancelar(id);
    }
}