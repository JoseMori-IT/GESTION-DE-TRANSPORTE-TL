package com.palmas.transporte.modulos.identidad.web;


import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.palmas.transporte.modulos.identidad.api.UsuarioRequest;
import com.palmas.transporte.modulos.identidad.api.UsuarioResponse;
import com.palmas.transporte.modulos.identidad.api.ContrasenaTemporalResponse;
import com.palmas.transporte.modulos.identidad.application.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(
            UsuarioService usuarioService) {

        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
            usuarioService.listar(jwt)
        );
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> registrar(
            @Valid @RequestBody UsuarioRequest request) {

        UsuarioResponse usuario =
            usuarioService.registrar(request);

        return ResponseEntity
            .created(
                URI.create(
                    "/api/usuarios/" + usuario.id()
                )
            )
            .body(usuario);
    }

    @PostMapping("/chofer/{conductorId}/crear-acceso")
    public ResponseEntity<ContrasenaTemporalResponse>
        crearAccesoChofer(
            @PathVariable Long conductorId
        ) {

        return ResponseEntity.ok()
            .header("Cache-Control", "no-store")
            .header("Pragma", "no-cache")
            .body(
                usuarioService
                    .crearAccesoTemporalChofer(
                        conductorId
                    )
            );
    }
}
