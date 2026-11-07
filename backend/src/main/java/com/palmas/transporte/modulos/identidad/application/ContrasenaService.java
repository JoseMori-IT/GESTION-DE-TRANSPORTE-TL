package com.palmas.transporte.modulos.identidad.application;
import com.palmas.transporte.modulos.auditoria.domain.Auditoria;
import com.palmas.transporte.modulos.auditoria.domain.ResultadoAuditoria;
import com.palmas.transporte.modulos.auditoria.persistence.AuditoriaRepository;
import com.palmas.transporte.modulos.identidad.api.CambiarContrasenaRequest;
import com.palmas.transporte.modulos.identidad.api.ContrasenaTemporalResponse;
import com.palmas.transporte.modulos.identidad.api.RestablecerContrasenaRequest;
import com.palmas.transporte.modulos.identidad.domain.Rol;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;



import jakarta.servlet.http.HttpServletRequest;

@Service
public class ContrasenaService {
    private final UsuarioRepository usuarios;
    private final AuditoriaRepository auditorias;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();
    private final int maxIntentos;
    private final int minutosBloqueo;

    public ContrasenaService(UsuarioRepository usuarios, AuditoriaRepository auditorias,
        PasswordEncoder encoder, @Value("${app.auth.max-intentos:5}") int maxIntentos,
        @Value("${app.auth.minutos-bloqueo:15}") int minutosBloqueo) {
        this.usuarios = usuarios;
        this.auditorias = auditorias;
        this.encoder = encoder;
        this.maxIntentos = Math.max(1, maxIntentos);
        this.minutosBloqueo = Math.max(1, minutosBloqueo);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void cambiar(Jwt jwt, CambiarContrasenaRequest request, HttpServletRequest http) {
        Usuario usuario = bloquear(id(jwt));
        validarSesion(usuario, jwt);
        LocalDateTime ahora = LocalDateTime.now();
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(ahora)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Espera antes de volver a intentar cambiar la contrasena");
        }
        if (Boolean.TRUE.equals(usuario.getCambioContrasenaRequerido())
            && usuario.getContrasenaTemporalHasta() != null
            && !usuario.getContrasenaTemporalHasta().isAfter(ahora)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesion ya no es valida");
        }
        validarNueva(request.contrasenaNueva());
        if (!encoder.matches(request.contrasenaActual(), usuario.getContrasenaHash())) {
            int intentos = (usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos()) + 1;
            usuario.setIntentosFallidos(intentos);
            if (intentos >= maxIntentos) {
                usuario.setBloqueadoHasta(ahora.plusMinutes(minutosBloqueo));
                revocar(usuario);
            }
            auditar(usuario, usuario, http, "CAMBIAR_CONTRASENA", "Clave actual incorrecta",
                ResultadoAuditoria.RECHAZADO);
            usuarios.saveAndFlush(usuario);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contrasena actual es incorrecta");
        }
        if (encoder.matches(request.contrasenaNueva(), usuario.getContrasenaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La nueva contrasena debe ser diferente de la actual");
        }
        usuario.setContrasenaHash(encoder.encode(request.contrasenaNueva()));
        usuario.setCambioContrasenaRequerido(false);
        usuario.setContrasenaTemporalHasta(null);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        revocar(usuario);
        usuarios.saveAndFlush(usuario);
        auditar(usuario, usuario, http, "CAMBIAR_CONTRASENA", "Contrasena cambiada; sesiones revocadas",
            ResultadoAuditoria.EXITOSO);
    }

    @Transactional
    public ContrasenaTemporalResponse restablecer(Jwt jwt, Long destinoId,
        RestablecerContrasenaRequest request, HttpServletRequest http) {
        Long actorId = id(jwt);
        if (actorId.equals(destinoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Utiliza el cambio de contrasena para tu propia cuenta");
        }
        if (destinoId == null || destinoId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuario invalido");
        }
        // Bloqueos en orden fijo para evitar actor/destino cruzados.
        Map<Long, Usuario> bloqueados = new TreeMap<>();
        java.util.stream.Stream.of(actorId, destinoId).sorted()
            .forEach(valor -> bloqueados.put(valor, bloquear(valor)));
        Usuario actor = bloqueados.get(actorId);
        Usuario destino = bloqueados.get(destinoId);
        validarSesion(actor, jwt);
        if (!puedeRestablecer(actor.getRol(), destino.getRol())
            || Boolean.TRUE.equals(actor.getCambioContrasenaRequerido())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operacion no permitida");
        }
        if (destino.getRol() == Rol.MASTER
            || destino.getRol() == Rol.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "El restablecimiento de la cuenta maestra requiere el procedimiento de TI");
        }
        if (!Boolean.TRUE.equals(destino.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede restablecer una cuenta desactivada");
        }
        if (request.motivo() == null || request.motivo().isBlank() || request.motivo().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica un motivo valido");
        }
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder temporal = new StringBuilder();
        for (int i = 0; i < 20; i++) temporal.append(caracteres.charAt(random.nextInt(caracteres.length())));
        LocalDateTime vence = LocalDateTime.now().plusHours(24);
        destino.setContrasenaHash(encoder.encode(temporal));
        destino.setCambioContrasenaRequerido(true);
        destino.setContrasenaTemporalHasta(vence);
        destino.setIntentosFallidos(0);
        destino.setBloqueadoHasta(null);
        revocar(destino);
        usuarios.saveAndFlush(destino);
        auditar(actor, destino, http, "RESTABLECER_CONTRASENA",
            "Restablecimiento autorizado. Motivo: " + request.motivo().trim(), ResultadoAuditoria.EXITOSO);
        return new ContrasenaTemporalResponse(destino.getId(),
            destino.getRol() == Rol.CHOFER ? destino.getDni() : destino.getCorreo(),
            temporal.toString(), vence);
    }

    private Usuario bloquear(Long id) {
        return usuarios.buscarPorIdParaActualizacion(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontro la cuenta"));
    }

    private boolean puedeRestablecer(Rol actor, Rol destino) {
        if (actor == Rol.MASTER || actor == Rol.ADMINISTRADOR) {
            return destino != Rol.MASTER && destino != Rol.ADMINISTRADOR;
        }

        if (actor == Rol.COORDINADOR) {
            return destino == Rol.OPERADOR
                || destino == Rol.CONSULTA
                || destino == Rol.CHOFER;
        }

        return false;
    }

    private Long id(Jwt jwt) {
        Object valor = jwt == null ? null : jwt.getClaim("usuarioId");
        if (!(valor instanceof Number numero) || numero.longValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion invalida");
        }
        return numero.longValue();
    }

    private void validarSesion(Usuario usuario, Jwt jwt) {
        Object version = jwt.getClaim("versionSeguridad");
        long actual = usuario.getVersionSeguridad() == null ? 0 : usuario.getVersionSeguridad();
        if (!Boolean.TRUE.equals(usuario.getActivo()) || !(version instanceof Number numero)
            || numero.longValue() != actual) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesion ya no es valida");
        }
    }

    private void validarNueva(String clave) {
        if (clave == null || clave.isBlank() || clave.length() < 12 || clave.length() > 64
            || clave.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Utiliza entre 12 y 64 caracteres, hasta 72 bytes UTF-8");
        }
    }

    private void revocar(Usuario usuario) {
        usuario.setVersionSeguridad(Math.addExact(
            usuario.getVersionSeguridad() == null ? 0L : usuario.getVersionSeguridad(), 1L));
    }

    private void auditar(Usuario actor, Usuario destino, HttpServletRequest http,
        String accion, String detalle, ResultadoAuditoria resultado) {
        // Misma transaccion que la credencial; nunca serializar DTO, hash ni claves.
        Auditoria a = new Auditoria();
        a.setUsuarioId(actor.getId());
        a.setUsuarioNombre(actor.getNombres());
        a.setUsuarioCorreo(actor.getCorreo());
        a.setUsuarioRol(actor.getRol().name());
        a.setAccion(accion);
        a.setModulo("AUTENTICACION");
        a.setEntidad("Usuario");
        a.setEntidadId(destino.getId().toString());
        a.setCodigoReferencia("USUARIO-" + destino.getId());
        a.setDetalle(detalle);
        if (http != null) {
            String ip = http.getRemoteAddr();
            a.setDireccionIp(ip == null ? null : ip.substring(0, Math.min(45, ip.length())));
            String agente = http.getHeader("User-Agent");
            a.setUserAgent(agente == null ? null : agente.substring(0, Math.min(500, agente.length())));
        }
        a.setResultado(resultado);
        auditorias.save(a);
    }
}
