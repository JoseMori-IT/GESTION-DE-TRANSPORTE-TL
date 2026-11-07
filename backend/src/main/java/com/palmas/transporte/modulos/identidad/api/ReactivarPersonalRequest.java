package com.palmas.transporte.modulos.identidad.api;


import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record ReactivarPersonalRequest(

    @NotNull(message = "La fecha de reingreso es obligatoria")
    LocalDate fechaReingreso

) {
}