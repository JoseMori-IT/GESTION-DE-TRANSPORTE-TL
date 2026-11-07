package com.palmas.transporte.modulos.identidad.application;


import java.util.List;
import java.util.Locale;
import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.oauth2.jwt.Jwt;

import com.palmas.transporte.modulos.identidad.api.UsuarioRequest;
import com.palmas.transporte.modulos.identidad.api.UsuarioResponse;
import com.palmas.transporte.modulos.identidad.api.ContrasenaTemporalResponse;
import com.palmas.transporte.modulos.identidad.domain.Conductor;
import com.palmas.transporte.modulos.identidad.domain.EstadoConductor;
import com.palmas.transporte.modulos.identidad.domain.Rol;
import com.palmas.transporte.modulos.identidad.domain.RolTripulacion;
import com.palmas.transporte.modulos.identidad.domain.TipoDocumento;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.identidad.persistence.ConductorRepository;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;

@Service
public class UsuarioService {

    private static final String CARACTERES_TEMPORALES =
        "ABCDEFGHJKLMNPQRSTUVWXYZ"
        + "abcdefghijkmnopqrstuvwxyz"
        + "23456789";

    private final SecureRandom random =
        new SecureRandom();

    private final UsuarioRepository
        usuarioRepository;

    private final ConductorRepository
        conductorRepository;

