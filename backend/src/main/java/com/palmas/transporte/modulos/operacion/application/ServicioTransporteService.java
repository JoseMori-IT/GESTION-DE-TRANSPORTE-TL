package com.palmas.transporte.modulos.operacion.application;
import com.palmas.transporte.modulos.recorridos.persistence.UbicacionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmas.transporte.modulos.operacion.api.ServicioRequest;
import com.palmas.transporte.modulos.identidad.domain.Conductor;
import com.palmas.transporte.modulos.identidad.domain.EstadoConductor;
import com.palmas.transporte.modulos.recorridos.domain.EstadoRuta;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.flota.domain.EstadoVehiculo;
import com.palmas.transporte.modulos.identidad.domain.RolTripulacion;
import com.palmas.transporte.modulos.recorridos.domain.Ruta;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.flota.domain.TipoVehiculo;
import com.palmas.transporte.modulos.flota.domain.Vehiculo;
import com.palmas.transporte.modulos.identidad.persistence.ConductorRepository;
import com.palmas.transporte.modulos.recorridos.persistence.RutaRepository;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;
import com.palmas.transporte.modulos.flota.persistence.VehiculoRepository;

@Service
public class ServicioTransporteService {
    private final com.palmas.transporte.modulos.configuracion.api.BaseOperativa baseOperativa;

    

    private static final Set<String> PRODUCTOS_PERMITIDOS = Set.of(
        "RFF",
        "FS",
        "ESCOBAJO",
        "FIBRA",
        "TORTA_CLARIFICACION",
        "LODO_BIOGAS",
        "CENIZA",
        "RESIDUOS_VARIOS"
    );


    private static final List<EstadoServicio>
    ESTADOS_QUE_RESERVAN_RECURSOS = List.of(
        EstadoServicio.PROGRAMADO,
        EstadoServicio.EN_CURSO
    );

    private final ServicioTransporteRepository servicioRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ConductorRepository conductorRepository;
    private final RutaRepository rutaRepository;
    private final EntityManager entityManager;
    @org.springframework.beans.factory.annotation.Autowired
    private com.palmas.transporte.modulos.recorridos.persistence.UbicacionRepository ubicacionRepository;

    public ServicioTransporteService(
        ServicioTransporteRepository servicioRepository,
        VehiculoRepository vehiculoRepository,
        ConductorRepository conductorRepository,
        RutaRepository rutaRepository,
        EntityManager entityManager,
        com.palmas.transporte.modulos.configuracion.api.BaseOperativa baseOperativa
    ) {
        this.servicioRepository = servicioRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.conductorRepository = conductorRepository;
        this.rutaRepository = rutaRepository;
        this.baseOperativa=baseOperativa;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<ServicioTransporte> listarTodos() {
        return servicioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ServicioTransporte buscarPorId(Long id) {
        return servicioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el servicio de transporte."
            ));
    }

