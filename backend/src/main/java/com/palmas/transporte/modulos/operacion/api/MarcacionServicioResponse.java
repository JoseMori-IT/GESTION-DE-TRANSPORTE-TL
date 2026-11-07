package com.palmas.transporte.modulos.operacion.api;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.palmas.transporte.modulos.operacion.domain.MarcacionServicio;
import com.palmas.transporte.modulos.operacion.domain.TipoMarcacionServicio;

public record MarcacionServicioResponse(
    Long id,
    String eventoUuid,
    Long servicioId,
    TipoMarcacionServicio tipo,
    BigDecimal latitud,
    BigDecimal longitud,
    BigDecimal precisionMetros,
    LocalDateTime fechaHoraDispositivo,
    LocalDateTime fechaHoraServidor,
    Boolean registradoOffline,
    String observacion,
    Integer tramoOrden,
    BigDecimal odometroKm,
    Boolean odometroNoDisponible,
    String motivoOdometro
) {
    public MarcacionServicioResponse(Long id,String eventoUuid,Long servicioId,TipoMarcacionServicio tipo,BigDecimal latitud,BigDecimal longitud,BigDecimal precisionMetros,LocalDateTime fechaHoraDispositivo,LocalDateTime fechaHoraServidor,Boolean registradoOffline,String observacion,Integer tramoOrden){this(id,eventoUuid,servicioId,tipo,latitud,longitud,precisionMetros,fechaHoraDispositivo,fechaHoraServidor,registradoOffline,observacion,tramoOrden,null,false,null);}
    public static MarcacionServicioResponse desde(MarcacionServicio entidad) {
        return new MarcacionServicioResponse(
            entidad.getId(),
            entidad.getEventoUuid(),
            entidad.getServicio().getId(),
            entidad.getTipo(),
            entidad.getLatitud(),
            entidad.getLongitud(),
            entidad.getPrecisionMetros(),
            entidad.getFechaHoraDispositivo(),
            entidad.getFechaHoraServidor(),
            entidad.getRegistradoOffline(),
            entidad.getObservacion(),
            null
        );
    }
}
