package com.palmas.transporte.modulos.identidad.web;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.palmas.transporte.modulos.identidad.api.LoginRequest;
import com.palmas.transporte.modulos.identidad.api.LoginResponse;
import com.palmas.transporte.modulos.identidad.application.AuthService;
import com.palmas.transporte.modulos.identidad.application.LimitadorLoginService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    private final LimitadorLoginService
        limitadorLoginService;

    public AuthController(
        AuthService authService,
        LimitadorLoginService limitadorLoginService
    ) {
        this.authService =
            authService;

        this.limitadorLoginService =
            limitadorLoginService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse>
        iniciarSesion(
            @Valid
            @RequestBody
            LoginRequest request,
            HttpServletRequest solicitudHttp
        ) {

        /*
         * El límite por IP se verifica antes de
         * ejecutar BCrypt o consultar credenciales.
         */
        limitadorLoginService.verificar(
            solicitudHttp
        );

        LoginResponse respuesta =
            authService.iniciarSesion(
                request,
                solicitudHttp
            );

        return ResponseEntity.ok().header("Cache-Control", "no-store").body(
            respuesta
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(
        @AuthenticationPrincipal Jwt jwt,
        HttpServletRequest solicitudHttp
    ) {
        authService.cerrarSesion(
            jwt,
            solicitudHttp
        );

        return ResponseEntity
            .noContent()
            .build();
    }
}
