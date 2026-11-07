package com.palmas.transporte.modulos.identidad.application;
import com.palmas.transporte.modulos.auditoria.application.AuditoriaService;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.palmas.transporte.modulos.identidad.api.LoginRequest;
import com.palmas.transporte.modulos.identidad.api.LoginResponse;
import com.palmas.transporte.modulos.auditoria.domain.ResultadoAuditoria;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {

    private static final String
        MENSAJE_CREDENCIALES_INVALIDAS =
            "Credenciales incorrectas";

    private final UsuarioRepository
        usuarioRepository;

    private final PasswordEncoder
        passwordEncoder;

    private final JwtService jwtService;

    private final AuditoriaService
        auditoriaService;

    private final int maximoIntentos;
    private final int minutosBloqueo;

    /*
     * Se utiliza para ejecutar BCrypt aun cuando
     * el identificador no existe y reducir las
     * diferencias de tiempo observables.
     */
    private final String hashFicticio;

    public AuthService(
        UsuarioRepository usuarioRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AuditoriaService auditoriaService,

        @Value("${app.auth.max-intentos:5}")
        int maximoIntentos,

        @Value("${app.auth.minutos-bloqueo:15}")
        int minutosBloqueo
    ) {
        if (
            maximoIntentos < 3
            || maximoIntentos > 10
        ) {
            throw new IllegalStateException(
                "app.auth.max-intentos debe estar "
                    + "entre 3 y 10"
            );
        }

        if (
            minutosBloqueo < 1
            || minutosBloqueo > 1440
        ) {
            throw new IllegalStateException(
                "app.auth.minutos-bloqueo debe estar "
                    + "entre 1 y 1440"
            );
        }

        this.usuarioRepository =
            usuarioRepository;

        this.passwordEncoder =
            passwordEncoder;

        this.jwtService =
            jwtService;

        this.auditoriaService =
            auditoriaService;

        this.maximoIntentos =
            maximoIntentos;

        this.minutosBloqueo =
            minutosBloqueo;

        this.hashFicticio =
            passwordEncoder.encode(
                UUID.randomUUID()
                    .toString()
            );
    }

    /*
     * noRollbackFor conserva el contador y el bloqueo
     * aunque se devuelva una respuesta HTTP de rechazo.
     */
    @Transactional(
        noRollbackFor = ResponseStatusException.class
    )
    public LoginResponse iniciarSesion(
        LoginRequest request,
        HttpServletRequest solicitudHttp
    ) {
        String identificador =
            normalizarIdentificador(
                request.identificador()
            );

        String contrasena =
            request.contrasena();

        Optional<Usuario> usuarioEncontrado =
            buscarUsuarioParaAutenticacion(
                identificador
            );

        if (usuarioEncontrado.isEmpty()) {
            /*
             * Comparación intencional para aproximar el
             * costo de un login con una cuenta real.
             */
            passwordEncoder.matches(
                contrasena,
                hashFicticio
            );

            auditoriaService
                .registrarAutenticacion(
                    null,
                    identificador,
                    solicitudHttp,
                    "INICIAR_SESION",
                    "Intento con identificador "
                        + "no registrado",
                    ResultadoAuditoria.RECHAZADO
                );

            throw credencialesInvalidas();
        }

        Usuario usuario =
            usuarioEncontrado.get();

        /*
         * La contraseña se compara antes de responder
         * sobre el estado de la cuenta. La respuesta
         * pública será siempre la misma.
         */
        boolean contrasenaCorrecta =
            passwordEncoder.matches(
                contrasena,
                usuario.getContrasenaHash()
            );

        LocalDateTime ahora =
            LocalDateTime.now();

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            auditoriaService
                .registrarAutenticacion(
                    usuario,
                    identificador,
                    solicitudHttp,
                    "INICIAR_SESION",
                    "Intento con una cuenta desactivada",
                    ResultadoAuditoria.RECHAZADO
                );

            throw credencialesInvalidas();
        }

        if (
            usuario.getBloqueadoHasta() != null
            && usuario
                .getBloqueadoHasta()
                .isAfter(ahora)
        ) {
            auditoriaService
                .registrarAutenticacion(
                    usuario,
                    identificador,
                    solicitudHttp,
                    "INICIAR_SESION",
                    "Intento durante el bloqueo "
                        + "temporal de la cuenta",
                    ResultadoAuditoria.RECHAZADO
                );

            throw credencialesInvalidas();
        }

        /*
         * Si el bloqueo venció, se reinicia el contador
         * antes de evaluar el nuevo intento.
         */
        if (usuario.getBloqueadoHasta() != null) {
            usuario.setBloqueadoHasta(null);
            usuario.setIntentosFallidos(0);
        }

        if (!contrasenaCorrecta) {
            registrarIntentoFallido(
                usuario,
                identificador,
                solicitudHttp,
                ahora
            );
        }

        usuario.setIntentosFallidos(0);
        if (Boolean.TRUE.equals(usuario.getCambioContrasenaRequerido())
            && usuario.getContrasenaTemporalHasta() != null
            && !usuario.getContrasenaTemporalHasta().isAfter(ahora)) {
            auditoriaService.registrarAutenticacion(usuario, identificador, solicitudHttp,
                "INICIAR_SESION", "Contrasena temporal vencida", ResultadoAuditoria.RECHAZADO);
            throw credencialesInvalidas();
        }
        usuario.setBloqueadoHasta(null);

        registrarDatosAcceso(
            usuario,
            solicitudHttp,
            ahora
        );

        usuarioRepository.saveAndFlush(
            usuario
        );

        String token =
            jwtService.generarToken(
                usuario
            );

        Long conductorId =
            usuario.getConductor() != null
                ? usuario
                    .getConductor()
                    .getId()
                : null;

        auditoriaService
            .registrarAutenticacion(
                usuario,
                identificador,
                solicitudHttp,
                "INICIAR_SESION",
                "Inicio de sesión exitoso",
                ResultadoAuditoria.EXITOSO
            );

        return new LoginResponse(
            usuario.getId(),
            usuario.getNombres(),
            usuario.getCorreo(),
            usuario.getRol().name(),
            conductorId,
            token,
            "Inicio de sesión correcto",
            Boolean.TRUE.equals(usuario.getCambioContrasenaRequerido())
        );
    }

    /*
     * Incrementar versionSeguridad invalida todos los
     * JWT emitidos anteriormente para este usuario.
     */
    @Transactional
    public void cerrarSesion(
        Jwt jwt,
        HttpServletRequest solicitudHttp
    ) {
        Long usuarioId =
            obtenerUsuarioId(jwt);

        if (usuarioId != null) {
            usuarioRepository
                .buscarPorIdParaActualizacion(
                    usuarioId
                )
                .ifPresent(usuario -> {
                    long versionActual =
                        usuario
                            .getVersionSeguridad()
                            != null
                                ? usuario
                                    .getVersionSeguridad()
                                : 0L;

                    usuario.setVersionSeguridad(
                        versionActual + 1L
                    );

                    usuarioRepository.saveAndFlush(
                        usuario
                    );
                });
        }

        auditoriaService.registrar(
            jwt,
            solicitudHttp,
            "CERRAR_SESION",
            "AUTENTICACION",
            "Usuario",
            usuarioId != null
                ? usuarioId.toString()
                : null,
            obtenerIdentificadorJwt(jwt),
            "Cierre de sesión solicitado "
                + "por el usuario",
            null,
            null,
            ResultadoAuditoria.EXITOSO
        );
    }

    private void registrarIntentoFallido(
        Usuario usuario,
        String identificador,
        HttpServletRequest solicitudHttp,
        LocalDateTime ahora
    ) {
        int intentosActuales =
            usuario.getIntentosFallidos() != null
                ? usuario.getIntentosFallidos()
                : 0;

        int intentos =
            intentosActuales + 1;

        usuario.setIntentosFallidos(
            intentos
        );

        if (intentos >= maximoIntentos) {
            usuario.setBloqueadoHasta(
                ahora.plusMinutes(
                    minutosBloqueo
                )
            );

            usuarioRepository.saveAndFlush(
                usuario
            );

            auditoriaService
                .registrarAutenticacion(
                    usuario,
                    identificador,
                    solicitudHttp,
                    "BLOQUEAR_CUENTA",
                    "Cuenta bloqueada durante "
                        + minutosBloqueo
                        + " minutos tras "
                        + intentos
                        + " intentos fallidos",
                    ResultadoAuditoria.RECHAZADO
                );

            throw credencialesInvalidas();
        }

        usuarioRepository.saveAndFlush(
            usuario
        );

        int intentosRestantes =
            maximoIntentos - intentos;

        /*
         * Los intentos restantes quedan únicamente en
         * auditoría; no se revelan al cliente.
         */
        auditoriaService
            .registrarAutenticacion(
                usuario,
                identificador,
                solicitudHttp,
                "INICIAR_SESION",
                "Contraseña incorrecta. "
                    + "Intentos restantes: "
                    + intentosRestantes,
                ResultadoAuditoria.RECHAZADO
            );

        throw credencialesInvalidas();
    }

    private Optional<Usuario>
        buscarUsuarioParaAutenticacion(
            String identificador
        ) {

        if (identificador == null) {
            return Optional.empty();
        }

        if (esDni(identificador)) {
            return usuarioRepository
                .buscarPorDniParaAutenticacion(
                    identificador
                );
        }

        return usuarioRepository
            .buscarPorCorreoParaAutenticacion(
                identificador
            );
    }

    private void registrarDatosAcceso(
        Usuario usuario,
        HttpServletRequest solicitud,
        LocalDateTime ahora
    ) {
        if (
            usuario.getFechaPrimerLogin()
                == null
        ) {
            usuario.setFechaPrimerLogin(
                ahora
            );
        }

        usuario.setFechaUltimoLogin(
            ahora
        );

        usuario.setFechaUltimaConexion(
            ahora
        );

        usuario.setUltimaDireccionIp(
            obtenerDireccionIp(solicitud)
        );

        usuario.setUltimoUserAgent(
            obtenerUserAgent(solicitud)
        );
    }

    /*
     * No confía directamente en X-Forwarded-For ni
     * X-Real-IP porque un cliente puede falsificarlos.
     * Cuando exista un proxy corporativo se deberá
     * configurar como proxy confiable en Spring.
     */
    private String obtenerDireccionIp(
        HttpServletRequest solicitud
    ) {
        if (solicitud == null) {
            return null;
        }

        return limitar(
            solicitud.getRemoteAddr(),
            45
        );
    }

    private String obtenerUserAgent(
        HttpServletRequest solicitud
    ) {
        if (solicitud == null) {
            return null;
        }

        return limitar(
            solicitud.getHeader(
                "User-Agent"
            ),
            500
        );
    }

    private Long obtenerUsuarioId(
        Jwt jwt
    ) {
        if (jwt == null) {
            return null;
        }

        Object valor =
            jwt.getClaim("usuarioId");

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

    private String obtenerIdentificadorJwt(
        Jwt jwt
    ) {
        if (jwt == null) {
            return null;
        }

        String identificador =
            jwt.getClaimAsString(
                "identificador"
            );

        if (
            identificador != null
            && !identificador.isBlank()
        ) {
            return limitar(
                identificador.trim(),
                100
            );
        }

        return limitar(
            jwt.getSubject(),
            100
        );
    }

    private String normalizarIdentificador(
        String identificador
    ) {
        if (
            identificador == null
            || identificador.isBlank()
        ) {
            return null;
        }

        return identificador
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    private boolean esDni(
        String identificador
    ) {
        return identificador != null
            && identificador.matches(
                "\\d{8}"
            );
    }

    private String limitar(
        String valor,
        int longitud
    ) {
        if (valor == null) {
            return null;
        }

        String limpio =
            valor.trim();

        if (limpio.isBlank()) {
            return null;
        }

        return limpio.length() <= longitud
            ? limpio
            : limpio.substring(
                0,
                longitud
            );
    }

    private ResponseStatusException
        credencialesInvalidas() {

        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            MENSAJE_CREDENCIALES_INVALIDAS
        );
    }
}
