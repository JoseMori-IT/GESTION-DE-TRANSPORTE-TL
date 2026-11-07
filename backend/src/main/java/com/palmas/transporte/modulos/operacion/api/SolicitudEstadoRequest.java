package com.palmas.transporte.modulos.operacion.api;


import com.palmas.transporte.modulos.operacion.domain.EstadoSolicitud;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudEstadoRequest(

    @NotNull(message = "El nuevo estado es obligatorio")
    EstadoSolicitud estado,

    @Size(
        max = 500,
        message = "El motivo admite hasta 500 caracteres"
    )
    String motivo
) {
}