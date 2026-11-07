package com.palmas.transporte.modulos.operacion.api;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.palmas.transporte.modulos.operacion.domain.TipoMarcacionServicio;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MarcacionServicioRequest(
    @NotBlank
    @Pattern(
        regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
        message = "El identificador de la marcación no es válido"
    )
    String eventoUuid,
    @NotNull TipoMarcacionServicio tipo,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitud,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitud,
    @NotNull @DecimalMin("0.0") @DecimalMax("1000.0") BigDecimal precisionMetros,
    @NotNull LocalDateTime fechaHoraDispositivo,
    @NotNull Boolean registradoOffline,
    @Size(max = 300) String observacion,
    @jakarta.validation.constraints.Min(1) Integer tramoOrden,
    @DecimalMin("0.0") @DecimalMax("99999999.99") @jakarta.validation.constraints.Digits(integer=8,fraction=2) BigDecimal odometroKm,
    Boolean odometroNoDisponible,
    @Size(max=300) String motivoOdometro
) {
    public MarcacionServicioRequest(String eventoUuid, TipoMarcacionServicio tipo, BigDecimal latitud, BigDecimal longitud, BigDecimal precisionMetros, LocalDateTime fechaHoraDispositivo, Boolean registradoOffline, String observacion, Integer tramoOrden, BigDecimal odometroKm) {
        this(eventoUuid,tipo,latitud,longitud,precisionMetros,fechaHoraDispositivo,registradoOffline,observacion,tramoOrden,odometroKm,false,null);
    }
    public MarcacionServicioRequest(String eventoUuid, TipoMarcacionServicio tipo, BigDecimal latitud, BigDecimal longitud, BigDecimal precisionMetros, LocalDateTime fechaHoraDispositivo, Boolean registradoOffline, String observacion, Integer tramoOrden) {
        this(eventoUuid,tipo,latitud,longitud,precisionMetros,fechaHoraDispositivo,registradoOffline,observacion,tramoOrden,null);
    }
}
