package com.palmas.transporte.modulos.evaluacion.api;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EncuestaServicioRequest(

    @NotNull(message = "La puntualidad es obligatoria")
    @Min(value = 1, message = "La puntualidad mínima es 1")
    @Max(value = 5, message = "La puntualidad máxima es 5")
    Integer puntualidad,

    @NotNull(message = "El cumplimiento es obligatorio")
    @Min(value = 1, message = "El cumplimiento mínimo es 1")
    @Max(value = 5, message = "El cumplimiento máximo es 5")
    Integer cumplimiento,

    @NotNull(message = "La atención es obligatoria")
    @Min(value = 1, message = "La atención mínima es 1")
    @Max(value = 5, message = "La atención máxima es 5")
    Integer atencion,

    @NotNull(message = "La satisfacción general es obligatoria")
    @Min(value = 1, message = "La satisfacción mínima es 1")
    @Max(value = 5, message = "La satisfacción máxima es 5")
    Integer satisfaccionGeneral,

    @Size(
        max = 1000,
        message = "Los comentarios no pueden superar 1000 caracteres"
    )
    String comentarios
) {
}