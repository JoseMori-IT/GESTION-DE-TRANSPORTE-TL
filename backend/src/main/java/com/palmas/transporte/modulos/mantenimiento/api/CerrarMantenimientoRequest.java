package com.palmas.transporte.modulos.mantenimiento.api;


import jakarta.validation.constraints.Size;

public record CerrarMantenimientoRequest(

    @Size(
        max = 1000,
        message = "Las observaciones admiten hasta 1000 caracteres"
    )
    String observacionesCierre
) {
}