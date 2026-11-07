package com.palmas.transporte.modulos.evaluacion.web;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.palmas.transporte.modulos.evaluacion.api.EncuestaServicioRequest;
import com.palmas.transporte.modulos.evaluacion.api.EncuestaServicioResponse;
import com.palmas.transporte.modulos.evaluacion.application.EncuestaServicioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/encuestas")
public class EncuestaServicioController {

    private final EncuestaServicioService encuestaService;

    public EncuestaServicioController(
            EncuestaServicioService encuestaService) {

        this.encuestaService = encuestaService;
    }

    @GetMapping
    public List<EncuestaServicioResponse> listar() {
        return encuestaService.listar();
    }

    @GetMapping("/{id}")
    public EncuestaServicioResponse buscarPorId(
            @PathVariable Long id) {

        return encuestaService.buscarPorId(id);
    }

    @GetMapping("/servicio/{servicioId}")
    public EncuestaServicioResponse buscarPorServicio(
            @PathVariable Long servicioId) {

        return encuestaService.buscarPorServicio(
            servicioId
        );
    }

    @PostMapping("/servicio/{servicioId}")
    @ResponseStatus(HttpStatus.CREATED)
    public EncuestaServicioResponse registrar(
            @PathVariable Long servicioId,
            @Valid
            @RequestBody EncuestaServicioRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return encuestaService.registrar(
            servicioId,
            request,
            obtenerNombre(jwt),
            jwt.getSubject()
        );
    }

    private String obtenerNombre(Jwt jwt) {
        String nombre = jwt.getClaimAsString(
            "nombre"
        );

        if (nombre == null || nombre.isBlank()) {
            return jwt.getSubject();
        }

        return nombre;
    }
}