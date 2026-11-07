package com.palmas.transporte.modulos.recorridos.api;


import java.math.BigDecimal;

import com.palmas.transporte.modulos.recorridos.domain.EstadoRuta;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

public record RutaRequest(

    // Generado por el servidor. Se conserva el campo por compatibilidad.
    String codigo,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
    String nombre,

    @NotNull(message = "Selecciona la ubicación de origen")
    Long origenUbicacionId,

    @NotNull(message = "Selecciona la ubicación de destino")
    Long destinoUbicacionId,

    @NotNull(message = "La distancia es obligatoria")
    @DecimalMin(
        value = "0.01",
        message = "La distancia debe ser mayor que cero"
    )
    @Digits(
        integer = 8,
        fraction = 2,
        message = "La distancia admite hasta dos decimales"
    )
    BigDecimal distanciaKm,

    @NotNull(message = "La duración estimada es obligatoria")
    @Min(value = 1, message = "La duración mínima es un minuto")
    @Max(value = 10080, message = "La duración ingresada es demasiado extensa")
    Integer duracionEstimadaMinutos,

    @Size(
        max = 500,
        message = "Las observaciones admiten hasta 500 caracteres"
    )
    String observaciones,

    @NotNull(message = "El estado es obligatorio")
    EstadoRuta estado
) {
    @AssertTrue(message = "El origen y el destino deben ser diferentes")
    public boolean isRecorridoValido() {
        return origenUbicacionId == null
            || destinoUbicacionId == null
            || !origenUbicacionId.equals(destinoUbicacionId);
    }
}
