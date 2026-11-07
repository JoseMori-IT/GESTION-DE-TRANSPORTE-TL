package com.palmas.transporte.modulos.mantenimiento.application;
import com.palmas.transporte.modulos.flota.application.VehiculoService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.palmas.transporte.modulos.mantenimiento.api.CerrarMantenimientoRequest;
import com.palmas.transporte.modulos.mantenimiento.api.EvaluarMantenimientoRequest;
import com.palmas.transporte.modulos.mantenimiento.api.MantenimientoRequest;
import com.palmas.transporte.modulos.mantenimiento.api.MantenimientoResponse;
import com.palmas.transporte.modulos.mantenimiento.api.RepararMantenimientoRequest;
import com.palmas.transporte.modulos.mantenimiento.domain.EstadoMantenimiento;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.mantenimiento.domain.Mantenimiento;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.mantenimiento.persistence.MantenimientoRepository;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;

@Service
public class MantenimientoService {

    private static final List<EstadoMantenimiento>
        ESTADOS_ACTIVOS = List.of(
            EstadoMantenimiento.REPORTADO,
            EstadoMantenimiento.EN_EVALUACION,
            EstadoMantenimiento.EN_MANTENIMIENTO,
            EstadoMantenimiento.REPARADO
        );

    private final MantenimientoRepository
        mantenimientoRepository;

    private final UsuarioRepository usuarioRepository;

    private final VehiculoService vehiculoService;

    public MantenimientoService(
            MantenimientoRepository mantenimientoRepository,
            UsuarioRepository usuarioRepository,
            VehiculoService vehiculoService) {

        this.mantenimientoRepository =
            mantenimientoRepository;

        this.usuarioRepository = usuarioRepository;
        this.vehiculoService = vehiculoService;
    }

    @Transactional(readOnly = true)
    public List<MantenimientoResponse> listar() {
        return mantenimientoRepository
            .findAllByOrderByFechaReporteDesc()
            .stream()
            .map(this::convertirRespuesta)
            .toList();
    }

    @Transactional(readOnly = true)
    public MantenimientoResponse buscarPorId(Long id) {
        return convertirRespuesta(buscarEntidad(id));
    }

    @Transactional(readOnly = true)
    public List<MantenimientoResponse>
            listarPorVehiculo(Long vehiculoId) {

        vehiculoService.buscarPorId(vehiculoId);

        return mantenimientoRepository
            .findByVehiculoIdOrderByFechaReporteDesc(
                vehiculoId
            )
            .stream()
            .map(this::convertirRespuesta)
            .toList();
    }

