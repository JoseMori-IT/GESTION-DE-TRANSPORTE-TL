package com.palmas.transporte.modulos.identidad.application;


import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.palmas.transporte.modulos.identidad.domain.Usuario;

@Service
public class JwtService {

    public static final String EMISOR =
        "sistema-transporte";

    private static final long EXPIRACION_MINIMA =
        300L;

    private static final long EXPIRACION_MAXIMA =
        86400L;

    private final JwtEncoder jwtEncoder;
    private final long expirationSeconds;

    public JwtService(
        @Value("${app.jwt.secret}")
        String secret,

        @Value("${app.jwt.expiration-seconds}")
        long expirationSeconds
    ) {
        if (
            secret == null
            || secret.isBlank()
        ) {
            throw new IllegalStateException(
                "La clave JWT no está configurada"
            );
        }

        byte[] clave =
            secret.getBytes(
                StandardCharsets.UTF_8
            );

        if (clave.length < 32) {
            throw new IllegalStateException(
                "La clave JWT debe tener al menos "
                    + "32 bytes"
            );
        }

        if (
            expirationSeconds < EXPIRACION_MINIMA
            || expirationSeconds > EXPIRACION_MAXIMA
        ) {
            throw new IllegalStateException(
                "La duración del JWT debe estar entre "
                    + EXPIRACION_MINIMA
                    + " y "
                    + EXPIRACION_MAXIMA
                    + " segundos"
            );
        }

        SecretKey secretKey =
            new SecretKeySpec(
                clave,
                "HmacSHA256"
            );

        this.jwtEncoder =
            NimbusJwtEncoder
                .withSecretKey(secretKey)
                .build();

        this.expirationSeconds =
            expirationSeconds;
    }

    public String generarToken(
        Usuario usuario
    ) {
        return generarToken(usuario, Instant.now(), null);
    }

    public String generarToken(Usuario usuario, Instant inicioSesion, Instant limiteSesion) {
        if (
            usuario == null
            || usuario.getId() == null
            || usuario.getRol() == null
        ) {
            throw new IllegalArgumentException(
                "No es posible generar el token "
                    + "para el usuario indicado"
            );
        }

        String identificador =
            obtenerIdentificador(usuario);

        String nombreCompleto = (
            usuario.getNombres()
            + " "
            + (usuario.getApellidos() == null
                ? ""
                : usuario.getApellidos())
        ).trim();

        Instant ahora =
            Instant.now();

        Long versionSeguridad =
            usuario.getVersionSeguridad() != null
                ? usuario.getVersionSeguridad()
                : 0L;

        JwtClaimsSet.Builder claims =
            JwtClaimsSet.builder()
                .issuer(EMISOR)
                .issuedAt(ahora)
                .expiresAt(limiteSesion != null && limiteSesion.isBefore(ahora.plusSeconds(expirationSeconds))
                    ? limiteSesion : ahora.plusSeconds(expirationSeconds))
                .claim("inicioSesion", inicioSesion.getEpochSecond())
                .id(
                    UUID.randomUUID()
                        .toString()
                )
                .subject(identificador)
                .claim(
                    "usuarioId",
                    usuario.getId()
                )
                .claim(
                    "nombre",
                    nombreCompleto
                )
                .claim(
                    "rol",
                    usuario.getRol().name()
                )
                .claim(
                    "identificador",
                    identificador
                )
                .claim(
                    "versionSeguridad",
                    versionSeguridad
                );

        if (
            usuario.getCorreo() != null
            && !usuario.getCorreo().isBlank()
        ) {
            claims.claim(
                "correo",
                usuario.getCorreo()
                    .trim()
                    .toLowerCase()
            );
        }

        if (
            usuario.getDni() != null
            && !usuario.getDni().isBlank()
        ) {
            claims.claim(
                "dni",
                usuario.getDni().trim()
            );
        }

        if (usuario.getConductor() != null) {
            claims.claim(
                "conductorId",
                usuario
                    .getConductor()
                    .getId()
            );
        }

        JwsHeader encabezado =
            JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        JwtEncoderParameters parametros =
            JwtEncoderParameters.from(
                encabezado,
                claims.build()
            );

        return jwtEncoder
            .encode(parametros)
            .getTokenValue();
    }

    private String obtenerIdentificador(
        Usuario usuario
    ) {
        if (
            usuario.getCorreo() != null
            && !usuario.getCorreo().isBlank()
        ) {
            return usuario.getCorreo()
                .trim()
                .toLowerCase();
        }

        if (
            usuario.getDni() != null
            && !usuario.getDni().isBlank()
        ) {
            return usuario.getDni().trim();
        }

        throw new IllegalArgumentException(
            "El usuario no tiene un identificador "
                + "válido"
        );
    }
}
