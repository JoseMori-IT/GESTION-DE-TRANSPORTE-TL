package com.palmas.transporte.modulos.recorridos.api;


import java.math.BigDecimal;

import com.palmas.transporte.modulos.recorridos.domain.EstadoRuta;
import com.palmas.transporte.modulos.recorridos.domain.TipoUbicacion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UbicacionRequest(
    @NotBlank(message = "El nombre de la ubicación es obligatorio")
    @Size(max = 150)
    String nombre,

    @NotNull(message = "El tipo de ubicación es obligatorio")
    TipoUbicacion tipo,

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud no es válida")
    @DecimalMax(value = "90.0", message = "La latitud no es válida")
    @Digits(integer = 3, fraction = 7)
    BigDecimal latitud,

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud no es válida")
    @DecimalMax(value = "180.0", message = "La longitud no es válida")
    @Digits(integer = 3, fraction = 7)
    BigDecimal longitud,

    @Size(max = 300, message = "La referencia admite hasta 300 caracteres")
    String referencia,

    @NotNull(message = "El estado es obligatorio")
    EstadoRuta estado
) {
}