    @Transactional
    public ServicioTransporte registrar(ServicioRequest request) {
        if (request.copilotoId() != null) throw new IllegalArgumentException("Los nuevos viajes se programan solo con chofer, sin copiloto.");
        if (request.fechaHoraSalida() == null) throw new IllegalArgumentException("Todo viaje requiere fecha y hora planificadas.");
        if (request.planViaje() == null) throw new IllegalArgumentException("Todo viaje requiere un recorrido planificado con retorno a planta.");
        baseOperativa.validarPlanta(request.planViaje().inicioUbicacionId(),request.planViaje().plantaRetornoId());
        if (request.rutaId() == null) throw new IllegalArgumentException("Debes seleccionar una ruta antes de programar el servicio.");
        // Bloquear antes de consultar reservas: vehiculo primero,
        // despues personal por ID ascendente, siempre en la misma transaccion.
        vehiculoRepository.bloquearPorId(request.vehiculoId())
            .orElseThrow(() -> new IllegalArgumentException("No se encontro el vehiculo."));
        java.util.stream.Stream.of(request.conductorId(), request.copilotoId())
            .filter(java.util.Objects::nonNull)
            .distinct()
            .sorted()
            .forEach(id -> conductorRepository.bloquearPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la persona.")));
        String codigo = request.codigo().trim().toUpperCase();

        if (servicioRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new IllegalArgumentException(
                "Ya existe un servicio con el código indicado."
            );
        }

        if (request.estado() != EstadoServicio.PROGRAMADO) {
            throw new IllegalArgumentException(
                "Un servicio nuevo debe iniciar como PROGRAMADO."
            );
        }

        String producto = request.productoTransportado();

        if (producto == null
            || !PRODUCTOS_PERMITIDOS.contains(producto)) {
            throw new IllegalArgumentException(
                "Selecciona un producto transportado válido."
            );
        }

        String descripcionResiduos = normalizarTexto(
            request.descripcionResiduos()
        );

        if ("RESIDUOS_VARIOS".equals(producto)
            && descripcionResiduos == null) {
            throw new IllegalArgumentException(
                "Describe los residuos transportados."
            );
        }

        Vehiculo vehiculo = obtenerVehiculoDisponible(
            request.vehiculoId()
        );

        Conductor chofer = obtenerPersonaDisponible(
            request.conductorId()
        );
        validarChofer(chofer);

        Conductor copiloto = null;

        if (request.copilotoId() != null) {
    if (request.copilotoId().equals(chofer.getId())) {
        throw new IllegalArgumentException(
            "El chofer y el copiloto deben ser personas distintas."
        );
    }

    copiloto = obtenerPersonaDisponible(
        request.copilotoId()
    );
    validarCopiloto(copiloto);
}

validarRecursosSinServicioActivo(
    vehiculo,
    chofer,
    copiloto
);

Ruta ruta = request.rutaId() != null
    ? obtenerRutaActiva(request.rutaId())
    : null;

        ServicioTransporte servicio = new ServicioTransporte();
        servicio.setCodigo(codigo);
        servicio.setVehiculo(vehiculo);
        servicio.setConductor(chofer);
        servicio.setCopiloto(copiloto);
        servicio.setRuta(ruta);
        if (ruta != null) {
            servicio.setOrigenDescripcion(ruta.getOrigen());
            servicio.setDestinoDescripcion(ruta.getDestino());
            servicio.setDistanciaEstimadaKm(ruta.getDistanciaKm());
            servicio.setDuracionEstimadaMinutos(
                ruta.getDuracionEstimadaMinutos()
            );

            if (ruta.getOrigenUbicacion() != null) {
                servicio.setOrigenLatitud(
                    ruta.getOrigenUbicacion().getLatitud()
                );
                servicio.setOrigenLongitud(
                    ruta.getOrigenUbicacion().getLongitud()
                );
            }

            if (ruta.getDestinoUbicacion() != null) {
                servicio.setDestinoLatitud(
                    ruta.getDestinoUbicacion().getLatitud()
                );
                servicio.setDestinoLongitud(
                    ruta.getDestinoUbicacion().getLongitud()
                );
            }
        } else {
            servicio.setOrigenDescripcion(
                request.origenDescripcion().trim()
            );
            servicio.setDestinoDescripcion(
                request.destinoDescripcion().trim()
            );
        }
        servicio.setRequiereRetorno(
            Boolean.TRUE.equals(request.requiereRetorno())
        );
        servicio.setFechaHoraSalida(request.fechaHoraSalida());
        servicio.setProductoTransportado(producto);
        servicio.setDescripcionResiduos(
            "RESIDUOS_VARIOS".equals(producto)
                ? descripcionResiduos
                : null
        );
        servicio.setEstado(EstadoServicio.PROGRAMADO);
        servicio.setObservaciones(request.observaciones());

        if (request.planViaje() != null) ItinerarioViaje.prepararPlan(servicio, request.planViaje(), id -> ubicacionRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No existe el punto seleccionado.")), rutaRepository.findAll());
        else ItinerarioViaje.preparar(servicio, request.tramos(), this::obtenerRutaActiva);
        return servicioRepository.save(servicio);
    }

    @Transactional
public ServicioTransporte cambiarEstado(
    Long id,
    EstadoServicio nuevoEstado
) {
    return cambiarEstadoInterno(id,nuevoEstado,false);
}

    @Transactional
    public ServicioTransporte cambiarEstadoDesdeMarcacion(Long id,EstadoServicio nuevoEstado){return cambiarEstadoInterno(id,nuevoEstado,true);}

    private ServicioTransporte cambiarEstadoInterno(Long id,EstadoServicio nuevoEstado,boolean desdeMarcacion){
    ServicioTransporte servicio = buscarPorId(id);
    // Recargar bajo bloqueo incluso si ChoferService ya cargo esta entidad.
    entityManager.refresh(servicio, LockModeType.PESSIMISTIC_WRITE);
    EstadoServicio estadoActual = servicio.getEstado();

    if (estadoActual == nuevoEstado) {
        return servicio;
    }

    if(!desdeMarcacion && nuevoEstado==EstadoServicio.EN_CURSO && !servicio.getTramos().isEmpty())throw new IllegalArgumentException("Inicia el viaje desde su marcación, registrando el odómetro o su excepción.");
    if (nuevoEstado == EstadoServicio.COMPLETADO && !servicio.getTramos().isEmpty() && servicio.getTramos().stream().anyMatch(t -> t.getLlegadaReal() == null)) throw new IllegalArgumentException("Registra todas las paradas del itinerario antes de finalizar el viaje.");
    if(servicio.isDetenidoPorAveria())throw new IllegalArgumentException("El viaje tiene una avería pendiente; gestiona la atención o retorno desde la incidencia.");
    if(estadoActual==EstadoServicio.EN_CURSO && nuevoEstado==EstadoServicio.CANCELADO)throw new IllegalArgumentException("Un viaje iniciado no se cancela. Registra la incidencia y su atención o retorno.");
    validarTransicion(estadoActual, nuevoEstado);

    Vehiculo vehiculo = servicio.getVehiculo();
    Conductor chofer = servicio.getConductor();
    Conductor copiloto = servicio.getCopiloto();

    entityManager.refresh(vehiculo, LockModeType.PESSIMISTIC_WRITE);
    java.util.stream.Stream.of(chofer, copiloto)
        .filter(java.util.Objects::nonNull)
        .sorted(java.util.Comparator.comparing(Conductor::getId))
        .forEach(persona -> entityManager.refresh(persona, LockModeType.PESSIMISTIC_WRITE));

    if (nuevoEstado == EstadoServicio.EN_CURSO) {
        obtenerVehiculoDisponible(vehiculo.getId());
        obtenerPersonaDisponible(chofer.getId());
        validarChofer(chofer);

        if (copiloto != null) {
            obtenerPersonaDisponible(copiloto.getId());
            validarCopiloto(copiloto);
        }

        servicio.setFechaHoraSalidaReal(
            LocalDateTime.now()
        );

        servicio.setFechaHoraLlegadaReal(null);

        vehiculo.setEstado(
            EstadoVehiculo.EN_SERVICIO
        );

        chofer.setEstado(
            EstadoConductor.EN_SERVICIO
        );

        if (copiloto != null) {
            copiloto.setEstado(
                EstadoConductor.EN_SERVICIO
            );
        }
    }

    if (
        estadoActual == EstadoServicio.EN_CURSO
        && nuevoEstado == EstadoServicio.COMPLETADO
    ) {
        servicio.setFechaHoraLlegadaReal(
            LocalDateTime.now()
        );

        liberarVehiculoYPersonal(
            vehiculo,
            chofer,
            copiloto
        );
    }

    if (
        estadoActual == EstadoServicio.EN_CURSO
        && nuevoEstado == EstadoServicio.CANCELADO
    ) {
        servicio.setFechaHoraLlegadaReal(null);

        liberarVehiculoYPersonal(
            vehiculo,
            chofer,
            copiloto
        );
    }

    /*
     * Si se anula un servicio PROGRAMADO, no existen
     * recursos ocupados ni horas reales que registrar.
     */
    if (
        estadoActual == EstadoServicio.PROGRAMADO
        && nuevoEstado == EstadoServicio.CANCELADO
    ) {
        servicio.setFechaHoraSalidaReal(null);
        servicio.setFechaHoraLlegadaReal(null);
    }

    servicio.setEstado(nuevoEstado);

    vehiculoRepository.save(vehiculo);
    conductorRepository.save(chofer);

    if (copiloto != null) {
        conductorRepository.save(copiloto);
    }

    return servicioRepository.save(servicio);
}
    
    private void validarTransicion(
        EstadoServicio actual,
        EstadoServicio nuevo
    ) {
        boolean permitida =
            actual == EstadoServicio.PROGRAMADO
                && (nuevo == EstadoServicio.EN_CURSO
                    || nuevo == EstadoServicio.CANCELADO);

        permitida = permitida
            || (actual == EstadoServicio.EN_CURSO
                && (nuevo == EstadoServicio.COMPLETADO
                    || nuevo == EstadoServicio.CANCELADO));

        if (!permitida) {
            throw new IllegalArgumentException(
                "No se puede cambiar el servicio de "
                    + actual + " a " + nuevo + "."
            );
        }
    }

    private Vehiculo obtenerVehiculoDisponible(Long id) {
        Vehiculo vehiculo = vehiculoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró el vehículo seleccionado."
            ));

        if (vehiculo.getTipo() != TipoVehiculo.CAMION) {
            throw new IllegalArgumentException(
                "Solo se pueden programar camiones."
            );
        }

        if (vehiculo.getEstado() != EstadoVehiculo.DISPONIBLE) {
            throw new IllegalArgumentException(
                "El camión seleccionado no está disponible."
            );
        }

        return vehiculo;
    }

