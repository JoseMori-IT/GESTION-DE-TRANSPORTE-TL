package com.palmas.transporte.modulos.mantenimiento.api;


import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RepararMantenimientoRequest(

    @NotBlank(message = "El trabajo realizado es obligatorio")
    @Size(
        max = 1500,
        message = "El trabajo realizado admite hasta 1500 caracteres"
    )
    String trabajoRealizado,

    @DecimalMin(
        value = "0.00",
        inclusive = true,
        message = "El costo no puede ser negativo"
    )
    @Digits(
        integer = 10,
        fraction = 2,
        message = "El costo admite hasta 10 enteros y 2 decimales"
    )
    BigDecimal costo
) {
}