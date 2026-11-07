package com.palmas.transporte.modulos.operacion.application;


import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.palmas.transporte.modulos.operacion.api.MarcacionServicioRequest;
import com.palmas.transporte.modulos.operacion.api.MarcacionServicioResponse;
import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import com.palmas.transporte.modulos.operacion.domain.MarcacionServicio;
import com.palmas.transporte.modulos.operacion.domain.ServicioTransporte;
import com.palmas.transporte.modulos.operacion.domain.TipoMarcacionServicio;
import com.palmas.transporte.modulos.operacion.persistence.MarcacionServicioRepository;
import com.palmas.transporte.modulos.operacion.persistence.ServicioTransporteRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
public class MarcacionServicioService {

    private static final BigDecimal PRECISION_MAXIMA = new BigDecimal("100.00");

    private final MarcacionServicioRepository marcacionRepository;
    private final ServicioTransporteRepository servicioRepository;
    private final ServicioTransporteService servicioService;
    private final EntityManager entityManager;
    private final MarcacionTramoService tramos;

    public MarcacionServicioService(
        MarcacionServicioRepository marcacionRepository,
        ServicioTransporteRepository servicioRepository,
        ServicioTransporteService servicioService,
        EntityManager entityManager,
        MarcacionTramoService tramos
    ) {
        this.marcacionRepository = marcacionRepository;
        this.servicioRepository = servicioRepository;
        this.servicioService = servicioService;
        this.entityManager = entityManager;
        this.tramos = tramos;
    }

    @Transactional(readOnly = true)
    public List<MarcacionServicioResponse> listar(
        Long servicioId,
        Long conductorId
    ) {
        var servicio = verificarServicioPropio(servicioId, conductorId);
        if (!servicio.getTramos().isEmpty()) return tramos.listar(servicioId);
        return marcacionRepository
            .findByServicioIdOrderByFechaHoraDispositivoAsc(servicioId)
            .stream()
            .map(MarcacionServicioResponse::desde)
            .toList();
    }

    @Transactional
    public MarcacionServicioResponse registrar(
        Long servicioId,
        Long conductorId,
        MarcacionServicioRequest request
    ) {
        if (request.tramoOrden() != null) { validarUbicacionYFecha(request); return tramos.registrar(servicioId, conductorId, request); }
        MarcacionServicio repetida = marcacionRepository
            .findByEventoUuid(request.eventoUuid())
            .orElse(null);

        if (repetida != null) {
            if (!repetida.getServicio().getId().equals(servicioId)
                || !repetida.getConductor().getId().equals(conductorId)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El identificador de la marcación ya fue utilizado."
                );
            }
            return MarcacionServicioResponse.desde(repetida);
        }

        ServicioTransporte servicio = verificarServicioPropio(
            servicioId,
            conductorId
        );
        entityManager.refresh(servicio, LockModeType.PESSIMISTIC_WRITE);
        if (!servicio.getTramos().isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Indica el tramo de la marcación. Actualiza el viaje.");

        if(servicio.isDetenidoPorAveria() || servicio.getEstado()==EstadoServicio.INTERRUMPIDO) throw new ResponseStatusException(HttpStatus.CONFLICT,"Atiende la avería antes de continuar el viaje.");
        validarUbicacionYFecha(request);
        validarSecuencia(servicio, request.tipo());

        MarcacionServicio marcacion = new MarcacionServicio();
        marcacion.setEventoUuid(request.eventoUuid().trim());
        marcacion.setServicio(servicio);
        marcacion.setConductor(servicio.getConductor());
        marcacion.setTipo(request.tipo());
        marcacion.setLatitud(request.latitud());
        marcacion.setLongitud(request.longitud());
        marcacion.setPrecisionMetros(request.precisionMetros());
        marcacion.setFechaHoraDispositivo(request.fechaHoraDispositivo());
        marcacion.setRegistradoOffline(request.registradoOffline());
        marcacion.setObservacion(request.observacion());

        aplicarEstado(servicio, request);

        return MarcacionServicioResponse.desde(
            marcacionRepository.save(marcacion)
        );
    }

