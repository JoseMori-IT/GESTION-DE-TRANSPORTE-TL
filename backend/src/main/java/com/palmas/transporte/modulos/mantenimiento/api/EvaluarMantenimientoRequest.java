package com.palmas.transporte.modulos.mantenimiento.api;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.palmas.transporte.modulos.mantenimiento.domain.PrioridadMantenimiento;

public record EvaluarMantenimientoRequest(

    @NotBlank(message = "El diagnóstico es obligatorio")
    @Size(
        max = 1500,
        message = "El diagnóstico admite hasta 1500 caracteres"
    )
    String diagnostico,

    @Size(
        max = 150,
        message = "El taller admite hasta 150 caracteres"
    )
    String tallerProveedor,

    PrioridadMantenimiento prioridad
) {
}
