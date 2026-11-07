package com.palmas.transporte.modulos.identidad.application;
import com.palmas.transporte.modulos.auditoria.application.AuditoriaService;
import com.palmas.transporte.modulos.mantenimiento.domain.Mantenimiento;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.palmas.transporte.modulos.identidad.api.DesactivarPersonalRequest;
import com.palmas.transporte.modulos.identidad.api.PersonalAdministrativoActualizarRequest;
import com.palmas.transporte.modulos.identidad.api.PersonalAdministrativoRequest;
import com.palmas.transporte.modulos.identidad.api.PersonalAdministrativoResponse;
import com.palmas.transporte.modulos.identidad.api.ReactivarPersonalRequest;
import com.palmas.transporte.modulos.auditoria.domain.ResultadoAuditoria;
import com.palmas.transporte.modulos.identidad.domain.Rol;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class PersonalAdministrativoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public PersonalAdministrativoService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            AuditoriaService auditoriaService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<PersonalAdministrativoResponse> listar(Jwt jwt) {
        Rol actor = obtenerRol(jwt);
        return usuarioRepository
            .findByCargoAdministrativoIsNotNull(
                Sort.by(
                    Sort.Direction.DESC,
                    "id"
                )
            )
            .stream()
            .filter(usuario -> puedeGestionar(actor, usuario.getRol()))
            .map(this::convertirRespuesta)
            .toList();
    }

    @Transactional(readOnly = true)
    public PersonalAdministrativoResponse buscarPorId(
            Long id,
            Jwt jwt) {

        Usuario usuario = buscarEntidad(id);
        validarGestion(obtenerRol(jwt), usuario.getRol());

        return convertirRespuesta(
            usuario
        );
    }

    @Transactional
    public PersonalAdministrativoResponse registrar(
            PersonalAdministrativoRequest request,
            Jwt jwt,
            HttpServletRequest solicitudHttp) {

        Rol actor = obtenerRol(jwt);
        validarGestion(actor, request.rol());

        String dni = normalizarDni(request.dni());

        String correo = normalizarCorreo(
            request.correo()
        );

        validarDniDisponible(dni, null);
        validarCorreoDisponible(correo, null);

        Usuario usuario = new Usuario();
        usuario.setCambioContrasenaRequerido(true);

        usuario.setDni(dni);

        usuario.setNombres(
            limpiar(request.nombres())
        );

        usuario.setApellidos(
            limpiar(request.apellidos())
        );

        usuario.setCorreo(correo);

        usuario.setContrasenaHash(
            passwordEncoder.encode(
                request.contrasena()
            )
        );

        usuario.setRol(request.rol());

        usuario.setCargoAdministrativo(
            request.cargo()
        );

        usuario.setFechaIngreso(
            request.fechaIngreso()
        );

        usuario.setFechaCese(null);
        usuario.setMotivoDesactivacion(null);
        usuario.setConductor(null);
        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);

        Usuario guardado =
            usuarioRepository.save(usuario);

        auditoriaService.registrar(
            jwt,
            solicitudHttp,
            "CREAR",
            "PERSONAL_ADMINISTRATIVO",
            "Usuario",
            guardado.getId().toString(),
            guardado.getDni(),
            "Se registró personal administrativo",
            null,
            crearResumen(guardado),
            ResultadoAuditoria.EXITOSO
        );

        return convertirRespuesta(guardado);
    }

    @Transactional
    public PersonalAdministrativoResponse actualizar(
            Long id,
            PersonalAdministrativoActualizarRequest request,
            Jwt jwt,
            HttpServletRequest solicitudHttp) {

        Usuario usuario = buscarEntidad(id);

        Rol actor = obtenerRol(jwt);
        validarGestion(actor, usuario.getRol());
        validarGestion(actor, request.rol());

        Map<String, Object> valorAnterior =
            crearResumen(usuario);

        String dni = normalizarDni(request.dni());

        String correo = normalizarCorreo(
            request.correo()
        );

        validarDniDisponible(dni, id);
        validarCorreoDisponible(correo, id);

        usuario.setDni(dni);

        usuario.setNombres(
            limpiar(request.nombres())
        );

        usuario.setApellidos(
            limpiar(request.apellidos())
        );

        usuario.setCorreo(correo);

        usuario.setCargoAdministrativo(
            request.cargo()
        );

        if (usuario.getRol() != request.rol()) {
            usuario.setRol(request.rol());
            usuario.setVersionSeguridad(
                (usuario.getVersionSeguridad() == null
                    ? 0L
                    : usuario.getVersionSeguridad()) + 1L
            );
        }

        usuario.setFechaIngreso(
            request.fechaIngreso()
        );

        Usuario guardado =
            usuarioRepository.save(usuario);

        auditoriaService.registrar(
            jwt,
            solicitudHttp,
            "ACTUALIZAR",
            "PERSONAL_ADMINISTRATIVO",
            "Usuario",
            guardado.getId().toString(),
            guardado.getDni(),
            "Se actualizaron los datos del personal",
            valorAnterior,
            crearResumen(guardado),
            ResultadoAuditoria.EXITOSO
        );

        return convertirRespuesta(guardado);
    }

    @Transactional
    public PersonalAdministrativoResponse desactivar(
            Long id,
            DesactivarPersonalRequest request,
            Long usuarioAutenticadoId,
            Jwt jwt,
            HttpServletRequest solicitudHttp) {

        Usuario usuario = buscarEntidad(id);

        validarGestion(obtenerRol(jwt), usuario.getRol());

        if (
            usuarioAutenticadoId != null
            && usuario.getId().equals(
                usuarioAutenticadoId
            )
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No puedes desactivar tu propia cuenta"
            );
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El personal ya se encuentra inactivo"
            );
        }

        validarFechaCese(
            usuario,
            request.fechaCese()
        );

        Map<String, Object> valorAnterior =
            crearResumen(usuario);

        usuario.setActivo(false);

        usuario.setFechaCese(
            request.fechaCese()
        );

        usuario.setMotivoDesactivacion(
            limpiar(request.motivo())
        );

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        revocarSesiones(usuario);

        Usuario guardado =
            usuarioRepository.save(usuario);

        auditoriaService.registrar(
            jwt,
            solicitudHttp,
            "DESACTIVAR",
            "PERSONAL_ADMINISTRATIVO",
            "Usuario",
            guardado.getId().toString(),
            guardado.getDni(),
            "Se desactivó el acceso del personal",
            valorAnterior,
            crearResumen(guardado),
            ResultadoAuditoria.EXITOSO
        );

        return convertirRespuesta(guardado);
    }

    @Transactional
    public PersonalAdministrativoResponse reactivar(
            Long id,
            ReactivarPersonalRequest request,
            Jwt jwt,
            HttpServletRequest solicitudHttp) {

        Usuario usuario = buscarEntidad(id);

        validarGestion(obtenerRol(jwt), usuario.getRol());

        if (Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El personal ya se encuentra activo"
            );
        }

        if (
            request.fechaReingreso()
                .isAfter(LocalDate.now())
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La fecha de reingreso no puede ser futura"
            );
        }

        Map<String, Object> valorAnterior =
            crearResumen(usuario);

        usuario.setFechaIngreso(
            request.fechaReingreso()
        );

        usuario.setFechaCese(null);
        usuario.setMotivoDesactivacion(null);
        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        revocarSesiones(usuario);

        Usuario guardado =
            usuarioRepository.save(usuario);

        auditoriaService.registrar(
            jwt,
            solicitudHttp,
            "REACTIVAR",
            "PERSONAL_ADMINISTRATIVO",
            "Usuario",
            guardado.getId().toString(),
            guardado.getDni(),
            "Se reactivó el acceso del personal",
            valorAnterior,
            crearResumen(guardado),
            ResultadoAuditoria.EXITOSO
        );

        return convertirRespuesta(guardado);
    }

    private Usuario buscarEntidad(Long id) {
        Usuario usuario = usuarioRepository
            .findById(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El personal no existe"
                )
            );

        if (
            usuario.getCargoAdministrativo()
                == null
        ) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "El usuario no pertenece al personal administrativo"
            );
        }

        return usuario;
    }

    private void validarDniDisponible(
            String dni,
            Long usuarioId) {

        boolean existe = usuarioId == null
            ? usuarioRepository.existsByDni(dni)
            : usuarioRepository
                .existsByDniAndIdNot(
                    dni,
                    usuarioId
                );

        if (existe) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe personal registrado con este DNI"
            );
        }
    }

    private void validarCorreoDisponible(
            String correo,
            Long usuarioId) {

        boolean existe = usuarioId == null
            ? usuarioRepository
                .existsByCorreoIgnoreCase(correo)
            : usuarioRepository
                .existsByCorreoIgnoreCaseAndIdNot(
                    correo,
                    usuarioId
                );

        if (existe) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe una cuenta registrada con este correo"
            );
        }
    }

    private void validarFechaCese(
            Usuario usuario,
            LocalDate fechaCese) {

        if (fechaCese.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La fecha de cese no puede ser futura"
            );
        }

        if (
            usuario.getFechaIngreso() != null
            && fechaCese.isBefore(
                usuario.getFechaIngreso()
            )
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La fecha de cese no puede ser anterior a la fecha de ingreso"
            );
        }
    }

    private Map<String, Object> crearResumen(
            Usuario usuario) {

        return auditoriaService
            .crearResumenUsuario(
                usuario.getId(),
                usuario.getDni(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getCargoAdministrativo()
                    != null
                        ? usuario
                            .getCargoAdministrativo()
                            .name()
                        : null,
                usuario.getActivo(),
                usuario.getFechaIngreso(),
                usuario.getFechaCese(),
                usuario.getMotivoDesactivacion()
            );
    }

    private PersonalAdministrativoResponse
        convertirRespuesta(Usuario usuario) {

        String nombres = usuario.getNombres();

        String apellidos =
            usuario.getApellidos() != null
                ? usuario.getApellidos()
                : "";

        String nombreCompleto =
            (nombres + " " + apellidos).trim();

        String cargo =
            usuario.getCargoAdministrativo() != null
                ? usuario
                    .getCargoAdministrativo()
                    .name()
                : null;

        String rol =
            usuario.getRol() != null
                ? usuario.getRol().name()
                : null;

        return new PersonalAdministrativoResponse(
            usuario.getId(),
            usuario.getDni(),
            nombres,
            usuario.getApellidos(),
            nombreCompleto,
            usuario.getCorreo(),
            cargo,
            rol,
            usuario.getActivo(),
            usuario.getFechaIngreso(),
            usuario.getFechaCese(),
            usuario.getMotivoDesactivacion(),
            usuario.getFechaCreacion(),
            usuario.getFechaActualizacion(),
            usuario.getFechaPrimerLogin(),
            usuario.getFechaUltimoLogin(),
            usuario.getFechaUltimaConexion(),
            usuario.getUltimaDireccionIp(),
            usuario.getUltimoUserAgent(),
            obtenerProcesosPermitidos(
                usuario.getRol()
            )
        );
    }

    private List<String> obtenerProcesosPermitidos(
            Rol rol) {

        if (rol == null) {
            return List.of();
        }

        if (
            rol == Rol.MASTER
            || rol == Rol.COORDINADOR
            || rol == Rol.ADMINISTRADOR
        ) {
            return List.of(
                "Dashboard: consultar indicadores generales",
                "Vehículos: consultar, registrar, editar y desactivar",
                "Conductores: consultar, registrar, editar y desactivar",
                "Rutas: consultar, registrar, editar y desactivar",
                "Solicitudes: consultar, registrar, aprobar, programar y cancelar",
                "Servicios: consultar y actualizar estados",
                "Flota: consultar disponibilidad y operación",
                "Mantenimiento: consultar y gestionar todo el ciclo",
                "Evidencias: consultar, registrar y descargar",
                "Encuestas: consultar y registrar",
                "Reportes: consultar e imprimir",
                "Personal administrativo: crear, editar, desactivar y reactivar",
                "Auditoría: consultar actividades del sistema"
            );
        }

        if (rol == Rol.OPERADOR) {
            return List.of(
                "Dashboard: consultar indicadores generales",
                "Vehículos: consultar, registrar, editar y desactivar",
                "Conductores: consultar, registrar, editar y desactivar",
                "Rutas: consultar, registrar, editar y desactivar",
                "Solicitudes: consultar, registrar, aprobar, programar y cancelar",
                "Servicios: consultar y actualizar estados",
                "Flota: consultar disponibilidad y operación",
                "Mantenimiento: consultar y gestionar todo el ciclo",
                "Evidencias: consultar, registrar y descargar",
                "Encuestas: consultar y registrar",
                "Reportes: consultar e imprimir"
            );
        }

        if (rol == Rol.CONSULTA) {
            return List.of(
                "Dashboard: consultar indicadores generales",
                "Vehículos: solo consulta",
                "Conductores: solo consulta",
                "Rutas: solo consulta",
                "Solicitudes: solo consulta",
                "Servicios: solo consulta",
                "Flota: solo consulta",
                "Mantenimiento: solo consulta",
                "Evidencias: consultar y descargar",
                "Encuestas: consultar y registrar",
                "Reportes: consultar e imprimir"
            );
        }

        if (rol == Rol.CHOFER) {
            return List.of(
                "Servicios asignados: consultar",
                "Traslados: registrar salida y llegada",
                "Traslados: cancelar servicio en curso",
                "Evidencias digitales: consultar, registrar y descargar"
            );
        }

        return List.of();
    }

    private Rol obtenerRol(Jwt jwt) {
        try {
            return Rol.valueOf(jwt.getClaimAsString("rol"));
        } catch (Exception exception) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El perfil autenticado no puede administrar cuentas"
            );
        }
    }

    private boolean puedeGestionar(Rol actor, Rol destino) {
        if (actor == Rol.MASTER || actor == Rol.ADMINISTRADOR) {
            return destino == Rol.COORDINADOR
                || destino == Rol.OPERADOR
                || destino == Rol.CONSULTA;
        }

        if (actor == Rol.COORDINADOR) {
            return destino == Rol.OPERADOR
                || destino == Rol.CONSULTA;
        }

        return false;
    }

    private void validarGestion(Rol actor, Rol destino) {
        if (destino == null || !puedeGestionar(actor, destino)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes permiso para administrar este perfil"
            );
        }
    }

    private void revocarSesiones(Usuario usuario) {
        usuario.setVersionSeguridad(
            (usuario.getVersionSeguridad() == null
                ? 0L
                : usuario.getVersionSeguridad()) + 1L
        );
    }

    private String normalizarDni(String dni) {
        return dni.trim();
    }

    private String normalizarCorreo(
            String correo) {

        return correo
            .trim()
            .toLowerCase();
    }

    private String limpiar(String valor) {
        return valor
            .trim()
            .replaceAll("\\s+", " ");
    }
}
