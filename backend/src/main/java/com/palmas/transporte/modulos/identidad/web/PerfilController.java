package com.palmas.transporte.modulos.identidad.web;


import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class PerfilController {

    private final UsuarioRepository usuarioRepository;

    public PerfilController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/perfil")
    public Map<String, Object> obtenerPerfil(
            @AuthenticationPrincipal Jwt jwt) {

        Object claimId = jwt.getClaim("usuarioId");
        if (!(claimId instanceof Number numero)) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "La sesión no contiene un usuario válido"
            );
        }

        Usuario usuario = usuarioRepository
            .findById(numero.longValue())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "La cuenta asociada a la sesión ya no existe"
            ));

        String nombreCompleto = (
            usuario.getNombres()
            + " "
            + (usuario.getApellidos() == null ? "" : usuario.getApellidos())
        ).trim();

        String primerNombre = obtenerPrimeraPalabra(
            usuario.getNombres()
        );

        String primerApellido = obtenerPrimeraPalabra(
            usuario.getApellidos()
        );

        String nombreCorto = (
            primerNombre + " " + primerApellido
        ).trim();

        Map<String, Object> perfil = new HashMap<>();

        perfil.put(
            "correo",
            usuario.getCorreo() != null
                ? usuario.getCorreo()
                : usuario.getDni()
        );

        perfil.put(
            "nombre",
            nombreCompleto
        );

        perfil.put(
            "nombreCorto",
            nombreCorto
        );

        perfil.put(
            "rol",
            usuario.getRol().name()
        );

        perfil.put(
            "usuarioId",
            usuario.getId()
        );

        perfil.put(
            "conductorId",
            jwt.getClaim("conductorId")
        );

        return perfil;
    }

    private String obtenerPrimeraPalabra(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }

        return valor.trim().split("\\s+")[0];
    }
}
