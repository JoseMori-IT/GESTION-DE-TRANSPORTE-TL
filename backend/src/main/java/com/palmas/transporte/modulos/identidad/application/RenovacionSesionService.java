package com.palmas.transporte.modulos.identidad.application;

import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.Objects;

@Service
public class RenovacionSesionService {
    private final UsuarioRepository usuarios;
    private final JwtService tokens;
    private final long duracionMaxima;
    public RenovacionSesionService(UsuarioRepository usuarios, JwtService tokens,
        @Value("${app.auth.sesion-maxima-segundos:28800}") long duracionMaxima) {
        if(duracionMaxima<300 || duracionMaxima>86400)throw new IllegalStateException("Revisa la duración máxima de la sesión.");
        this.usuarios=usuarios;this.tokens=tokens;this.duracionMaxima=duracionMaxima;
    }
    @Transactional(readOnly=true)
    public String renovar(Jwt jwt) {
        var ahora=Instant.now();
        Object id=jwt.getClaim("usuarioId"), version=jwt.getClaim("versionSeguridad"), origen=jwt.getClaim("inicioSesion");
        if(!(id instanceof Number numero) || !(version instanceof Number v) || jwt.getExpiresAt()==null || !jwt.getExpiresAt().isAfter(ahora))throw rechazar();
        var inicio=origen instanceof Number n?Instant.ofEpochSecond(n.longValue()):jwt.getIssuedAt();
        if(inicio==null || inicio.isAfter(ahora.plusSeconds(30)) || !inicio.plusSeconds(duracionMaxima).isAfter(ahora))throw rechazar();
        var usuario=usuarios.findById(numero.longValue()).orElseThrow(this::rechazar);
        if(!Boolean.TRUE.equals(usuario.getActivo()) || !Objects.equals(usuario.getVersionSeguridad(),v.longValue()) || Boolean.TRUE.equals(usuario.getCambioContrasenaRequerido()))throw rechazar();
        return tokens.generarToken(usuario,inicio,inicio.plusSeconds(duracionMaxima));
    }
    private ResponseStatusException rechazar(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Tu sesión terminó. Ingresa nuevamente para continuar.");}
}
