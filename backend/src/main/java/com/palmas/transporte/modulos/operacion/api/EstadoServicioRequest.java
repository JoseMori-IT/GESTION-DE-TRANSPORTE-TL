package com.palmas.transporte.modulos.operacion.api;


import com.palmas.transporte.modulos.operacion.domain.EstadoServicio;
import jakarta.validation.constraints.NotNull;

public record EstadoServicioRequest(

    @NotNull(message = "El estado del servicio es obligatorio")
    EstadoServicio estado

) {
}