    private ServicioTransporte verificarServicioPropio(
        Long servicioId,
        Long conductorId
    ) {
        return servicioRepository
            .findByIdAndConductorId(servicioId, conductorId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "El servicio no existe o no está asignado al chofer autenticado"
            ));
    }

    private void validarUbicacionYFecha(MarcacionServicioRequest request) {
        if (request.precisionMetros().compareTo(PRECISION_MAXIMA) > 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La señal GPS no tiene precisión suficiente. Intenta desde un lugar despejado."
            );
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (request.fechaHoraDispositivo().isAfter(ahora.plusMinutes(15))) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La hora del dispositivo está adelantada. Corrige la fecha y hora del celular."
            );
        }
        if (Duration.between(request.fechaHoraDispositivo(), ahora).toDays() > 7) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La marcación pendiente tiene más de siete días y requiere revisión administrativa."
            );
        }
    }

    private void validarSecuencia(
        ServicioTransporte servicio,
        TipoMarcacionServicio tipo
    ) {
        Long id = servicio.getId();
        boolean salida = existe(id, TipoMarcacionServicio.SALIDA_IDA);
        boolean llegada = existe(id, TipoMarcacionServicio.LLEGADA_DESTINO);
        boolean retorno = existe(id, TipoMarcacionServicio.SALIDA_RETORNO);

        switch (tipo) {
            case SALIDA_IDA -> {
                if (salida || servicio.getEstado() != EstadoServicio.PROGRAMADO) {
                    secuenciaInvalida();
                }
            }
            case LLEGADA_DESTINO -> {
                if (!salida || llegada || servicio.getEstado() != EstadoServicio.EN_CURSO) {
                    secuenciaInvalida();
                }
            }
            case SALIDA_RETORNO -> {
                if (!Boolean.TRUE.equals(servicio.getRequiereRetorno())
                    || !llegada || retorno
                    || servicio.getEstado() != EstadoServicio.EN_CURSO) {
                    secuenciaInvalida();
                }
            }
            case LLEGADA_BASE -> {
                if (!Boolean.TRUE.equals(servicio.getRequiereRetorno())
                    || !retorno
                    || existe(id, TipoMarcacionServicio.LLEGADA_BASE)
                    || servicio.getEstado() != EstadoServicio.EN_CURSO) {
                    secuenciaInvalida();
                }
            }
        }
    }

    private void aplicarEstado(
        ServicioTransporte servicio,
        MarcacionServicioRequest request
    ) {
        if (request.tipo() == TipoMarcacionServicio.SALIDA_IDA) {
            ServicioTransporte actualizado = servicioService.cambiarEstado(
                servicio.getId(),
                EstadoServicio.EN_CURSO
            );
            actualizado.setFechaHoraSalidaReal(request.fechaHoraDispositivo());
            servicioRepository.save(actualizado);
        }

        boolean finalizaEnDestino =
            request.tipo() == TipoMarcacionServicio.LLEGADA_DESTINO
            && !Boolean.TRUE.equals(servicio.getRequiereRetorno());

        if (finalizaEnDestino
            || request.tipo() == TipoMarcacionServicio.LLEGADA_BASE) {
            ServicioTransporte actualizado = servicioService.cambiarEstado(
                servicio.getId(),
                EstadoServicio.COMPLETADO
            );
            actualizado.setFechaHoraLlegadaReal(request.fechaHoraDispositivo());
            servicioRepository.save(actualizado);
        }
    }

    private boolean existe(Long servicioId, TipoMarcacionServicio tipo) {
        return marcacionRepository.existsByServicioIdAndTipo(servicioId, tipo);
    }

    private void secuenciaInvalida() {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "La marcación no corresponde con la etapa actual del servicio. Actualiza la pantalla."
        );
    }
}
