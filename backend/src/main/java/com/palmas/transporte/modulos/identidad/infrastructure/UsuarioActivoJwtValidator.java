package com.palmas.transporte.modulos.identidad.infrastructure;


import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;

@Component
public class UsuarioActivoJwtValidator
        implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error
        ERROR_TOKEN_INVALIDO =
            new OAuth2Error(
                "invalid_token",
                "El token no es válido o "
                    + "la sesión ya no está activa",
                null
            );

    private final UsuarioRepository
        usuarioRepository;

    public UsuarioActivoJwtValidator(
        UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository =
            usuarioRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(
        Jwt jwt
    ) {
        if (jwt == null) {
            return fallo();
        }

        Long usuarioId =
            obtenerLong(
                jwt.getClaim("usuarioId")
            );

        Long versionToken =
            obtenerLong(
                jwt.getClaim(
                    "versionSeguridad"
                )
            );

        if (
            usuarioId == null
            || versionToken == null
        ) {
            return fallo();
        }

        boolean tokenValido =
            usuarioRepository
                .findById(usuarioId)
                .filter(usuario ->
                    Boolean.TRUE.equals(
                        usuario.getActivo()
                    )
                )
                .filter(usuario ->
                    versionesCoinciden(
                        usuario,
                        versionToken
                    )
                )
                .filter(usuario -> !Boolean.TRUE.equals(usuario.getCambioContrasenaRequerido())
                    || usuario.getContrasenaTemporalHasta() == null
                    || usuario.getContrasenaTemporalHasta().isAfter(java.time.LocalDateTime.now()))
                .isPresent();

        if (!tokenValido) {
            return fallo();
        }

        return OAuth2TokenValidatorResult
            .success();
    }

    private boolean versionesCoinciden(
        Usuario usuario,
        Long versionToken
    ) {
        Long versionActual =
            usuario.getVersionSeguridad() != null
                ? usuario.getVersionSeguridad()
                : 0L;

        return versionActual.equals(
            versionToken
        );
    }

    private Long obtenerLong(
        Object valor
    ) {
        if (valor instanceof Number numero) {
            return numero.longValue();
        }

        if (valor != null) {
            try {
                return Long.valueOf(
                    valor.toString()
                );
            } catch (
                NumberFormatException ignored
            ) {
                return null;
            }
        }

        return null;
    }

    private OAuth2TokenValidatorResult fallo() {
        return OAuth2TokenValidatorResult
            .failure(
                ERROR_TOKEN_INVALIDO
            );
    }
}
