package com.palmas.transporte.modulos.operacion.application;
import com.palmas.transporte.modulos.operacion.api.PlanViajeRequest;
import com.palmas.transporte.modulos.operacion.api.RecorridoSolicitudRequest;
import com.palmas.transporte.modulos.operacion.domain.TipoSolicitudTransporte;
import com.palmas.transporte.modulos.recorridos.domain.EstadoRuta;
import com.palmas.transporte.modulos.recorridos.persistence.RutaRepository;
import com.palmas.transporte.modulos.recorridos.persistence.UbicacionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmas.transporte.modulos.operacion.api.ProgramarSolicitudRequest;
import com.palmas.transporte.modulos.operacion.api.ServicioRequest;
import com.palmas.transporte.modulos.operacion.api.SolicitudTransporteRequest;
import com.palmas.transporte.modulos.operacion.api.SolicitudTransporteResponse;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.EstadoSolicitud;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.domain.SolicitudTransporte;
import com.palmas.transporte.modulos.identidad.domain.Usuario;
import com.palmas.transporte.modulos.operacion.persistence.SolicitudTransporteRepository;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;

@Service
public class SolicitudTransporteService {
    private final com.palmas.transporte.modulos.configuracion.api.BaseOperativa baseOperativa;

    private final SolicitudTransporteRepository
        solicitudRepository;

    private final UsuarioRepository usuarioRepository;
    private final com.palmas.transporte.modulos.recorridos.persistence.RutaRepository rutaRepository;
    private final com.palmas.transporte.modulos.recorridos.persistence.UbicacionRepository ubicacionRepository;

    private final ServicioTransporteService
        servicioTransporteService;

    public SolicitudTransporteService(
        SolicitudTransporteRepository solicitudRepository,
        UsuarioRepository usuarioRepository,
        ServicioTransporteService servicioTransporteService,
        com.palmas.transporte.modulos.recorridos.persistence.RutaRepository rutaRepository,
        com.palmas.transporte.modulos.recorridos.persistence.UbicacionRepository ubicacionRepository,
        com.palmas.transporte.modulos.configuracion.api.BaseOperativa baseOperativa
    ) {
        this.rutaRepository = rutaRepository;
        this.baseOperativa=baseOperativa;
        this.ubicacionRepository=ubicacionRepository;
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioTransporteService =
            servicioTransporteService;
    }

    @Transactional(readOnly = true)
    public List<SolicitudTransporteResponse> listarTodas() {
        return solicitudRepository
            .findAllByOrderByIdDesc()
            .stream()
            .map(this::convertirRespuesta)
            .toList();
    }

    @Transactional(readOnly = true)
    public SolicitudTransporteResponse buscarPorId(Long id) {
        return convertirRespuesta(
            buscarEntidadPorId(id)
        );
    }

