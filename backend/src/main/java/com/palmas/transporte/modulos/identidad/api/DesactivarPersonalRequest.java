package com.palmas.transporte.modulos.identidad.api;


import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DesactivarPersonalRequest(

    @NotNull(message = "La fecha de cese es obligatoria")
    LocalDate fechaCese,

    @NotBlank(message = "El motivo de desactivación es obligatorio")
    @Size(
        max = 500,
        message = "El motivo admite hasta 500 caracteres"
    )
    String motivo

) {
}