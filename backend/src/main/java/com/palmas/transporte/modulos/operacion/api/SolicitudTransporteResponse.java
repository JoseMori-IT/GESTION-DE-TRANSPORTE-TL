package com.palmas.transporte.modulos.operacion.api;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.palmas.transporte.modulos.operacion.domain.EstadoSolicitud;
import com.palmas.transporte.modulos.operacion.domain.PrioridadSolicitud;
import com.palmas.transporte.modulos.operacion.domain.TipoSolicitudTransporte;

public record SolicitudTransporteResponse(

    Long id,

    String codigo,

    TipoSolicitudTransporte tipoSolicitud,

    String solicitanteNombre,

    String areaSolicitante,

    String telefono,

    String origen,

    String destino,

    String productoTransportado,

    String descripcionProducto,

    BigDecimal cantidadEstimadaToneladas,

    LocalDateTime fechaRequerida,

    PrioridadSolicitud prioridad,

    EstadoSolicitud estado,

    String observaciones,

    String motivoRechazoCancelacion,

    Long usuarioSolicitanteId,

    String usuarioSolicitanteCorreo,

    Long servicioId,

    String servicioCodigo,

    LocalDateTime fechaRegistro,

    LocalDateTime fechaActualizacion,

    Long rutaId,
    PlanViajeRequest planViaje,
    String resumenRecorrido,
    BigDecimal distanciaEstimadaKm,
    Integer duracionEstimadaMinutos
) {
}