package com.palmas.transporte.modulos.operacion.web;


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
import org.springframework.web.server.ResponseStatusException;

import com.palmas.transporte.modulos.operacion.api.MarcacionServicioRequest;
import com.palmas.transporte.modulos.operacion.api.MarcacionServicioResponse;
import com.palmas.transporte.modulos.operacion.application.MarcacionServicioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chofer/servicios/{servicioId}/marcaciones")
public class MarcacionServicioController {

    private final MarcacionServicioService marcacionService;

    public MarcacionServicioController(MarcacionServicioService marcacionService) {
        this.marcacionService = marcacionService;
    }

    @GetMapping
    public List<MarcacionServicioResponse> listar(
        @PathVariable Long servicioId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return marcacionService.listar(servicioId, obtenerConductorId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MarcacionServicioResponse registrar(
        @PathVariable Long servicioId,
        @Valid @RequestBody MarcacionServicioRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return marcacionService.registrar(
            servicioId,
            obtenerConductorId(jwt),
            request
        );
    }

    private Long obtenerConductorId(Jwt jwt) {
        Object valor = jwt.getClaim("conductorId");
        try {
            return valor instanceof Number numero
                ? numero.longValue()
                : Long.valueOf(String.valueOf(valor));
        } catch (Exception exception) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "La cuenta no está vinculada con un conductor"
            );
        }
    }
}