    private Conductor obtenerPersonaDisponible(Long id) {
        Conductor persona = conductorRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró la persona seleccionada."
            ));

        if (persona.getEstado() != EstadoConductor.DISPONIBLE) {
            throw new IllegalArgumentException(
                "La persona seleccionada no está disponible."
            );
        }

        return persona;
    }

    private void validarChofer(Conductor persona) {
        RolTripulacion rol = persona.getRolTripulacion();

        if (rol != RolTripulacion.CHOFER
            && rol != RolTripulacion.AMBOS) {
            throw new IllegalArgumentException(
                "La persona seleccionada no está habilitada como chofer."
            );
        }

        if (persona.getNumeroLicencia() == null
            || persona.getNumeroLicencia().isBlank()
            || persona.getCategoriaLicencia() == null
            || persona.getVencimientoLicencia() == null
            || persona.getVencimientoLicencia()
                .isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                "El chofer debe tener una licencia vigente."
            );
        }
    }

    private void validarCopiloto(Conductor persona) {
        RolTripulacion rol = persona.getRolTripulacion();

        if (rol != RolTripulacion.COPILOTO
            && rol != RolTripulacion.AMBOS) {
            throw new IllegalArgumentException(
                "La persona seleccionada no está registrada como copiloto."
            );
        }
    }

