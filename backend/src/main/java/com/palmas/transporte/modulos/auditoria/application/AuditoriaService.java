package com.palmas.transporte.modulos.auditoria.application;


import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.palmas.transporte.modulos.auditoria.domain.Auditoria;
import com.palmas.transporte.modulos.auditoria.domain.ResultadoAuditoria;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.auditoria.persistence.AuditoriaRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(
            AuditoriaRepository auditoriaRepository) {

        this.auditoriaRepository = auditoriaRepository;
    }

    /*
     * Registra operaciones realizadas por un usuario
     * autenticado mediante su token JWT.
     */
    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public void registrar(
            Jwt jwt,
            HttpServletRequest solicitudHttp,
            String accion,
            String modulo,
            String entidad,
            String entidadId,
            String codigoReferencia,
            String detalle,
            Object valorAnterior,
            Object valorNuevo,
            ResultadoAuditoria resultado) {

        Auditoria auditoria = new Auditoria();

        auditoria.setUsuarioId(
            obtenerUsuarioId(jwt)
        );

        auditoria.setUsuarioNombre(
            obtenerNombre(jwt)
        );

        /*
         * No usamos directamente el subject porque,
         * para los choferes, ahora contiene su DNI.
         */
        auditoria.setUsuarioCorreo(
            obtenerCorreo(jwt)
        );

        auditoria.setUsuarioRol(
            jwt != null
                ? jwt.getClaimAsString("rol")
                : null
        );

        auditoria.setAccion(
            limpiar(accion, "DESCONOCIDA")
        );

        auditoria.setModulo(
            limpiar(modulo, "SISTEMA")
        );

        auditoria.setEntidad(
            limpiarOpcional(entidad)
        );

        auditoria.setEntidadId(
            limpiarOpcional(entidadId)
        );

        auditoria.setCodigoReferencia(
            limitar(
                limpiarOpcional(codigoReferencia),
                100
            )
        );

        auditoria.setDetalle(
            limitar(
                limpiarOpcional(detalle),
                1000
            )
        );

        auditoria.setValorAnterior(
            serializar(valorAnterior)
        );

        auditoria.setValorNuevo(
            serializar(valorNuevo)
        );

        auditoria.setResultado(
            resultado != null
                ? resultado
                : ResultadoAuditoria.EXITOSO
        );

        auditoria.setDireccionIp(
            obtenerDireccionIp(solicitudHttp)
        );

        auditoria.setUserAgent(
            obtenerUserAgent(solicitudHttp)
        );

        auditoriaRepository.save(auditoria);
    }

    /*
     * Registra eventos de autenticación incluso cuando
     * todavía no existe un JWT:
     *
     * - Inicio de sesión exitoso.
     * - Credenciales incorrectas.
     * - Cuenta desactivada.
     * - Cuenta bloqueada.
     * - Intento con usuario inexistente.
     */
    @Transactional(
        propagation = Propagation.REQUIRES_NEW
    )
    public void registrarAutenticacion(
            Usuario usuario,
            String identificador,
            HttpServletRequest solicitudHttp,
            String accion,
            String detalle,
            ResultadoAuditoria resultado) {

        Auditoria auditoria = new Auditoria();

        if (usuario != null) {
            auditoria.setUsuarioId(
                usuario.getId()
            );

            auditoria.setUsuarioNombre(
                obtenerNombreCompleto(usuario)
            );

            auditoria.setUsuarioCorreo(
                usuario.getCorreo()
            );

            auditoria.setUsuarioRol(
                usuario.getRol() != null
                    ? usuario.getRol().name()
                    : null
            );

            auditoria.setEntidadId(
                usuario.getId() != null
                    ? usuario.getId().toString()
                    : null
            );

            auditoria.setCodigoReferencia(
                limitar(
                    obtenerIdentificadorUsuario(
                        usuario
                    ),
                    100
                )
            );
        } else {
            auditoria.setUsuarioNombre(
                "Usuario no identificado"
            );

            String identificadorLimpio =
                limpiarOpcional(identificador);

            /*
             * Si el identificador desconocido tiene formato
             * de correo, se almacena como correo.
             * Nunca se almacena la contraseña.
             */
            if (
                identificadorLimpio != null
                && identificadorLimpio.contains("@")
            ) {
                auditoria.setUsuarioCorreo(
                    limitar(
                        identificadorLimpio
                            .toLowerCase(),
                        150
                    )
                );
            }

            auditoria.setCodigoReferencia(
                limitar(
                    identificadorLimpio,
                    100
                )
            );
        }

        auditoria.setAccion(
            limpiar(accion, "INICIAR_SESION")
        );

        auditoria.setModulo(
            "AUTENTICACION"
        );

        auditoria.setEntidad(
            "Usuario"
        );

        auditoria.setDetalle(
            limitar(
                limpiarOpcional(detalle),
                1000
            )
        );

        auditoria.setResultado(
            resultado != null
                ? resultado
                : ResultadoAuditoria.RECHAZADO
        );

        auditoria.setDireccionIp(
            obtenerDireccionIp(solicitudHttp)
        );

        auditoria.setUserAgent(
            obtenerUserAgent(solicitudHttp)
        );

        auditoriaRepository.save(auditoria);
    }

    public Map<String, Object> crearResumenUsuario(
            Long id,
            String dni,
            String nombres,
            String apellidos,
            String correo,
            String cargo,
            Boolean activo,
            Object fechaIngreso,
            Object fechaCese,
            String motivoDesactivacion) {

        Map<String, Object> resumen =
            new LinkedHashMap<>();

        resumen.put("id", id);
        resumen.put("dni", dni);
        resumen.put("nombres", nombres);
        resumen.put("apellidos", apellidos);
        resumen.put("correo", correo);
        resumen.put("cargo", cargo);
        resumen.put("activo", activo);

        resumen.put(
            "fechaIngreso",
            fechaIngreso
        );

        resumen.put(
            "fechaCese",
            fechaCese
        );

        resumen.put(
            "motivoDesactivacion",
            motivoDesactivacion
        );

        return resumen;
    }

    private Long obtenerUsuarioId(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        Object claim = jwt.getClaim("usuarioId");

        if (claim instanceof Number numero) {
            return numero.longValue();
        }

        if (claim != null) {
            try {
                return Long.valueOf(
                    claim.toString()
                );
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private String obtenerNombre(Jwt jwt) {
        if (jwt == null) {
            return "Usuario no identificado";
        }

        String nombre =
            jwt.getClaimAsString("nombre");

        if (
            nombre == null
            || nombre.isBlank()
        ) {
            String subject = jwt.getSubject();

            return subject != null
                && !subject.isBlank()
                    ? subject.trim()
                    : "Usuario no identificado";
        }

        return nombre.trim();
    }

    private String obtenerCorreo(Jwt jwt) {
        if (jwt == null) {
            return null;
        }

        String correo =
            jwt.getClaimAsString("correo");

        if (
            correo != null
            && !correo.isBlank()
        ) {
            return limitar(
                correo.trim().toLowerCase(),
                150
            );
        }

        /*
         * Compatibilidad con tokens anteriores, cuyo
         * subject siempre contenía el correo.
         */
        String subject = jwt.getSubject();

        if (
            subject != null
            && subject.contains("@")
        ) {
            return limitar(
                subject.trim().toLowerCase(),
                150
            );
        }

        return null;
    }

    private String obtenerNombreCompleto(
            Usuario usuario) {

        if (usuario == null) {
            return "Usuario no identificado";
        }

        String nombres =
            limpiarOpcional(usuario.getNombres());

        String apellidos =
            limpiarOpcional(usuario.getApellidos());

        if (
            nombres != null
            && apellidos != null
        ) {
            return limitar(
                nombres + " " + apellidos,
                240
            );
        }

        if (nombres != null) {
            return limitar(nombres, 240);
        }

        if (usuario.getCorreo() != null) {
            return limitar(
                usuario.getCorreo(),
                240
            );
        }

        if (usuario.getDni() != null) {
            return limitar(
                usuario.getDni(),
                240
            );
        }

        return "Usuario no identificado";
    }

    private String obtenerIdentificadorUsuario(
            Usuario usuario) {

        if (usuario == null) {
            return null;
        }

        if (
            usuario.getCorreo() != null
            && !usuario.getCorreo().isBlank()
        ) {
            return usuario
                .getCorreo()
                .trim()
                .toLowerCase();
        }

        if (
            usuario.getDni() != null
            && !usuario.getDni().isBlank()
        ) {
            return usuario.getDni().trim();
        }

        return usuario.getId() != null
            ? usuario.getId().toString()
            : null;
    }

    private String obtenerDireccionIp(
            HttpServletRequest solicitud) {

        if (solicitud == null) {
            return null;
        }

        String reenviada =
            solicitud.getHeader(
                "X-Forwarded-For"
            );

        if (
            reenviada != null
            && !reenviada.isBlank()
        ) {
            String primeraIp =
                reenviada.split(",")[0].trim();

            return limitar(primeraIp, 45);
        }

        String ipReal =
            solicitud.getHeader("X-Real-IP");

        if (
            ipReal != null
            && !ipReal.isBlank()
        ) {
            return limitar(
                ipReal.trim(),
                45
            );
        }

        return limitar(
            solicitud.getRemoteAddr(),
            45
        );
    }

    private String obtenerUserAgent(
            HttpServletRequest solicitud) {

        if (solicitud == null) {
            return null;
        }

        return limitar(
            solicitud.getHeader("User-Agent"),
            500
        );
    }

    private String serializar(Object valor) {
        if (valor == null) {
            return null;
        }

        if (valor instanceof Map<?, ?> mapa) {
            StringBuilder json =
                new StringBuilder("{");

            boolean primero = true;

            for (
                Map.Entry<?, ?> entrada
                : mapa.entrySet()
            ) {
                if (!primero) {
                    json.append(",");
                }

                primero = false;

                json.append("\"")
                    .append(
                        escaparJson(
                            String.valueOf(
                                entrada.getKey()
                            )
                        )
                    )
                    .append("\":")
                    .append(
                        convertirValorJson(
                            entrada.getValue()
                        )
                    );
            }

            json.append("}");

            return json.toString();
        }

        return convertirValorJson(valor);
    }

    private String convertirValorJson(
            Object valor) {

        if (valor == null) {
            return "null";
        }

        if (
            valor instanceof Number
            || valor instanceof Boolean
        ) {
            return valor.toString();
        }

        return "\""
            + escaparJson(valor.toString())
            + "\"";
    }

    private String escaparJson(
            String valor) {

        return valor
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t");
    }

    private String limpiar(
            String valor,
            String predeterminado) {

        String limpio =
            limpiarOpcional(valor);

        return limpio != null
            ? limpio
            : predeterminado;
    }

    private String limpiarOpcional(
            String valor) {

        if (
            valor == null
            || valor.isBlank()
        ) {
            return null;
        }

        return valor.trim();
    }

    private String limitar(
            String valor,
            int longitud) {

        if (valor == null) {
            return null;
        }

        if (valor.length() <= longitud) {
            return valor;
        }

        return valor.substring(0, longitud);
    }
}