    private final PasswordEncoder
        passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            ConductorRepository conductorRepository,
            PasswordEncoder passwordEncoder) {

        this.usuarioRepository =
            usuarioRepository;

        this.conductorRepository =
            conductorRepository;

        this.passwordEncoder =
            passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Jwt jwt) {
        String rolActor = jwt.getClaimAsString("rol");
        return usuarioRepository
            .findAll(
                Sort.by(
                    Sort.Direction.DESC,
                    "id"
                )
            )
            .stream()
            .filter(usuario -> usuario.getRol() != Rol.MASTER)
            .filter(usuario -> usuario.getRol() != Rol.ADMINISTRADOR)
            .filter(usuario -> !"COORDINADOR".equals(rolActor)
                || usuario.getRol() != Rol.COORDINADOR)
            .map(this::convertirRespuesta)
            .toList();
    }

    @Transactional
    public UsuarioResponse registrar(
            UsuarioRequest request) {

        if (request.rol() != Rol.CHOFER) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Las cuentas administrativas se crean desde Personal administrativo"
            );
        }

        Conductor conductor =
            validarConductor(
                request.rol(),
                request.conductorId()
            );

        String correo =
            normalizarCorreo(
                request.correo()
            );

        if (
            correo != null
            && usuarioRepository
                .existsByCorreoIgnoreCase(
                    correo
                )
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe una cuenta registrada "
                    + "con este correo"
            );
        }

        String dni =
            conductor != null
                ? obtenerDniConductor(conductor)
                : null;

        if (
            dni != null
            && usuarioRepository
                .existsByDni(dni)
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe una cuenta registrada "
                    + "con este DNI"
            );
        }

        Usuario usuario = new Usuario();
        usuario.setCambioContrasenaRequerido(true);

        usuario.setNombres(
            request.nombres().trim()
        );

        usuario.setDni(dni);

        /*
         * Las cuentas de chofer utilizan el DNI.
         * El correo queda vacío.
         */
        usuario.setCorreo(
            request.rol() == Rol.CHOFER
                ? null
                : correo
        );

        usuario.setContrasenaHash(
            passwordEncoder.encode(
                request.contrasena()
            )
        );

        usuario.setRol(
            request.rol()
        );

        usuario.setConductor(
            conductor
        );

        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);

        Usuario guardado =
            usuarioRepository.save(usuario);

        return convertirRespuesta(
            guardado
        );
    }

    @Transactional
    public ContrasenaTemporalResponse
        crearAccesoTemporalChofer(
            Long conductorId
        ) {

        Conductor conductor = validarConductor(
            Rol.CHOFER,
            conductorId
        );

        String dni = obtenerDniConductor(conductor);

        usuarioRepository.findByDni(dni).ifPresent(existente -> {
            String mensaje = existente.getRol() == Rol.CHOFER
                ? "Ya existe una cuenta de chofer con ese DNI. Consulta su acceso y utiliza Restablecer contraseña si corresponde."
                : "El DNI ya pertenece a una cuenta administrativa. No se creó otra cuenta ni se modificaron sus permisos.";
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
        });

        String temporal = generarContrasenaTemporal();
        LocalDateTime vence =
            LocalDateTime.now().plusHours(24);

        Usuario usuario = new Usuario();
        usuario.setNombres(conductor.getNombres());
        usuario.setApellidos(conductor.getApellidos());
        usuario.setDni(dni);
        usuario.setCorreo(null);
        usuario.setContrasenaHash(
            passwordEncoder.encode(temporal)
        );
        usuario.setRol(Rol.CHOFER);
        usuario.setConductor(conductor);
        usuario.setActivo(true);
        usuario.setCambioContrasenaRequerido(true);
        usuario.setContrasenaTemporalHasta(vence);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);

        Usuario guardado =
            usuarioRepository.saveAndFlush(usuario);

        return new ContrasenaTemporalResponse(
            guardado.getId(),
            dni,
            temporal,
            vence
        );
    }

    private String generarContrasenaTemporal() {
        StringBuilder resultado =
            new StringBuilder(20);

        for (int indice = 0; indice < 20; indice++) {
            resultado.append(
                CARACTERES_TEMPORALES.charAt(
                    random.nextInt(
                        CARACTERES_TEMPORALES.length()
                    )
                )
            );
        }

        return resultado.toString();
    }

    private Conductor validarConductor(
            Rol rol,
            Long conductorId) {

        if (rol != Rol.CHOFER) {
            if (conductorId != null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo una cuenta CHOFER puede "
                        + "vincularse con un conductor"
                );
            }

            return null;
        }

        if (conductorId == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Selecciona el conductor "
                    + "correspondiente a la cuenta"
            );
        }

        if (
            usuarioRepository
                .existsByConductorId(
                    conductorId
                )
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El conductor seleccionado ya tiene "
                    + "una cuenta de usuario"
            );
        }

        Conductor conductor =
            conductorRepository
                .findById(conductorId)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El conductor seleccionado "
                            + "no existe"
                    )
                );

        if (
            conductor.getRolTripulacion()
                != RolTripulacion.CHOFER
            && conductor.getRolTripulacion()
                != RolTripulacion.AMBOS
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La persona seleccionada no está "
                    + "registrada como chofer"
            );
        }

        if (
            conductor.getEstado()
                == EstadoConductor.INACTIVO
            || conductor.getEstado()
                == EstadoConductor.SUSPENDIDO
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El conductor seleccionado está "
                    + "inactivo o suspendido"
            );
        }

        /*
         * Verifica el DNI antes de crear
         * la cuenta.
         */
        obtenerDniConductor(conductor);

        return conductor;
    }

    private String obtenerDniConductor(
            Conductor conductor) {

        String dni =
            conductor.getNumeroDocumento();

        if (
            dni == null
            || dni.isBlank()
        ) {
            dni = conductor.getDni();
        }

        if (
            conductor.getTipoDocumento() != null
            && conductor.getTipoDocumento()
                != TipoDocumento.DNI
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El acceso por DNI requiere que "
                    + "el conductor tenga documento DNI"
            );
        }

        if (
            dni == null
            || !dni.trim().matches("\\d{8}")
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El conductor debe tener un DNI "
                    + "válido de 8 dígitos"
            );
        }

        return dni.trim();
    }

    private String normalizarCorreo(
            String correo) {

        if (
            correo == null
            || correo.isBlank()
        ) {
            return null;
        }

        return correo
            .trim()
            .toLowerCase(
                Locale.ROOT
            );
    }

    private UsuarioResponse convertirRespuesta(
            Usuario usuario) {

        Conductor conductor =
            usuario.getConductor();

        Long conductorId =
            conductor != null
                ? conductor.getId()
                : null;

        String nombreConductor =
            conductor != null
                ? (
                    conductor.getNombres()
                    + " "
                    + conductor.getApellidos()
                ).trim()
                : null;

        return new UsuarioResponse(
            usuario.getId(),
            usuario.getNombres(),
            usuario.getCorreo(),
            usuario.getRol().name(),
            usuario.getActivo(),
            conductorId,
            nombreConductor,
            usuario.getDni()
        );
    }
}