private void validarRecursosSinServicioActivo(
    Vehiculo vehiculo,
    Conductor chofer,
    Conductor copiloto
) {
    boolean vehiculoReservado =
        servicioRepository
            .existsByVehiculoIdAndEstadoIn(
                vehiculo.getId(),
                ESTADOS_QUE_RESERVAN_RECURSOS
            );

    if (vehiculoReservado) {
        throw new IllegalArgumentException(
            "El camión seleccionado ya tiene un servicio "
                + "programado o en curso."
        );
    }

    validarPersonaSinServicioActivo(
        chofer.getId(),
        "chofer"
    );

    if (copiloto != null) {
        validarPersonaSinServicioActivo(
            copiloto.getId(),
            "copiloto"
        );
    }
}

private void validarPersonaSinServicioActivo(
    Long personaId,
    String funcion
) {
    boolean asignadoComoChofer =
        servicioRepository
            .existsByConductorIdAndEstadoIn(
                personaId,
                ESTADOS_QUE_RESERVAN_RECURSOS
            );

    boolean asignadoComoCopiloto =
        servicioRepository
            .existsByCopilotoIdAndEstadoIn(
                personaId,
                ESTADOS_QUE_RESERVAN_RECURSOS
            );

    if (
        asignadoComoChofer
        || asignadoComoCopiloto
    ) {
        throw new IllegalArgumentException(
            "La persona seleccionada como "
                + funcion
                + " ya tiene un servicio programado "
                + "o en curso."
        );
    }
}



    private Ruta obtenerRutaActiva(Long id) {
        Ruta ruta = rutaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException(
                "No se encontró la ruta seleccionada."
            ));

        if (ruta.getEstado() != EstadoRuta.ACTIVA) {
            throw new IllegalArgumentException(
                "La ruta seleccionada está inactiva."
            );
        }

        return ruta;
    }

    private void liberarVehiculoYPersonal(
        Vehiculo vehiculo,
        Conductor chofer,
        Conductor copiloto
    ) {
        if (vehiculo.getEstado() == EstadoVehiculo.EN_SERVICIO) {
            vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        }

        if (chofer.getEstado() == EstadoConductor.EN_SERVICIO) {
            chofer.setEstado(EstadoConductor.DISPONIBLE);
        }

        if (copiloto != null
            && copiloto.getEstado() == EstadoConductor.EN_SERVICIO) {
            copiloto.setEstado(EstadoConductor.DISPONIBLE);
        }
    }

    private String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }
}
