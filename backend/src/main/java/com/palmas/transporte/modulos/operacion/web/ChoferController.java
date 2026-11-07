package com.palmas.transporte.modulos.operacion.web;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.palmas.transporte.modulos.operacion.api.EstadoServicioRequest;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.application.ChoferService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chofer/servicios")
public class ChoferController {

    private final ChoferService choferService;

    public ChoferController(
            ChoferService choferService) {

        this.choferService = choferService;
    }

    @GetMapping
    public List<ServicioTransporte> listar(
            @AuthenticationPrincipal Jwt jwt) {

        return choferService.listarServicios(
            obtenerConductorId(jwt)
        );
    }

    @PatchMapping("/{id}/estado")
public ServicioTransporte cambiarEstado(
        @PathVariable Long id,
        @Valid @RequestBody
        EstadoServicioRequest request,
        @AuthenticationPrincipal Jwt jwt) {

    return choferService.cambiarEstado(
        obtenerConductorId(jwt),
        id,
        request.estado()
    );
}

    private Long obtenerConductorId(Jwt jwt) {
        Object claim = jwt.getClaim("conductorId");

        if (claim instanceof Number numero) {
            return numero.longValue();
        }

        if (claim instanceof String texto) {
            try {
                return Long.valueOf(texto);
            } catch (NumberFormatException exception) {
                throw accesoDenegado();
            }
        }

        throw accesoDenegado();
    }

    private ResponseStatusException accesoDenegado() {
        return new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "La cuenta no está vinculada con un conductor"
        );
    }
}