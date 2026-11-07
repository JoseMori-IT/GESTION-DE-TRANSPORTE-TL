package com.palmas.transporte.modulos.operacion.api;


import java.time.LocalDateTime;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProgramarSolicitudRequest(

    @NotBlank(message = "El código del servicio es obligatorio")
    @Pattern(
        regexp = "^[A-Za-z0-9-]{3,30}$",
        message = "El código debe contener entre 3 y 30 caracteres"
    )
    String codigoServicio,

    @NotNull(message = "El vehículo es obligatorio")
    Long vehiculoId,

    @NotNull(message = "El chofer es obligatorio")
    Long conductorId,

    Long copilotoId,

    @NotNull(message = "Selecciona una ruta estandarizada")
    Long rutaId,

    Boolean requiereRetorno,

    @NotNull(message = "Todo viaje debe tener fecha y hora planificadas")
    @FutureOrPresent(
        message = "La salida no puede programarse en el pasado"
    )
    LocalDateTime fechaHoraSalida,

    @Size(
        max = 500,
        message = "Las observaciones admiten hasta 500 caracteres"
    )
    String observaciones,

    @jakarta.validation.Valid @Size(max = 30) java.util.List<TramoRequest> tramos,
    @jakarta.validation.Valid PlanViajeRequest planViaje
 ) {
    public ProgramarSolicitudRequest(String codigoServicio, Long vehiculoId, Long conductorId, Long copilotoId, Long rutaId, Boolean requiereRetorno, LocalDateTime fechaHoraSalida, String observaciones, java.util.List<TramoRequest> tramos) {
        this(codigoServicio,vehiculoId,conductorId,copilotoId,rutaId,requiereRetorno,fechaHoraSalida,observaciones,tramos,null);
    }
}