    @Transactional
    public SolicitudTransporteResponse registrar(
        SolicitudTransporteRequest request,
        String correoUsuario
    ) {
        if(request.fechaRequerida()==null || !request.fechaRequerida().isAfter(LocalDateTime.now()))throw new IllegalArgumentException("La fecha y hora requerida deben ser posteriores al momento actual.");
        Usuario usuario = usuarioRepository
            .findByCorreoIgnoreCase(correoUsuario)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "No se encontró el usuario autenticado."
                )
            );

        validarTipoCarga(request.tipoSolicitud(), request.productoTransportado());
        validarDescripcionProducto(request);

        SolicitudTransporte solicitud =
            new SolicitudTransporte();

        solicitud.setCodigo(generarCodigo());
        solicitud.setTipoSolicitud(
            request.tipoSolicitud()
        );
        solicitud.setSolicitanteNombre(
            request.solicitanteNombre()
        );
        solicitud.setAreaSolicitante(
            request.areaSolicitante()
        );
        solicitud.setTelefono(
            normalizarOpcional(request.telefono())
        );
        var ruta = rutaRepository.findById(java.util.Objects.requireNonNull(request.rutaId(), "Selecciona una ruta")).orElseThrow(() -> new IllegalArgumentException("La ruta no existe."));
        if (ruta.getEstado() != com.palmas.transporte.modulos.recorridos.domain.EstadoRuta.ACTIVA) throw new IllegalArgumentException("La ruta seleccionada está inactiva.");
        solicitud.setRuta(ruta);
        solicitud.setOrigen(ruta.getOrigen());
        solicitud.setDestino(ruta.getDestino());
        solicitud.setProductoTransportado(
            request.productoTransportado()
        );
        solicitud.setDescripcionProducto(
            normalizarOpcional(
                request.descripcionProducto()
            )
        );
        solicitud.setCantidadEstimadaToneladas(
            request.cantidadEstimadaToneladas()
        );
        solicitud.setFechaRequerida(
            request.fechaRequerida()
        );
        solicitud.setPrioridad(request.prioridad());
        solicitud.setEstado(
            EstadoSolicitud.SOLICITADA
        );
        solicitud.setObservaciones(
            normalizarOpcional(
                request.observaciones()
            )
        );
        solicitud.setUsuarioSolicitante(usuario);
        aplicarRecorrido(solicitud,request.planViaje());

        SolicitudTransporte guardada =
            solicitudRepository.save(solicitud);

        return convertirRespuesta(guardada);
    }

    @Transactional
    public SolicitudTransporteResponse cambiarEstado(
        Long id,
        EstadoSolicitud nuevoEstado,
        String motivo
    ) {
        SolicitudTransporte solicitud =
            buscarEntidadPorId(id);

        EstadoSolicitud estadoActual =
            solicitud.getEstado();

        if (estadoActual == nuevoEstado) {
            return convertirRespuesta(solicitud);
        }

        validarTransicion(
            estadoActual,
            nuevoEstado
        );

        String motivoNormalizado =
            normalizarOpcional(motivo);

        if (
            nuevoEstado == EstadoSolicitud.RECHAZADA
            || nuevoEstado == EstadoSolicitud.CANCELADA
        ) {
            if (motivoNormalizado == null) {
                throw new IllegalArgumentException(
                    "Debes indicar el motivo del rechazo "
                        + "o cancelación."
                );
            }

            solicitud.setMotivoRechazoCancelacion(
                motivoNormalizado
            );
        } else {
            solicitud.setMotivoRechazoCancelacion(null);
        }

        if(nuevoEstado==EstadoSolicitud.APROBADA && solicitud.getPlanViaje()==null) throw new IllegalArgumentException("Completa el recorrido de la solicitud antes de aprobarla.");
        solicitud.setEstado(nuevoEstado);

        return convertirRespuesta(
            solicitudRepository.save(solicitud)
        );
    }

    @Transactional
    public SolicitudTransporteResponse programar(
        Long id,
        ProgramarSolicitudRequest request
    ) {
        SolicitudTransporte solicitud =
            buscarEntidadPorId(id);

        if (
            solicitud.getEstado()
                != EstadoSolicitud.APROBADA
        ) {
            throw new IllegalArgumentException(
                "Solo se puede programar una solicitud aprobada."
            );
        }

        if (solicitud.getServicio() != null) {
            throw new IllegalArgumentException(
                "La solicitud ya tiene un servicio asignado."
            );
        }

        if (request.fechaHoraSalida() == null) throw new IllegalArgumentException("Indica la fecha y hora planificadas del viaje.");
        if (request.rutaId() == null) throw new IllegalArgumentException("Debes definir la ruta antes de programar el servicio.");
        if (solicitud.getRuta() != null && !solicitud.getRuta().getId().equals(request.rutaId())) throw new IllegalArgumentException("La ruta del servicio debe corresponder a la solicitud.");
        if (solicitud.getPlanViaje()==null) throw new IllegalArgumentException("Completa y aprueba el recorrido desde la solicitud antes de programar.");
        if (request.planViaje()!=null && !request.planViaje().equals(solicitud.getPlanViaje())) throw new IllegalArgumentException("El recorrido aprobado no puede modificarse al programar.");
        if(request.tramos()!=null && !request.tramos().isEmpty())throw new IllegalArgumentException("La programación utiliza las paradas aprobadas en la solicitud.");
        LocalDateTime fechaSalida =
            request.fechaHoraSalida() != null
                ? request.fechaHoraSalida()
                : solicitud.getFechaRequerida();

        if (
            fechaSalida != null
            && fechaSalida.isBefore(LocalDateTime.now())
        ) {
            throw new IllegalArgumentException(
                "La fecha prevista ya se encuentra en el pasado. "
                    + "Selecciona una nueva fecha."
            );
        }

        if (solicitud.getFechaRequerida() != null && fechaSalida.isBefore(solicitud.getFechaRequerida())) {
            throw new IllegalArgumentException("La salida no puede ser anterior a la fecha y hora requerida en la solicitud.");
        }

        String observaciones =
            construirObservaciones(
                solicitud,
                request.observaciones()
            );

        String descripcionResiduos =
            "RESIDUOS_VARIOS".equals(
                solicitud.getProductoTransportado()
            )
                ? solicitud.getDescripcionProducto()
                : null;

        String codigoServicio = generarCodigoServicio(
    solicitud.getCodigo()
);

ServicioRequest servicioRequest = new ServicioRequest(
    codigoServicio,
    request.vehiculoId(),
    request.conductorId(),
    request.rutaId(),
    solicitud.getOrigen(),
    solicitud.getDestino(),
    true,
    fechaSalida,
    solicitud.getProductoTransportado(),
    EstadoServicio.PROGRAMADO,
    request.observaciones(),
    request.copilotoId(),
    solicitud.getDescripcionProducto(),
    request.tramos(),
    solicitud.getPlanViaje()
);

        ServicioTransporte servicio =
            servicioTransporteService.registrar(
                servicioRequest
            );

        solicitud.setServicio(servicio);
        solicitud.setRuta(servicio.getRuta());
        solicitud.setEstado(
            EstadoSolicitud.PROGRAMADA
        );
        solicitud.setMotivoRechazoCancelacion(null);

        return convertirRespuesta(
            solicitudRepository.save(solicitud)
        );
    }

    private void aplicarRecorrido(SolicitudTransporte solicitud,com.palmas.transporte.modulos.operacion.api.PlanViajeRequest plan) {
        if(plan==null || plan.estimaciones()==null)throw new IllegalArgumentException("Define las paradas y calcula o completa los kilómetros de todos los tramos desde la solicitud.");
        baseOperativa.validarPlanta(plan.inicioUbicacionId(),plan.plantaRetornoId());
        var ruta=solicitud.getRuta();
        if(ruta==null || ruta.getOrigenUbicacion()==null || ruta.getDestinoUbicacion()==null)throw new IllegalArgumentException("La ruta debe tener ubicaciones registradas.");
        boolean saleDePlanta=ItinerarioViaje.esBase(ruta.getOrigenUbicacion());
        var base=saleDePlanta?ruta.getOrigenUbicacion():ruta.getDestinoUbicacion();
        var destino=saleDePlanta?ruta.getDestinoUbicacion():ruta.getOrigenUbicacion();
        if(!java.util.Objects.equals(plan.inicioUbicacionId(),base.getId()) || !java.util.Objects.equals(plan.plantaRetornoId(),base.getId()) || plan.paradas()==null || plan.paradas().isEmpty() || !java.util.Objects.equals(plan.paradas().get(plan.paradas().size()-1).ubicacionId(),destino.getId()))throw new IllegalArgumentException("Conserva la salida y regreso a planta y el destino final de la ruta predefinida; las paradas adicionales se insertan antes de ese destino.");
        var temporal=new ServicioTransporte();temporal.setRuta(solicitud.getRuta());
        ItinerarioViaje.prepararPlan(temporal,plan,id->ubicacionRepository.findById(id).orElseThrow(()->new IllegalArgumentException("La ubicación no existe.")),List.of());
        solicitud.setPlanViaje(plan);
        solicitud.setResumenRecorrido(temporal.getOrigenDescripcion()+" → "+temporal.getTramos().stream().map(t->t.getDestino()).collect(java.util.stream.Collectors.joining(" → ")));
        solicitud.setDistanciaEstimadaKm(temporal.getDistanciaEstimadaKm());solicitud.setDuracionEstimadaMinutos(temporal.getDuracionEstimadaMinutos());
    }
    @Transactional
    public SolicitudTransporteResponse completarRecorrido(Long id,com.palmas.transporte.modulos.operacion.api.RecorridoSolicitudRequest request) {
        var solicitud=buscarEntidadPorId(id);
        if(solicitud.getServicio()!=null || !java.util.Set.of(EstadoSolicitud.SOLICITADA,EstadoSolicitud.EN_REVISION,EstadoSolicitud.APROBADA).contains(solicitud.getEstado()))throw new IllegalArgumentException("No se puede cambiar el recorrido de una solicitud ya programada o cerrada.");
        if(solicitud.getPlanViaje()!=null && solicitud.getEstado()==EstadoSolicitud.APROBADA)throw new IllegalArgumentException("El recorrido ya está aprobado.");
        var ruta=rutaRepository.findById(request.rutaId()).orElseThrow(()->new IllegalArgumentException("La ruta no existe."));
        if(ruta.getEstado()!=com.palmas.transporte.modulos.recorridos.domain.EstadoRuta.ACTIVA)throw new IllegalArgumentException("La ruta está inactiva.");
        solicitud.setRuta(ruta);solicitud.setOrigen(ruta.getOrigen());solicitud.setDestino(ruta.getDestino());
        aplicarRecorrido(solicitud,request.planViaje());
        solicitud.setEstado(EstadoSolicitud.EN_REVISION);
        return convertirRespuesta(solicitudRepository.save(solicitud));
    }

    private void validarTransicion(
        EstadoSolicitud estadoActual,
        EstadoSolicitud nuevoEstado
    ) {
        boolean permitida = false;

        if (estadoActual == EstadoSolicitud.SOLICITADA) {
            permitida =
                nuevoEstado == EstadoSolicitud.EN_REVISION
                || nuevoEstado == EstadoSolicitud.CANCELADA;
        }

        if (estadoActual == EstadoSolicitud.EN_REVISION) {
            permitida =
                nuevoEstado == EstadoSolicitud.APROBADA
                || nuevoEstado == EstadoSolicitud.RECHAZADA
                || nuevoEstado == EstadoSolicitud.CANCELADA;
        }

        if (estadoActual == EstadoSolicitud.APROBADA) {
            permitida =
                nuevoEstado == EstadoSolicitud.CANCELADA;
        }

        if (!permitida) {
            throw new IllegalArgumentException(
                "No se puede cambiar la solicitud de "
                    + estadoActual
                    + " a "
                    + nuevoEstado
                    + "."
            );
        }
    }

    static void validarTipoCarga(com.palmas.transporte.modulos.operacion.domain.TipoSolicitudTransporte tipo, String producto) {
        String esperado = tipo == com.palmas.transporte.modulos.operacion.domain.TipoSolicitudTransporte.RECOJO_RFF ? "RFF"
            : tipo == com.palmas.transporte.modulos.operacion.domain.TipoSolicitudTransporte.RECOJO_FS ? "FS" : null;
        if (esperado != null && !esperado.equals(producto))
            throw new IllegalArgumentException("La carga debe corresponder al tipo de recojo seleccionado: " + esperado + ".");
    }

    private void validarDescripcionProducto(
        SolicitudTransporteRequest request
    ) {
        if (
            "RESIDUOS_VARIOS".equals(
                request.productoTransportado()
            )
            && normalizarOpcional(
                request.descripcionProducto()
            ) == null
        ) {
            throw new IllegalArgumentException(
                "Debes describir los residuos transportados."
            );
        }
    }

    private String construirObservaciones(
        SolicitudTransporte solicitud,
        String observacionesProgramacion
    ) {
        String observacionesSolicitud =
            normalizarOpcional(
                solicitud.getObservaciones()
            );

        String observacionesNuevas =
            normalizarOpcional(
                observacionesProgramacion
            );

        String referencia =
            "Solicitud: " + solicitud.getCodigo()
                + " | Origen solicitado: "
                + solicitud.getOrigen()
                + " | Destino solicitado: "
                + solicitud.getDestino();

        String resultado = referencia;

        if (observacionesSolicitud != null) {
            resultado += " | "
                + observacionesSolicitud;
        }

        if (observacionesNuevas != null) {
            resultado += " | "
                + observacionesNuevas;
        }

        /*
         * La columna observaciones de servicios admite
         * como máximo 500 caracteres.
         */
        if (resultado.length() > 500) {
            return resultado.substring(0, 500);
        }

        return resultado;
    }

    private SolicitudTransporte buscarEntidadPorId(
        Long id
    ) {
        return solicitudRepository.findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "No se encontró la solicitud "
                        + "de transporte."
                )
            );
    }

    private String generarCodigo() {
        String fecha = LocalDate.now().format(
            DateTimeFormatter.BASIC_ISO_DATE
        );

        String codigo;

        do {
            String aleatorio = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();

            codigo = "SOL-" + fecha + "-" + aleatorio;
        } while (
            solicitudRepository
                .existsByCodigoIgnoreCase(codigo)
        );

        return codigo;
    }

    private String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private SolicitudTransporteResponse
        convertirRespuesta(
            SolicitudTransporte solicitud
        ) {

        Usuario usuario =
            solicitud.getUsuarioSolicitante();

        Long servicioId =
            solicitud.getServicio() != null
                ? solicitud.getServicio().getId()
                : null;

        String servicioCodigo =
            solicitud.getServicio() != null
                ? solicitud.getServicio().getCodigo()
                : null;

        return new SolicitudTransporteResponse(
            solicitud.getId(),
            solicitud.getCodigo(),
            solicitud.getTipoSolicitud(),
            solicitud.getSolicitanteNombre(),
            solicitud.getAreaSolicitante(),
            solicitud.getTelefono(),
            solicitud.getOrigen(),
            solicitud.getDestino(),
            solicitud.getProductoTransportado(),
            solicitud.getDescripcionProducto(),
            solicitud.getCantidadEstimadaToneladas(),
            solicitud.getFechaRequerida(),
            solicitud.getPrioridad(),
            estadoVisible(solicitud),
            solicitud.getObservaciones(),
            solicitud.getMotivoRechazoCancelacion(),
            usuario.getId(),
            usuario.getCorreo(),
            servicioId,
            servicioCodigo,
            solicitud.getFechaRegistro(),
            solicitud.getFechaActualizacion(),
            solicitud.getRuta() == null ? null : solicitud.getRuta().getId(),
            solicitud.getPlanViaje(),solicitud.getResumenRecorrido(),solicitud.getDistanciaEstimadaKm(),solicitud.getDuracionEstimadaMinutos()
        );
    }

    static EstadoSolicitud estadoVisible(SolicitudTransporte solicitud) {
        if (solicitud.getServicio() == null) {
            return solicitud.getEstado();
        }
        return switch (solicitud.getServicio().getEstado()) {
            case PROGRAMADO -> EstadoSolicitud.PROGRAMADA;
            case EN_CURSO -> EstadoSolicitud.EN_CURSO;
            case COMPLETADO -> EstadoSolicitud.COMPLETADA;
            case CANCELADO -> EstadoSolicitud.CANCELADA;
            case INTERRUMPIDO -> EstadoSolicitud.INTERRUMPIDA;
        };
    }

private String generarCodigoServicio(
        String codigoSolicitud) {

    if (
        codigoSolicitud != null
        && codigoSolicitud.startsWith("SOL-")
    ) {
        return "SER-" + codigoSolicitud.substring(4);
    }

    throw new IllegalArgumentException(
        "La solicitud no tiene un código válido."
    );
}


}