    @Transactional
    public MantenimientoResponse registrar(
            MantenimientoRequest request,
            String correoUsuario) {

        Vehiculo vehiculo =
            vehiculoService.bloquearPorId(
                request.vehiculoId()
            );

        Usuario usuario =
            buscarUsuario(correoUsuario);

        boolean mantenimientoActivo =
            mantenimientoRepository
                .existsByVehiculoIdAndEstadoIn(
                    vehiculo.getId(),
                    ESTADOS_ACTIVOS
                );

        if (mantenimientoActivo) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El vehículo ya tiene un mantenimiento activo"
            );
        }

        /*
         * Este método también valida que el vehículo
         * no tenga un servicio programado o en curso.
         */
        vehiculoService.cambiarEstado(
            vehiculo.getId(),
            EstadoVehiculo.MANTENIMIENTO
        );

        Mantenimiento mantenimiento =
            new Mantenimiento();

        mantenimiento.setCodigo(generarCodigo());
        mantenimiento.setVehiculo(vehiculo);
        mantenimiento.setReportadoPor(usuario);
        mantenimiento.setTipoFalla(
            request.tipoFalla()
        );
        mantenimiento.setPrioridad(
            request.prioridad()
        );
        mantenimiento.setTitulo(
            request.titulo().trim()
        );
        mantenimiento.setDescripcion(
            request.descripcion().trim()
        );
        mantenimiento.setKilometrajeReporte(
            request.kilometrajeReporte()
        );
        mantenimiento.setEstado(
            EstadoMantenimiento.REPORTADO
        );

        return convertirRespuesta(
            mantenimientoRepository.save(mantenimiento)
        );
    }

    @Transactional
    public MantenimientoResponse evaluar(
            Long id,
            EvaluarMantenimientoRequest request) {

        Mantenimiento mantenimiento =
            buscarEntidad(id);

        validarEstado(
            mantenimiento,
            EstadoMantenimiento.REPORTADO,
            "Solo se puede evaluar un reporte pendiente"
        );

        mantenimiento.setDiagnostico(
            request.diagnostico().trim()
        );
        mantenimiento.setTallerProveedor(
            textoOpcional(request.tallerProveedor())
        );
        if (request.prioridad() != null) {
            mantenimiento.setPrioridad(request.prioridad());
        }
        mantenimiento.setEstado(
            EstadoMantenimiento.EN_EVALUACION
        );

        return convertirRespuesta(
            mantenimientoRepository.save(mantenimiento)
        );
    }

    @Transactional
    public MantenimientoResponse iniciar(Long id) {
        Mantenimiento mantenimiento =
            buscarEntidad(id);

        validarEstado(
            mantenimiento,
            EstadoMantenimiento.EN_EVALUACION,
            "El mantenimiento debe estar evaluado antes de iniciarse"
        );

        mantenimiento.setEstado(
            EstadoMantenimiento.EN_MANTENIMIENTO
        );
        mantenimiento.setFechaInicioAtencion(
            LocalDateTime.now()
        );

        return convertirRespuesta(
            mantenimientoRepository.save(mantenimiento)
        );
    }

    @Transactional
    public MantenimientoResponse reparar(
            Long id,
            RepararMantenimientoRequest request) {

        Mantenimiento mantenimiento =
            buscarEntidad(id);

        validarEstado(
            mantenimiento,
            EstadoMantenimiento.EN_MANTENIMIENTO,
            "Solo se puede reparar un mantenimiento en ejecución"
        );

        mantenimiento.setTrabajoRealizado(
            request.trabajoRealizado().trim()
        );
        mantenimiento.setCosto(request.costo());
        mantenimiento.setFechaReparacion(
            LocalDateTime.now()
        );
        mantenimiento.setEstado(
            EstadoMantenimiento.REPARADO
        );

        return convertirRespuesta(
            mantenimientoRepository.save(mantenimiento)
        );
    }

    @Transactional
    public MantenimientoResponse cerrar(
            Long id,
            CerrarMantenimientoRequest request) {

        Mantenimiento mantenimiento =
            buscarEntidad(id);

        validarEstado(
            mantenimiento,
            EstadoMantenimiento.REPARADO,
            "El mantenimiento debe estar reparado antes de cerrarse"
        );

        mantenimiento.setObservacionesCierre(
            textoOpcional(
                request.observacionesCierre()
            )
        );
        mantenimiento.setFechaCierre(
            LocalDateTime.now()
        );
        mantenimiento.setEstado(
            EstadoMantenimiento.CERRADO
        );

        Mantenimiento guardado =
            mantenimientoRepository.save(mantenimiento);

        vehiculoService.cambiarEstado(
            mantenimiento.getVehiculo().getId(),
            EstadoVehiculo.DISPONIBLE
        );

        return convertirRespuesta(guardado);
    }

    @Transactional
    public MantenimientoResponse cancelar(Long id) {
        Mantenimiento mantenimiento =
            buscarEntidad(id);

        if (
            mantenimiento.getEstado()
                != EstadoMantenimiento.REPORTADO
            && mantenimiento.getEstado()
                != EstadoMantenimiento.EN_EVALUACION
        ) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Solo se puede cancelar un mantenimiento pendiente o en evaluación"
            );
        }

        mantenimiento.setEstado(
            EstadoMantenimiento.CANCELADO
        );
        mantenimiento.setFechaCierre(
            LocalDateTime.now()
        );

        Mantenimiento guardado =
            mantenimientoRepository.save(mantenimiento);

        vehiculoService.cambiarEstado(
            mantenimiento.getVehiculo().getId(),
            EstadoVehiculo.DISPONIBLE
        );

        return convertirRespuesta(guardado);
    }

    private Mantenimiento buscarEntidad(Long id) {
        return mantenimientoRepository.findById(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Mantenimiento no encontrado"
                )
            );
    }

    private Usuario buscarUsuario(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "No se pudo identificar al usuario"
            );
        }

        return usuarioRepository
            .findByCorreoIgnoreCase(correo)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "El usuario autenticado no existe"
                )
            );
    }

    private void validarEstado(
            Mantenimiento mantenimiento,
            EstadoMantenimiento esperado,
            String mensaje) {

        if (mantenimiento.getEstado() != esperado) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                mensaje
            );
        }
    }

    private String generarCodigo() {
        String fecha = LocalDate.now().format(
            DateTimeFormatter.BASIC_ISO_DATE
        );

        String codigo;

        do {
            String identificador = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();

            codigo = "MAN-" + fecha + "-"
                + identificador;

        } while (
            mantenimientoRepository
                .existsByCodigoIgnoreCase(codigo)
        );

        return codigo;
    }

    private String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private MantenimientoResponse convertirRespuesta(
            Mantenimiento mantenimiento) {

        Vehiculo vehiculo =
            mantenimiento.getVehiculo();

        Usuario usuario =
            mantenimiento.getReportadoPor();

        return new MantenimientoResponse(
            mantenimiento.getId(),
            mantenimiento.getCodigo(),

            vehiculo.getId(),
            vehiculo.getCodigoInterno(),
            vehiculo.getPlaca(),
            vehiculo.getMarca() + " "
                + vehiculo.getModelo(),

            usuario.getId(),
            usuario.getNombres(),

            mantenimiento.getTipoFalla(),
            mantenimiento.getPrioridad(),
            mantenimiento.getEstado(),

            mantenimiento.getTitulo(),
            mantenimiento.getDescripcion(),
            mantenimiento.getKilometrajeReporte(),

            mantenimiento.getDiagnostico(),
            mantenimiento.getTrabajoRealizado(),
            mantenimiento.getTallerProveedor(),
            mantenimiento.getCosto(),

            mantenimiento.getFechaReporte(),
            mantenimiento.getFechaInicioAtencion(),
            mantenimiento.getFechaReparacion(),
            mantenimiento.getFechaCierre(),
            mantenimiento.getFechaActualizacion(),

            mantenimiento.getObservacionesCierre()
        );
    }
}
