package com.palmas.transporte.modulos.identidad.web;
import com.palmas.transporte.modulos.identidad.api.CambiarContrasenaRequest;
import com.palmas.transporte.modulos.identidad.api.ContrasenaTemporalResponse;
import com.palmas.transporte.modulos.identidad.api.RestablecerContrasenaRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.palmas.transporte.modulos.identidad.application.ContrasenaService;
import com.palmas.transporte.modulos.identidad.application.LimitadorLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ContrasenaController {
    private final ContrasenaService servicio;
    private final LimitadorLoginService limite;
    public ContrasenaController(ContrasenaService servicio, LimitadorLoginService limite) {
        this.servicio = servicio;
        this.limite = limite;
    }

    @PostMapping("/auth/cambiar-contrasena")
    public ResponseEntity<Void> cambiar(@AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CambiarContrasenaRequest request, HttpServletRequest http) {
        limite.verificar(http);
        servicio.cambiar(jwt, request, http);
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }

    @PostMapping("/usuarios/{id}/restablecer-contrasena")
    public ResponseEntity<ContrasenaTemporalResponse> restablecer(@AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id, @Valid @RequestBody RestablecerContrasenaRequest request,
        HttpServletRequest http) {
        limite.verificar(http);
        return ResponseEntity.ok().header("Cache-Control", "no-store")
            .header("Pragma", "no-cache").body(servicio.restablecer(jwt, id, request, http));
    }
}
